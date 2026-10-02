package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.Enrollment;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Ghi danh học viên vào khóa học
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseService courseService) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
    }

    // dùng chung cho: thanh toán thành công / đăng ký khóa miễn phí
    @Transactional
    public Enrollment enroll(User user, Course course) {
        Enrollment existing = enrollmentRepository.findByUserAndCourse(user, course).orElse(null);
        if (existing != null) {
            return existing;
        }
        return enrollmentRepository.save(new Enrollment(user, course));
    }

    @Transactional
    public void enrollFreeCourse(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessException("Khóa học chưa được xuất bản");
        }
        if (!course.isFree()) {
            throw new BusinessException("Khóa học có phí, vui lòng thêm vào giỏ hàng để thanh toán");
        }
        if (course.getInstructor().getId().equals(user.getId())) {
            throw new BusinessException("Bạn là giảng viên của khóa học này");
        }
        enroll(user, course);
    }

    public boolean isEnrolled(User user, Course course) {
        if (user == null) {
            return false;
        }
        return enrollmentRepository.existsByUserAndCourse(user, course);
    }

    public Enrollment findEnrollment(User user, Course course) {
        if (user == null) {
            return null;
        }
        return enrollmentRepository.findByUserAndCourse(user, course).orElse(null);
    }

    public List<Enrollment> getMyCourses(User user) {
        return enrollmentRepository.findByUserOrderByEnrolledAtDesc(user);
    }

    public List<Enrollment> getStudentsOfCourse(Course course) {
        return enrollmentRepository.findByCourseOrderByEnrolledAtDesc(course);
    }
}
