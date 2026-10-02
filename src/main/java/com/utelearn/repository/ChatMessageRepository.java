package com.utelearn.repository;

import com.utelearn.entity.ChatMessage;
import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByCourseAndStudentOrderBySentAtAsc(Course course, User student);

    // tin nhắn mới nhất của từng cuộc trò chuyện (course, student) mà giảng viên phụ trách
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.course.instructor = :instructor " +
            "AND m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 GROUP BY m2.course.id, m2.student.id) " +
            "ORDER BY m.sentAt DESC")
    List<ChatMessage> findLatestConversationsOfInstructor(@Param("instructor") User instructor);

    // đánh dấu đã đọc các tin do người kia gửi
    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true " +
            "WHERE m.course = :course AND m.student = :student AND m.sender <> :reader AND m.read = false")
    int markAsRead(@Param("course") Course course, @Param("student") User student, @Param("reader") User reader);
}
