package com.utelearn.service;

import com.utelearn.dto.LessonForm;
import com.utelearn.entity.Course;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.AssignmentRepository;
import com.utelearn.repository.LessonProgressRepository;
import com.utelearn.repository.LessonRepository;
import com.utelearn.repository.SectionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Quản lý nội dung khóa học: chương (Section) và bài học (Lesson)
@Service
public class CurriculumService {

    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseService courseService;
    private final FileUploadService fileUploadService;
    private final AssignmentRepository assignmentRepository;

    public CurriculumService(SectionRepository sectionRepository, LessonRepository lessonRepository,
                             LessonProgressRepository lessonProgressRepository, CourseService courseService,
                             FileUploadService fileUploadService, AssignmentRepository assignmentRepository) {
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseService = courseService;
        this.fileUploadService = fileUploadService;
        this.assignmentRepository = assignmentRepository;
    }

    // ================== CHƯƠNG ==================

    @Transactional
    public void addSection(Long courseId, String title, User user) {
        Course course = courseService.getOwnedCourse(courseId, user);
        if (title == null || title.isBlank()) {
            throw new BusinessException("Tên chương không được để trống");
        }
        Section section = new Section();
        section.setCourse(course);
        section.setTitle(title.trim());
        section.setPosition(sectionRepository.countByCourse(course) + 1);
        sectionRepository.save(section);
    }

    @Transactional
    public void renameSection(Long sectionId, String title, User user) {
        Section section = getOwnedSection(sectionId, user);
        if (title == null || title.isBlank()) {
            throw new BusinessException("Tên chương không được để trống");
        }
        section.setTitle(title.trim());
        sectionRepository.save(section);
    }

    @Transactional
    public Long deleteSection(Long sectionId, User user) {
        Section section = getOwnedSection(sectionId, user);
        Long courseId = section.getCourse().getId();
        // xóa tiến độ học + bỏ liên kết bài tập của các bài trong chương trước, nếu không sẽ lỗi khóa ngoại
        for (Lesson lesson : section.getLessons()) {
            lessonProgressRepository.deleteByLesson(lesson);
            assignmentRepository.clearLesson(lesson);
        }
        section.getCourse().getSections().remove(section);
        sectionRepository.delete(section);
        return courseId;
    }

    public Section getOwnedSection(Long sectionId, User user) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương"));
        if (!courseService.canManage(section.getCourse(), user)) {
            throw new AccessDeniedException("Bạn không có quyền sửa chương này");
        }
        return section;
    }

    // ================== BÀI HỌC ==================

    @Transactional
    public Lesson addLesson(Long sectionId, LessonForm form, User user) {
        Section section = getOwnedSection(sectionId, user);
        Lesson lesson = new Lesson();
        lesson.setSection(section);
        lesson.setPosition(lessonRepository.countBySection(section) + 1);
        copyFormToLesson(form, lesson);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson updateLesson(Long lessonId, LessonForm form, User user) {
        Lesson lesson = getOwnedLesson(lessonId, user);
        copyFormToLesson(form, lesson);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Long deleteLesson(Long lessonId, User user) {
        Lesson lesson = getOwnedLesson(lessonId, user);
        Long courseId = lesson.getSection().getCourse().getId();
        lessonProgressRepository.deleteByLesson(lesson);
        assignmentRepository.clearLesson(lesson);
        lesson.getSection().getLessons().remove(lesson);
        lessonRepository.delete(lesson);
        return courseId;
    }

    public Lesson getOwnedLesson(Long lessonId, User user) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài học"));
        if (!courseService.canManage(lesson.getSection().getCourse(), user)) {
            throw new AccessDeniedException("Bạn không có quyền sửa bài học này");
        }
        return lesson;
    }

    private void copyFormToLesson(LessonForm form, Lesson lesson) {
        lesson.setTitle(form.getTitle().trim());
        lesson.setContent(form.getContent());
        lesson.setDurationMinutes(form.getDurationMinutes());
        lesson.setPreview(form.isPreview());

        // có upload file video thì dùng file, không thì dùng link nhập tay
        String uploadedUrl = fileUploadService.uploadVideo(form.getVideoFile(), "videos");
        if (uploadedUrl != null) {
            lesson.setVideoUrl(uploadedUrl);
        } else if (form.getVideoUrl() != null && !form.getVideoUrl().isBlank()) {
            lesson.setVideoUrl(form.getVideoUrl().trim());
        } else {
            lesson.setVideoUrl(null);
        }
    }
}
