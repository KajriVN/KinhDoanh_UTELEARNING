package com.utelearn.service;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Giỏ hàng lưu trong DB (bảng cart_items) để đăng nhập máy khác vẫn còn
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public CartService(CartItemRepository cartItemRepository, CourseService courseService,
                       EnrollmentService enrollmentService) {
        this.cartItemRepository = cartItemRepository;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    public List<CartItem> getItems(User user) {
        return cartItemRepository.findByUserOrderByAddedAtDesc(user);
    }

    public long count(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.countByUser(user);
    }

    public BigDecimal getTotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getCourse().getPrice());
        }
        return total;
    }

    public boolean isInCart(User user, Course course) {
        if (user == null) {
            return false;
        }
        return cartItemRepository.existsByUserAndCourse(user, course);
    }

    @Transactional
    public void addToCart(User user, Long courseId) {
        Course course = courseService.findById(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessException("Khóa học chưa được xuất bản");
        }
        if (course.isFree()) {
            throw new BusinessException("Khóa học miễn phí, bấm \"Đăng ký học\" để vào học luôn");
        }
        if (course.getInstructor().getId().equals(user.getId())) {
            throw new BusinessException("Bạn là giảng viên của khóa học này");
        }
        if (enrollmentService.isEnrolled(user, course)) {
            throw new BusinessException("Bạn đã sở hữu khóa học này");
        }
        if (cartItemRepository.existsByUserAndCourse(user, course)) {
            throw new BusinessException("Khóa học đã có trong giỏ hàng");
        }
        cartItemRepository.save(new CartItem(user, course));
    }

    @Transactional
    public void removeFromCart(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        cartItemRepository.findByUserAndCourse(user, course).ifPresent(cartItemRepository::delete);
    }
}
