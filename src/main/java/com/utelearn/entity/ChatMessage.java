package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Tin nhắn giữa 1 học viên và giảng viên của 1 khóa học.
// Một cuộc trò chuyện được xác định bởi cặp (course, student).
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // học viên của cuộc trò chuyện
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // người gửi (có thể là học viên hoặc giảng viên)
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @PrePersist
    public void prePersist() {
        sentAt = LocalDateTime.now();
    }
}
