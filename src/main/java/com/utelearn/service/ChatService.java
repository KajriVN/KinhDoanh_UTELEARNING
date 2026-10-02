package com.utelearn.service;

import com.utelearn.entity.ChatMessage;
import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.ChatMessageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Chat 1-1 giữa học viên và giảng viên của khóa học
@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository, CourseService courseService,
                       EnrollmentService enrollmentService, UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    // chỉ học viên đó hoặc giảng viên của khóa mới được vào cuộc trò chuyện
    public void checkCanAccess(User user, Course course, User student) {
        if (!enrollmentService.isEnrolled(student, course)) {
            throw new AccessDeniedException("Học viên chưa đăng ký khóa học này");
        }
        boolean isTheStudent = user.getId().equals(student.getId());
        boolean isInstructor = course.getInstructor().getId().equals(user.getId());
        if (!isTheStudent && !isInstructor) {
            throw new AccessDeniedException("Bạn không thuộc cuộc trò chuyện này");
        }
    }

    public List<ChatMessage> getMessages(Course course, User student) {
        return chatMessageRepository.findByCourseAndStudentOrderBySentAtAsc(course, student);
    }

    @Transactional
    public void markAsRead(Course course, User student, User reader) {
        chatMessageRepository.markAsRead(course, student, reader);
    }

    @Transactional
    public ChatMessage sendMessage(String senderEmail, Long courseId, Long studentId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("Tin nhắn trống");
        }
        if (content.length() > 1000) {
            throw new BusinessException("Tin nhắn tối đa 1000 ký tự");
        }

        User sender = userService.findByEmail(senderEmail);
        Course course = courseService.findById(courseId);
        User student = userService.findById(studentId);
        checkCanAccess(sender, course, student);

        ChatMessage message = new ChatMessage();
        message.setCourse(course);
        message.setStudent(student);
        message.setSender(sender);
        message.setContent(content.trim());
        return chatMessageRepository.save(message);
    }

    // danh sách cuộc trò chuyện của giảng viên (mỗi cuộc lấy tin mới nhất)
    public List<ChatMessage> getInstructorConversations(User instructor) {
        return chatMessageRepository.findLatestConversationsOfInstructor(instructor);
    }
}
