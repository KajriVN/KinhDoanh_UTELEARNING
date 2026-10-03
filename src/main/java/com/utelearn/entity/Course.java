package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // giá tính theo VND, 0 = miễn phí
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    // lý do admin từ chối (nếu có)
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Section> sections = new ArrayList<>();

    // tính sẵn trong câu SELECT (không có cột thật), dùng cho ngôi sao ở thẻ khóa học
    @Formula("(SELECT AVG(r.rating) FROM reviews r WHERE r.course_id = id)")
    private Double averageRating;

    @Formula("(SELECT COUNT(*) FROM reviews r WHERE r.course_id = id)")
    private Integer reviewCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isFree() {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }

    public int getTotalLessons() {
        int total = 0;
        for (Section s : sections) {
            total += s.getLessons().size();
        }
        return total;
    }

    public int getTotalMinutes() {
        int total = 0;
        for (Section s : sections) {
            for (Lesson l : s.getLessons()) {
                total += l.getDurationMinutes();
            }
        }
        return total;
    }

    // tất cả bài học theo đúng thứ tự (chương 1 -> chương n)
    public List<Lesson> getAllLessons() {
        List<Lesson> list = new ArrayList<>();
        for (Section s : sections) {
            list.addAll(s.getLessons());
        }
        return list;
    }
}
