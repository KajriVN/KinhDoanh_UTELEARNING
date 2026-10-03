package com.utelearn.dto;

import com.utelearn.entity.Category;
import com.utelearn.entity.Course;
import com.utelearn.entity.Enrollment;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

// Các object JSON của REST API (/api/**) dành cho app mobile
public final class ApiDtos {

    private ApiDtos() {
    }

    public record LoginRequest(@NotBlank(message = "Vui lòng nhập email") String email,
                               @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    public record UserInfo(Long id, String fullName, String email, String role, String avatarUrl) {
        public static UserInfo of(User u) {
            return new UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getAvatarUrl());
        }
    }

    public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserInfo user) {
    }

    public record CategoryInfo(Long id, String name) {
        public static CategoryInfo of(Category c) {
            return c == null ? null : new CategoryInfo(c.getId(), c.getName());
        }
    }

    public record CourseSummary(Long id, String title, String shortDescription, BigDecimal price, boolean free,
                                String level, CategoryInfo category, String instructor, String thumbnailUrl,
                                int totalLessons, Double averageRating, int reviewCount) {
        public static CourseSummary of(Course c) {
            Double avg = c.getAverageRating() == null ? null : Math.round(c.getAverageRating() * 10) / 10.0;
            return new CourseSummary(c.getId(), c.getTitle(), c.getShortDescription(), c.getPrice(), c.isFree(),
                    c.getLevel().getLabel(), CategoryInfo.of(c.getCategory()), c.getInstructor().getFullName(),
                    c.getThumbnailUrl(), c.getTotalLessons(), avg,
                    c.getReviewCount() == null ? 0 : c.getReviewCount());
        }
    }

    // videoUrl chỉ có khi người gọi được phép xem bài đó (học thử / đã mua / giảng viên)
    public record LessonInfo(Long id, String title, int durationMinutes, boolean preview, String videoUrl) {
        public static LessonInfo of(Lesson l, boolean canView) {
            return new LessonInfo(l.getId(), l.getTitle(), l.getDurationMinutes(), l.isPreview(),
                    canView ? l.getVideoUrl() : null);
        }
    }

    public record SectionInfo(Long id, String title, List<LessonInfo> lessons) {
        public static SectionInfo of(Section s, Function<Lesson, Boolean> canView) {
            return new SectionInfo(s.getId(), s.getTitle(),
                    s.getLessons().stream().map(l -> LessonInfo.of(l, canView.apply(l))).toList());
        }
    }

    public record CourseDetail(CourseSummary course, String description, long studentCount, boolean enrolled,
                               List<SectionInfo> sections) {
    }

    public record EnrollmentInfo(Long courseId, String title, String thumbnailUrl, int progressPercent,
                                 LocalDateTime enrolledAt, LocalDateTime completedAt) {
        public static EnrollmentInfo of(Enrollment e) {
            return new EnrollmentInfo(e.getCourse().getId(), e.getCourse().getTitle(), e.getCourse().getThumbnailUrl(),
                    e.getProgressPercent(), e.getEnrolledAt(), e.getCompletedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int totalPages, long totalElements) {
        public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getTotalPages(), page.getTotalElements());
        }
    }
}
