package com.utelearn.service;

import com.utelearn.dto.CourseForm;
import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.Role;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CartItemRepository;
import com.utelearn.repository.CourseRepository;
import com.utelearn.repository.EnrollmentRepository;
import com.utelearn.repository.OrderItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CourseService {

    public static final int PAGE_SIZE = 9;

    private final CourseRepository courseRepository;
    private final CategoryService categoryService;
    private final FileUploadService fileUploadService;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    public CourseService(CourseRepository courseRepository, CategoryService categoryService,
                         FileUploadService fileUploadService, EnrollmentRepository enrollmentRepository,
                         OrderItemRepository orderItemRepository, CartItemRepository cartItemRepository) {
        this.courseRepository = courseRepository;
        this.categoryService = categoryService;
        this.fileUploadService = fileUploadService;
        this.enrollmentRepository = enrollmentRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // ================== PHÍA HỌC VIÊN / KHÁCH ==================

    // priceType: "free" | "paid" | null ; sort: "newest" | "price_asc" | "price_desc"
    public Page<Course> searchPublished(String keyword, Long categoryId, String priceType, String sort, int page) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }
        if (keyword != null) {
            keyword = keyword.trim();
        }

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if ("free".equals(priceType)) {
            maxPrice = BigDecimal.ZERO;
        } else if ("paid".equals(priceType)) {
            minPrice = BigDecimal.ONE;
        }

        Sort sortBy;
        if ("price_asc".equals(sort)) {
            sortBy = Sort.by("price").ascending();
        } else if ("price_desc".equals(sort)) {
            sortBy = Sort.by("price").descending();
        } else {
            sortBy = Sort.by("createdAt").descending();
        }

        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortBy);
        return courseRepository.search(CourseStatus.PUBLISHED, keyword, categoryId, minPrice, maxPrice, pageable);
    }

    public List<Course> getLatestCourses() {
        return courseRepository.findTop8ByStatusOrderByCreatedAtDesc(CourseStatus.PUBLISHED);
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học"));
    }

    // khóa chưa duyệt thì chỉ giảng viên chủ khóa và admin mới xem được
    public Course getCourseForDetail(Long id, User viewer) {
        Course course = findById(id);
        if (course.getStatus() != CourseStatus.PUBLISHED && !canManage(course, viewer)) {
            throw new NotFoundException("Không tìm thấy khóa học");
        }
        return course;
    }

    public long countStudents(Course course) {
        return enrollmentRepository.countByCourse(course);
    }

    // ================== PHÍA GIẢNG VIÊN ==================

    public boolean canManage(Course course, User user) {
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN || course.getInstructor().getId().equals(user.getId());
    }

    // lấy khóa học + kiểm tra quyền (không phải chủ khóa thì báo 403)
    public Course getOwnedCourse(Long id, User user) {
        Course course = findById(id);
        if (!canManage(course, user)) {
            throw new AccessDeniedException("Bạn không phải giảng viên của khóa học này");
        }
        return course;
    }

    public List<Course> findByInstructor(User instructor) {
        return courseRepository.findByInstructorOrderByCreatedAtDesc(instructor);
    }

    @Transactional
    public Course createCourse(CourseForm form, User instructor) {
        Course course = new Course();
        course.setInstructor(instructor);
        course.setStatus(CourseStatus.DRAFT);
        copyFormToCourse(form, course);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseForm form, User user) {
        Course course = getOwnedCourse(id, user);
        copyFormToCourse(form, course);
        // khóa bị từ chối thì sửa xong quay về nháp để gửi duyệt lại
        if (course.getStatus() == CourseStatus.REJECTED) {
            course.setStatus(CourseStatus.DRAFT);
        }
        return courseRepository.save(course);
    }

    private void copyFormToCourse(CourseForm form, Course course) {
        course.setTitle(form.getTitle().trim());
        course.setShortDescription(form.getShortDescription());
        course.setDescription(form.getDescription());
        course.setPrice(form.getPrice());
        course.setLevel(form.getLevel());
        course.setCategory(categoryService.findById(form.getCategoryId()));

        String thumbnailUrl = fileUploadService.uploadImage(form.getThumbnail(), "thumbnails");
        if (thumbnailUrl != null) {
            course.setThumbnailUrl(thumbnailUrl);
        }
    }

    @Transactional
    public void submitForReview(Long id, User user) {
        Course course = getOwnedCourse(id, user);
        if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.REJECTED) {
            throw new BusinessException("Khóa học đã được gửi duyệt hoặc đã xuất bản");
        }
        if (course.getTotalLessons() == 0) {
            throw new BusinessException("Khóa học phải có ít nhất 1 bài học trước khi gửi duyệt");
        }
        course.setStatus(CourseStatus.PENDING);
        course.setRejectReason(null);
        courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id, User user) {
        Course course = getOwnedCourse(id, user);
        if (enrollmentRepository.countByCourse(course) > 0 || orderItemRepository.existsByCourse(course)) {
            throw new BusinessException("Khóa học đã có học viên/đơn hàng, không thể xóa");
        }
        cartItemRepository.deleteByCourse(course);
        courseRepository.delete(course);
    }

    // ================== PHÍA ADMIN ==================

    public List<Course> findPendingCourses() {
        return courseRepository.findByStatusOrderByUpdatedAtAsc(CourseStatus.PENDING);
    }

    public Page<Course> findAllForAdmin(CourseStatus status, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("createdAt").descending());
        if (status == null) {
            return courseRepository.findAll(pageable);
        }
        return courseRepository.findByStatus(status, pageable);
    }

    @Transactional
    public void approve(Long id) {
        Course course = findById(id);
        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BusinessException("Khóa học không ở trạng thái chờ duyệt");
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setRejectReason(null);
        courseRepository.save(course);
        // TODO (hand-off): gửi Notification cho giảng viên
    }

    @Transactional
    public void reject(Long id, String reason) {
        Course course = findById(id);
        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BusinessException("Khóa học không ở trạng thái chờ duyệt");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do từ chối");
        }
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectReason(reason.trim());
        courseRepository.save(course);
    }

    public long countByStatus(CourseStatus status) {
        return courseRepository.countByStatus(status);
    }
}
