package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.Review;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Đánh giá khóa học (1-5 sao): chỉ học viên đã ghi danh, mỗi người 1 đánh giá (được sửa)
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public ReviewService(ReviewRepository reviewRepository, CourseService courseService,
                         EnrollmentService enrollmentService) {
        this.reviewRepository = reviewRepository;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    public List<Review> findByCourse(Course course) {
        return reviewRepository.findByCourseOrderByCreatedAtDesc(course);
    }

    public Review findMine(User user, Course course) {
        if (user == null) {
            return null;
        }
        return reviewRepository.findByCourseAndUser(course, user).orElse(null);
    }

    // thêm mới hoặc cập nhật đánh giá của chính mình
    @Transactional
    public Review save(User user, Long courseId, int rating, String comment) {
        Course course = courseService.findById(courseId);
        if (!enrollmentService.isEnrolled(user, course)) {
            throw new BusinessException("Chỉ học viên đã đăng ký khóa học mới được đánh giá");
        }
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Điểm đánh giá từ 1 đến 5 sao");
        }
        if (comment != null && comment.length() > 2000) {
            throw new BusinessException("Nhận xét tối đa 2000 ký tự");
        }
        Review review = reviewRepository.findByCourseAndUser(course, user).orElse(null);
        if (review == null) {
            review = new Review();
            review.setCourse(course);
            review.setUser(user);
        }
        review.setRating(rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteMine(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        reviewRepository.findByCourseAndUser(course, user).ifPresent(reviewRepository::delete);
    }
}
