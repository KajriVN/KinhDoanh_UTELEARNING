package com.utelearn.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.utelearn.entity.Certificate;
import com.utelearn.entity.Enrollment;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Chứng chỉ hoàn thành: cấp khi học xong 100%, sinh PDF khổ A4 ngang bằng OpenPDF (font Be Vietnam Pro hỗ trợ tiếng Việt)
@Service
public class CertificateService {

    // bỏ các ký tự dễ nhầm (0/O, 1/I)
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(107, 114, 128);

    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.base-url}")
    private String baseUrl;

    public CertificateService(CertificateRepository certificateRepository, NotificationService notificationService) {
        this.certificateRepository = certificateRepository;
        this.notificationService = notificationService;
    }

    // Cấp chứng chỉ nếu chưa có (gọi lặp lại vẫn an toàn)
    @Transactional
    public Certificate issue(Enrollment enrollment) {
        Optional<Certificate> existing = certificateRepository.findByEnrollment(enrollment);
        if (existing.isPresent()) {
            return existing.get();
        }
        Certificate certificate = new Certificate();
        certificate.setEnrollment(enrollment);
        certificate.setCode(generateUniqueCode());
        certificateRepository.save(certificate);

        notificationService.notify(enrollment.getUser(), "Bạn nhận được chứng chỉ",
                "Chúc mừng! Bạn đã hoàn thành khóa \"" + enrollment.getCourse().getTitle() + "\".",
                "/certificates/" + certificate.getCode());
        return certificate;
    }

    public Certificate findByCode(String code) {
        return certificateRepository.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chứng chỉ"));
    }

    // Học viên đã học xong 100% thì trả về mã chứng chỉ (cấp bù nếu hoàn thành từ trước khi có chức năng này), chưa xong thì null
    @Transactional
    public String codeFor(Enrollment enrollment) {
        if (enrollment == null || enrollment.getProgressPercent() < 100) {
            return null;
        }
        return issue(enrollment).getCode();
    }

    // key = id ghi danh, chỉ gồm các khóa đã hoàn thành
    @Transactional
    public Map<Long, String> codesFor(List<Enrollment> enrollments) {
        Map<Long, String> codes = new HashMap<>();
        for (Enrollment e : enrollments) {
            String code = codeFor(e);
            if (code != null) {
                codes.put(e.getId(), code);
            }
        }
        return codes;
    }

    private String generateUniqueCode() {
        while (true) {
            StringBuilder sb = new StringBuilder("UTE-");
            for (int i = 0; i < 10; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (certificateRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
    }

    // ================== PDF ==================

    public byte[] buildPdf(Certificate certificate) {
        Enrollment enrollment = certificate.getEnrollment();
        String studentName = enrollment.getUser().getFullName();
        String courseTitle = enrollment.getCourse().getTitle();
        String instructor = enrollment.getCourse().getInstructor().getFullName();
        String date = certificate.getIssuedAt().format(DATE);

        try {
            BaseFont regular = loadFont("BeVietnamPro-Regular.ttf");
            BaseFont bold = loadFont("BeVietnamPro-Bold.ttf");
            BaseFont italic = loadFont("BeVietnamPro-Italic.ttf");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 60, 60, 70, 60);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle("Chứng chỉ hoàn thành - " + courseTitle);
            doc.addAuthor("UTELearn");
            doc.open();

            drawBorder(writer.getDirectContent(), doc.getPageSize());

            doc.add(centered("UTELearn", new Font(bold, 18, Font.NORMAL, PRIMARY), 0, 6));
            doc.add(centered("CHỨNG CHỈ HOÀN THÀNH", new Font(bold, 34, Font.NORMAL, TEXT), 0, 18));
            doc.add(centered("Chứng nhận", new Font(italic, 14, Font.NORMAL, MUTED), 0, 6));
            doc.add(centered(studentName, new Font(bold, 30, Font.NORMAL, PRIMARY), 0, 12));
            doc.add(centered("đã hoàn thành xuất sắc khóa học", new Font(italic, 14, Font.NORMAL, MUTED), 0, 8));
            doc.add(centered(courseTitle, new Font(bold, 22, Font.NORMAL, TEXT), 0, 18));
            doc.add(centered("Giảng viên: " + instructor + "   ·   Ngày cấp: " + date,
                    new Font(regular, 13, Font.NORMAL, TEXT), 0, 26));
            doc.add(centered("Mã chứng chỉ: " + certificate.getCode(), new Font(bold, 12, Font.NORMAL, TEXT), 0, 4));
            doc.add(centered("Xác thực tại: " + baseUrl + "/certificates/" + certificate.getCode(),
                    new Font(regular, 10, Font.NORMAL, MUTED), 0, 0));
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Không tạo được file PDF chứng chỉ", e);
        }
    }

    private Paragraph centered(String text, Font font, float spaceBefore, float spaceAfter) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        return p;
    }

    // khung viền kép quanh trang
    private void drawBorder(PdfContentByte canvas, Rectangle page) {
        canvas.setColorStroke(PRIMARY);
        canvas.setLineWidth(3f);
        canvas.rectangle(24, 24, page.getWidth() - 48, page.getHeight() - 48);
        canvas.stroke();
        canvas.setLineWidth(0.8f);
        canvas.rectangle(32, 32, page.getWidth() - 64, page.getHeight() - 64);
        canvas.stroke();
    }

    // font nhúng vào file PDF (IDENTITY_H = Unicode) để hiển thị đủ dấu tiếng Việt
    private BaseFont loadFont(String fileName) throws IOException, DocumentException {
        try (InputStream in = new ClassPathResource("fonts/" + fileName).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        }
    }
}
