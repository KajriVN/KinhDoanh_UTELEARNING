package com.utelearn.repository;

import com.utelearn.entity.Category;
import com.utelearn.entity.Course;
import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // tìm kiếm khóa học, tham số nào null thì bỏ qua điều kiện đó
    @Query("SELECT c FROM Course c " +
            "WHERE c.status = :status " +
            "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
            "AND (:minPrice IS NULL OR c.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR c.price <= :maxPrice)")
    Page<Course> search(@Param("status") CourseStatus status,
                        @Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

    List<Course> findTop8ByStatusOrderByCreatedAtDesc(CourseStatus status);

    List<Course> findTop60ByStatusOrderByCreatedAtDesc(CourseStatus status);

    List<Course> findByInstructorOrderByCreatedAtDesc(User instructor);

    List<Course> findByStatusOrderByUpdatedAtAsc(CourseStatus status);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    long countByStatus(CourseStatus status);

    long countByCategory(Category category);

    // số khóa đã xuất bản theo danh mục: [tên danh mục, số khóa]
    @Query("SELECT c.category.name, COUNT(c) FROM Course c WHERE c.status = :status " +
            "GROUP BY c.category.id, c.category.name ORDER BY COUNT(c) DESC")
    List<Object[]> countByCategoryName(@Param("status") CourseStatus status);

    long countByCategoryAndStatus(Category category, CourseStatus status);
}
