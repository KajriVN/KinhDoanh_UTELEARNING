package com.utelearn.repository;

import com.utelearn.entity.Enrollment;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByEnrollmentAndLesson(Enrollment enrollment, Lesson lesson);

    long countByEnrollment(Enrollment enrollment);

    // lấy id các bài đã học xong để tô xanh ở sidebar
    @Query("SELECT lp.lesson.id FROM LessonProgress lp WHERE lp.enrollment = :enrollment")
    List<Long> findCompletedLessonIds(@Param("enrollment") Enrollment enrollment);

    // xóa tiến độ khi giảng viên xóa bài học (tránh lỗi khóa ngoại)
    void deleteByLesson(Lesson lesson);
}
