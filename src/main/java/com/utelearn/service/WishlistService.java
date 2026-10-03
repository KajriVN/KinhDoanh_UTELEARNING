package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.User;
import com.utelearn.entity.Wishlist;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Danh sách khóa học yêu thích
@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CourseService courseService;

    public WishlistService(WishlistRepository wishlistRepository, CourseService courseService) {
        this.wishlistRepository = wishlistRepository;
        this.courseService = courseService;
    }

    public List<Wishlist> findByUser(User user) {
        return wishlistRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Set<Long> getCourseIds(User user) {
        if (user == null) {
            return new HashSet<>();
        }
        return wishlistRepository.findCourseIdsByUser(user);
    }

    // đang thích -> bỏ thích, chưa thích -> thêm. Trả về true nếu sau khi bấm là "đã thích"
    @Transactional
    public boolean toggle(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        Wishlist existing = wishlistRepository.findByUserAndCourse(user, course).orElse(null);
        if (existing != null) {
            wishlistRepository.delete(existing);
            return false;
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessException("Khóa học chưa được xuất bản");
        }
        wishlistRepository.save(new Wishlist(user, course));
        return true;
    }
}
