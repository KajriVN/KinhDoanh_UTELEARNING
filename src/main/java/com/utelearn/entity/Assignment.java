package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Bài tập do giảng viên giao cho 1 khóa học.
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // bài tập có thể gắn với 1 bài học cụ thể (không bắt buộc)
    @ManyToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // file đề bài (Cloudinary), có thể null
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 10;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // đã quá hạn nộp chưa (không có hạn thì không bao giờ quá hạn)
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }
}
