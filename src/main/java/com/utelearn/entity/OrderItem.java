package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // lưu lại giá lúc mua (giá khóa học sau này có thể đổi)
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    public OrderItem(Order order, Course course, BigDecimal price) {
        this.order = order;
        this.course = course;
        this.price = price;
    }
}
