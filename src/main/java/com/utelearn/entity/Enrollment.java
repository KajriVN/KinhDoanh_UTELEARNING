package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Học viên đã ghi danh vào khóa học (sau khi thanh toán hoặc khóa miễn phí)
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    // % tiến độ (0 - 100)
    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    // thời điểm học xong 100%
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    @PrePersist
    public void prePersist() {
        enrolledAt = LocalDateTime.now();
    }
}
