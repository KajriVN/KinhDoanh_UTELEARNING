package com.utelearn.repository;

import com.utelearn.entity.Assignment;
import com.utelearn.entity.Course;
import com.utelearn.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourseOrderByCreatedAtDesc(Course course);

    List<Assignment> findByLessonOrderByCreatedAtAsc(Lesson lesson);

    long countByCourse(Course course);

    void deleteByCourse(Course course);

    // xóa bài học thì bài tập vẫn giữ, chỉ bỏ liên kết với bài học
    @Modifying
    @Query("UPDATE Assignment a SET a.lesson = null WHERE a.lesson = :lesson")
    void clearLesson(@Param("lesson") Lesson lesson);
}
