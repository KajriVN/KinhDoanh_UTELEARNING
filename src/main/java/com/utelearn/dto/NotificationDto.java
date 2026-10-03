package com.utelearn.dto;

import com.utelearn.entity.Notification;

import java.time.format.DateTimeFormatter;

// Thông báo đẩy qua WebSocket (JSON) tới /user/queue/notifications
public record NotificationDto(Long id, String title, String content, String link, String createdAt) {

    public static NotificationDto fromEntity(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getContent(), n.getLink(),
                n.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")));
    }
}
