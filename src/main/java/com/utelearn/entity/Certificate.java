package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Chứng chỉ hoàn thành khóa học (cấp khi tiến độ đạt 100%). File PDF được sinh lại từ dữ liệu này mỗi lần tải.
@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // mỗi lượt học chỉ có 1 chứng chỉ
    @OneToOne
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    private Enrollment enrollment;

    // mã tra cứu công khai, vd: UTE-7K2M9QXH4B
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @PrePersist
    public void prePersist() {
        issuedAt = LocalDateTime.now();
    }
}
