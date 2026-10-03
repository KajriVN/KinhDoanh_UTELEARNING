package com.utelearn.repository;

import com.utelearn.entity.Lesson;
import com.utelearn.entity.LessonComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonCommentRepository extends JpaRepository<LessonComment, Long> {

    // các câu hỏi (không phải trả lời) của 1 bài học, mới nhất lên đầu
    List<LessonComment> findByLessonAndParentIsNullOrderByCreatedAtDesc(Lesson lesson);

    // xóa cả bài học: xóa câu trả lời trước rồi mới xóa câu hỏi (khóa ngoại parent_id)
    @Modifying
    @Query("DELETE FROM LessonComment c WHERE c.lesson = :lesson AND c.parent IS NOT NULL")
    void deleteRepliesByLesson(@Param("lesson") Lesson lesson);

    @Modifying
    @Query("DELETE FROM LessonComment c WHERE c.lesson = :lesson")
    void deleteAllByLesson(@Param("lesson") Lesson lesson);
}
