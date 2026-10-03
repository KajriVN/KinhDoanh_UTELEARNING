package com.utelearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Chatbot tư vấn khóa học. Có ANTHROPIC_API_KEY thì hỏi Claude (kèm danh sách khóa PUBLISHED trong system prompt);
// chưa có key hoặc gọi lỗi thì tự gợi ý theo từ khóa để widget vẫn dùng được.
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_HISTORY = 6;
    private static final int MAX_COURSES_IN_PROMPT = 60;
    // chống lạm dụng (endpoint công khai và mỗi lần gọi tốn tiền): tối đa 20 tin / 10 phút / IP
    private static final int RATE_LIMIT = 20;
    private static final long RATE_WINDOW_MS = 10 * 60 * 1000L;

    public record ChatTurn(String role, String content) {
    }

    public record ChatReply(String reply, String mode) {
    }

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.api-url}")
    private String apiUrl;

    public AiChatService(CourseRepository courseRepository, ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.objectMapper = objectMapper;
    }

    public ChatReply chat(String message, List<ChatTurn> history, String clientIp) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("Vui lòng nhập câu hỏi");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException("Câu hỏi tối đa " + MAX_MESSAGE_LENGTH + " ký tự");
        }
        checkRateLimit(clientIp);

        List<Course> courses = courseRepository
                .findTop60ByStatusOrderByCreatedAtDesc(CourseStatus.PUBLISHED);

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return new ChatReply(askClaude(message.trim(), history, courses), "ai");
            } catch (Exception e) {
                // lỗi mạng / hết hạn mức / key sai -> vẫn trả lời được bằng gợi ý theo từ khóa
                log.warn("Gọi AI thất bại, chuyển sang gợi ý theo từ khóa: {}", e.getMessage());
            }
        }
        return new ChatReply(suggestByKeywords(message, courses), "fallback");
    }

    // ================== CLAUDE ==================

    private String askClaude(String message, List<ChatTurn> history, List<Course> courses) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 600);
        body.put("system", buildSystemPrompt(courses));

        ArrayNode messages = body.putArray("messages");
        // chỉ giữ vài lượt gần nhất; Claude yêu cầu bắt đầu bằng lượt "user" và xen kẽ user/assistant
        List<ChatTurn> recent = history == null ? List.of()
                : history.subList(Math.max(0, history.size() - MAX_HISTORY), history.size());
        String expected = "user";
        for (ChatTurn turn : recent) {
            if (turn == null || turn.content() == null || turn.content().isBlank()
                    || !turn.role().equals(expected) || turn.content().length() > 2000) {
                continue;
            }
            messages.addObject().put("role", turn.role()).put("content", turn.content());
            expected = expected.equals("user") ? "assistant" : "user";
        }
        if (!expected.equals("user")) {
            // lượt cuối của lịch sử là "user" -> bỏ để câu hỏi mới không bị 2 lượt user liền nhau
            messages.remove(messages.size() - 1);
        }
        messages.addObject().put("role", "user").put("content", message);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(25))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonNode content = objectMapper.readTree(response.body()).path("content");
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("Phản hồi rỗng");
        }
        return text.toString().trim();
    }

    private String buildSystemPrompt(List<Course> courses) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý tư vấn khóa học của UTELearn, nền tảng bán khóa học trực tuyến. ")
                .append("Hãy trả lời bằng tiếng Việt, thân thiện, ngắn gọn (tối đa khoảng 120 từ). ")
                .append("Chỉ gợi ý các khóa có trong danh sách bên dưới, tuyệt đối không bịa khóa học, giá hay nội dung. ")
                .append("Khi nhắc tới một khóa, viết dưới dạng liên kết markdown [Tên khóa](/courses/ID). ")
                .append("Nếu không có khóa phù hợp, hãy nói thật và gợi ý người dùng xem trang /courses. ")
                .append("Không trả lời các câu hỏi không liên quan đến học tập hoặc khóa học trên UTELearn.\n\n")
                .append("DANH SÁCH KHÓA HỌC HIỆN CÓ (id | tên | danh mục | trình độ | giá | mô tả):\n");
        int n = 0;
        for (Course c : courses) {
            if (n++ >= MAX_COURSES_IN_PROMPT) {
                break;
            }
            String desc = c.getShortDescription() == null ? "" : c.getShortDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 200) + "...";
            }
            sb.append(c.getId()).append(" | ").append(c.getTitle()).append(" | ")
                    .append(c.getCategory() != null ? c.getCategory().getName() : "-").append(" | ")
                    .append(c.getLevel().getLabel()).append(" | ")
                    .append(c.isFree() ? "Miễn phí" : c.getPrice().toPlainString() + " đ").append(" | ")
                    .append(desc).append('\n');
        }
        return sb.toString();
    }

    // ================== GỢI Ý THEO TỪ KHÓA (không cần API key) ==================

    String suggestByKeywords(String message, List<Course> courses) {
        if (courses.isEmpty()) {
            return "Hiện UTELearn chưa có khóa học nào được xuất bản. Bạn quay lại sau nhé!";
        }
        String query = normalize(message);
        boolean wantsFree = query.contains("mien phi") || query.contains("free");

        Set<String> keywords = new HashSet<>();
        for (String word : query.split("[^a-z0-9+#.]+")) {
            // bỏ từ quá ngắn và các từ hỏi thông dụng
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }

        List<Course> ranked = new ArrayList<>(courses);
        List<int[]> scores = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Course c = ranked.get(i);
            String haystack = normalize(c.getTitle() + " " + (c.getShortDescription() == null ? "" : c.getShortDescription())
                    + " " + (c.getCategory() != null ? c.getCategory().getName() : ""));
            int score = 0;
            for (String k : keywords) {
                if (haystack.contains(k)) {
                    score += normalize(c.getTitle()).contains(k) ? 3 : 1;
                }
            }
            if (wantsFree && c.isFree()) {
                score += 2;
            }
            scores.add(new int[]{i, score});
        }
        scores.sort(Comparator.<int[]>comparingInt(s -> s[1]).reversed().thenComparingInt(s -> s[0]));

        boolean matched = !scores.isEmpty() && scores.get(0)[1] > 0;
        StringBuilder sb = new StringBuilder(matched
                ? "Mình gợi ý một vài khóa phù hợp với bạn:\n"
                : "Mình chưa tìm thấy khóa khớp hẳn với yêu cầu, bạn tham khảo các khóa mới nhất nhé:\n");
        for (int i = 0; i < Math.min(3, scores.size()); i++) {
            Course c = ranked.get(scores.get(i)[0]);
            if (matched && scores.get(i)[1] == 0) {
                break;
            }
            sb.append("• [").append(c.getTitle()).append("](/courses/").append(c.getId()).append(") — ")
                    .append(c.isFree() ? "Miễn phí" : c.getPrice().toPlainString() + " đ")
                    .append(", ").append(c.getLevel().getLabel()).append('\n');
        }
        sb.append("Xem thêm tại [Tất cả khóa học](/courses).");
        return sb.toString();
    }

    private static final Set<String> STOP_WORDS = Set.of("toi", "minh", "muon", "hoc", "khoa", "can", "tu", "van",
            "cho", "la", "gi", "co", "nao", "the", "nhung", "cac", "mot", "hay", "ban", "xin", "chao", "duoc",
            "khong", "gioi", "thieu", "tim", "nen", "bat", "dau", "cua", "va", "de", "den");

    // bỏ dấu tiếng Việt + chữ thường để so khớp từ khóa
    static String normalize(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return s.toLowerCase();
    }

    private void checkRateLimit(String clientIp) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requestLog.computeIfAbsent(clientIp == null ? "?" : clientIp, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > RATE_WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= RATE_LIMIT) {
                throw new BusinessException("Bạn hỏi hơi nhanh, vui lòng thử lại sau vài phút");
            }
            times.addLast(now);
        }
    }
}
