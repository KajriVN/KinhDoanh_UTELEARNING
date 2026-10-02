package com.utelearn.dto;

import com.utelearn.entity.ChatMessage;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

// Dữ liệu tin nhắn gửi qua WebSocket (JSON)
@Getter
@Setter
public class ChatMessageDto {

    private Long id;
    private Long courseId;
    private Long studentId;
    private Long senderId;
    private String senderName;
    private String content;
    private String sentAt;

    public static ChatMessageDto fromEntity(ChatMessage m) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(m.getId());
        dto.setCourseId(m.getCourse().getId());
        dto.setStudentId(m.getStudent().getId());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(m.getSender().getFullName());
        dto.setContent(m.getContent());
        dto.setSentAt(m.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")));
        return dto;
    }
}
