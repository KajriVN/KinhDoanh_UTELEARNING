package com.utelearn.controller.api;

import com.utelearn.exception.BusinessException;
import com.utelearn.service.AiChatService;
import com.utelearn.service.AiChatService.ChatReply;
import com.utelearn.service.AiChatService.ChatTurn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Chatbot AI tư vấn khóa học (widget góc phải trang chủ / trang khóa học)
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    public record ChatRequest(String message, List<ChatTurn> history) {
    }

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request, HttpServletRequest http) {
        try {
            ChatReply reply = aiChatService.chat(request.message(), request.history(), http.getRemoteAddr());
            return ResponseEntity.ok(reply);
        } catch (BusinessException e) {
            HttpStatus status = e.getMessage().contains("hỏi hơi nhanh") ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }
}
