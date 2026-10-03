package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.Enrollment;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.LessonProgress;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.EnrollmentRepository;
import com.utelearn.repository.LessonProgressRepository;
import com.utelearn.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Học bài + theo dõi tiến độ
@Service
public class LearningService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final CertificateService certificateService;

    public LearningService(LessonRepository lessonRepository, LessonProgressRepository lessonProgressRepository,
                           EnrollmentRepository enrollmentRepository, EnrollmentService enrollmentService,
                           CourseService courseService, CertificateService certificateService) {
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.certificateService = certificateService;
    }

    public Lesson findLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài học"));
    }

    // Ai được xem bài học? học viên đã ghi danh, giảng viên chủ khóa, admin, hoặc bài cho học thử
    public boolean canViewLesson(User user, Course course, Lesson lesson) {
        if (lesson.isPreview()) {
            return true;
        }
        return enrollmentService.isEnrolled(user, course) || courseService.canManage(course, user);
    }

    // chọn bài để mở: có lessonId thì mở bài đó, không thì mở bài đầu tiên chưa học
    public Lesson chooseLesson(Course course, Long lessonId, List<Long> completedIds) {
        List<Lesson> lessons = course.getAllLessons();
        if (lessons.isEmpty()) {
            throw new BusinessException("Khóa học chưa có bài học nào");
        }
        if (lessonId != null) {
            for (Lesson l : lessons) {
                if (l.getId().equals(lessonId)) {
                    return l;
                }
            }
            throw new NotFoundException("Bài học không thuộc khóa học này");
        }
        for (Lesson l : lessons) {
            if (!completedIds.contains(l.getId())) {
                return l;
            }
        }
        return lessons.get(0);
    }

    public List<Long> getCompletedLessonIds(Enrollment enrollment) {
        if (enrollment == null) {
            return new ArrayList<>();
        }
        return lessonProgressRepository.findCompletedLessonIds(enrollment);
    }

    // bài kế tiếp (null nếu là bài cuối)
    public Lesson getNextLesson(Course course, Lesson current) {
        List<Lesson> lessons = course.getAllLessons();
        for (int i = 0; i < lessons.size() - 1; i++) {
            if (lessons.get(i).getId().equals(current.getId())) {
                return lessons.get(i + 1);
            }
        }
        return null;
    }

    public Lesson getPreviousLesson(Course course, Lesson current) {
        List<Lesson> lessons = course.getAllLessons();
        for (int i = 1; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(current.getId())) {
                return lessons.get(i - 1);
            }
        }
        return null;
    }

    // Đánh dấu hoàn thành 1 bài -> tính lại % tiến độ của khóa
    @Transactional
    public Enrollment markLessonComplete(User user, Long courseId, Long lessonId) {
        Course course = courseService.findById(courseId);
        Lesson lesson = findLesson(lessonId);
        if (!lesson.getSection().getCourse().getId().equals(course.getId())) {
            throw new NotFoundException("Bài học không thuộc khóa học này");
        }

        Enrollment enrollment = enrollmentService.findEnrollment(user, course);
        if (enrollment == null) {
            throw new BusinessException("Bạn chưa đăng ký khóa học này");
        }

        if (!lessonProgressRepository.existsByEnrollmentAndLesson(enrollment, lesson)) {
            lessonProgressRepository.save(new LessonProgress(enrollment, lesson));
        }

        updateProgress(enrollment);
        return enrollment;
    }

    private void updateProgress(Enrollment enrollment) {
        int totalLessons = enrollment.getCourse().getTotalLessons();
        long done = lessonProgressRepository.countByEnrollment(enrollment);

        int percent = 0;
        if (totalLessons > 0) {
            percent = (int) Math.round(done * 100.0 / totalLessons);
        }
        if (percent > 100) {
            percent = 100;
        }
        enrollment.setProgressPercent(percent);

        if (percent == 100 && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }
        enrollmentRepository.save(enrollment);
        if (percent == 100) {
            certificateService.issue(enrollment);
        }
    }
}
