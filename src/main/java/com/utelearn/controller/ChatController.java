package com.utelearn.controller;

import com.utelearn.dto.ChatMessageDto;
import com.utelearn.entity.ChatMessage;
import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.ChatService;
import com.utelearn.service.CourseService;
import com.utelearn.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

// Chat realtime học viên <-> giảng viên
@Controller
public class ChatController {

    private final ChatService chatService;
    private final CourseService courseService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, CourseService courseService, UserService userService,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.courseService = courseService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    // Học viên mở khung chat với giảng viên của khóa
    @GetMapping("/learn/{courseId}/chat")
    public String studentChat(@PathVariable Long courseId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.findById(courseId);
        return openChat(course, me, me, model);
    }

    // Giảng viên xem danh sách các cuộc trò chuyện
    @GetMapping("/instructor/chats")
    public String instructorChats(Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("conversations", chatService.getInstructorConversations(me));
        return "chat/conversations";
    }

    // Giảng viên mở 1 cuộc trò chuyện với học viên
    @GetMapping("/instructor/chats/{courseId}/{studentId}")
    public String instructorChat(@PathVariable Long courseId, @PathVariable Long studentId,
                                 Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.findById(courseId);
        User student = userService.findById(studentId);
        return openChat(course, student, me, model);
    }

    private String openChat(Course course, User student, User me, Model model) {
        chatService.checkCanAccess(me, course, student);
        chatService.markAsRead(course, student, me);

        model.addAttribute("course", course);
        model.addAttribute("student", student);
        model.addAttribute("me", me);
        // người đang nói chuyện cùng (hiện trên header khung chat)
        model.addAttribute("partner", me.getId().equals(student.getId()) ? course.getInstructor() : student);
        model.addAttribute("messages", chatService.getMessages(course, student));
        return "chat/chat";
    }

    // Nhận tin nhắn từ client gửi lên /app/chat.send
    // rồi đẩy về cho cả học viên và giảng viên qua /user/{email}/queue/messages
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto dto, Principal principal) {
        try {
            ChatMessage saved = chatService.sendMessage(principal.getName(), dto.getCourseId(),
                    dto.getStudentId(), dto.getContent());
            ChatMessageDto out = ChatMessageDto.fromEntity(saved);

            messagingTemplate.convertAndSendToUser(saved.getStudent().getEmail(), "/queue/messages", out);
            messagingTemplate.convertAndSendToUser(saved.getCourse().getInstructor().getEmail(), "/queue/messages", out);
        } catch (BusinessException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", e.getMessage());
        }
    }
}
