package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.LessonComment;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.LessonCommentRepository;
import com.utelearn.repository.LessonRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Hỏi đáp dưới bài học: học viên đã ghi danh + giảng viên chủ khóa được đăng
@Service
public class LessonCommentService {

    private final LessonCommentRepository commentRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final NotificationService notificationService;

    public LessonCommentService(LessonCommentRepository commentRepository, LessonRepository lessonRepository,
                                EnrollmentService enrollmentService, CourseService courseService,
                                NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.notificationService = notificationService;
    }

    public List<LessonComment> findByLesson(Lesson lesson) {
        return commentRepository.findByLessonAndParentIsNullOrderByCreatedAtDesc(lesson);
    }

    public boolean canPost(User user, Course course) {
        return user != null && (enrollmentService.isEnrolled(user, course) || courseService.canManage(course, user));
    }

    @Transactional
    public LessonComment post(User user, Long lessonId, Long parentId, String content) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài học"));
        Course course = lesson.getSection().getCourse();
        if (!canPost(user, course)) {
            throw new AccessDeniedException("Bạn cần đăng ký khóa học để đặt câu hỏi");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("Nội dung không được để trống");
        }
        if (content.length() > 2000) {
            throw new BusinessException("Nội dung tối đa 2000 ký tự");
        }

        LessonComment comment = new LessonComment();
        comment.setLesson(lesson);
        comment.setUser(user);
        comment.setContent(content.trim());

        if (parentId != null) {
            LessonComment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
            if (!parent.getLesson().getId().equals(lessonId)) {
                throw new BusinessException("Câu hỏi không thuộc bài học này");
            }
            // trả lời vào 1 câu trả lời thì gắn vào câu hỏi gốc (chỉ 1 cấp)
            comment.setParent(parent.getParent() != null ? parent.getParent() : parent);
        }
        commentRepository.save(comment);
        notifyAboutComment(comment, course);
        return comment;
    }

    private void notifyAboutComment(LessonComment comment, Course course) {
        String link = "/learn/" + course.getId() + "?lessonId=" + comment.getLesson().getId() + "#qa";
        User author = comment.getUser();
        if (comment.getParent() == null) {
            // câu hỏi mới -> báo giảng viên (trừ khi chính GV đăng)
            if (!course.getInstructor().getId().equals(author.getId())) {
                notificationService.notify(course.getInstructor(), "Câu hỏi mới trong bài học",
                        author.getFullName() + ": " + comment.getContent(), link);
            }
        } else if (!comment.getParent().getUser().getId().equals(author.getId())) {
            // câu trả lời -> báo người đặt câu hỏi
            notificationService.notify(comment.getParent().getUser(), "Câu hỏi của bạn có trả lời",
                    author.getFullName() + ": " + comment.getContent(), link);
        }
    }

    // người viết hoặc giảng viên chủ khóa / admin được xóa. Xóa câu hỏi thì xóa luôn các câu trả lời.
    @Transactional
    public Long delete(User user, Long commentId) {
        LessonComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận"));
        Course course = comment.getLesson().getSection().getCourse();
        boolean isAuthor = comment.getUser().getId().equals(user.getId());
        if (!isAuthor && !courseService.canManage(course, user)) {
            throw new AccessDeniedException("Bạn không có quyền xóa bình luận này");
        }
        Long lessonId = comment.getLesson().getId();
        commentRepository.delete(comment);
        return lessonId;
    }
}
