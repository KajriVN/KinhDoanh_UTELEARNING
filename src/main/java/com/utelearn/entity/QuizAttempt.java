package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Một lần làm bài của học viên (làm lại được nhiều lần)
@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // số câu đúng / tổng số câu lúc làm bài
    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private boolean passed;

    // đáp án đã chọn, dạng "idCâuHỏi:đápÁn,idCâuHỏi:đápÁn" (để xem lại bài)
    @Column(length = 2000)
    private String answers;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @PrePersist
    public void prePersist() {
        takenAt = LocalDateTime.now();
    }

    public int getPercent() {
        return total == 0 ? 0 : (int) Math.round(score * 100.0 / total);
    }

    // đọc chuỗi answers thành Map<idCâuHỏi, đápÁn>
    public Map<Long, Integer> getAnswerMap() {
        Map<Long, Integer> map = new HashMap<>();
        if (answers == null || answers.isBlank()) {
            return map;
        }
        for (String pair : answers.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                map.put(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
            }
        }
        return map;
    }
}
