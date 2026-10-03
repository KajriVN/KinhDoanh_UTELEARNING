package com.utelearn.dto;

import com.utelearn.entity.Assignment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssignmentForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập tên bài tập")
    @Size(max = 200)
    private String title;

    private String description;

    // gắn với 1 bài học (không bắt buộc)
    private Long lessonId;

    // input type="datetime-local" gửi lên dạng 2026-10-20T23:59
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueDate;

    @Min(value = 1, message = "Điểm tối đa từ 1 đến 100")
    @Max(value = 100, message = "Điểm tối đa từ 1 đến 100")
    private int maxScore = 10;

    // file đề bài (không bắt buộc)
    private MultipartFile attachmentFile;

    public static AssignmentForm fromEntity(Assignment assignment) {
        AssignmentForm form = new AssignmentForm();
        form.setId(assignment.getId());
        form.setTitle(assignment.getTitle());
        form.setDescription(assignment.getDescription());
        form.setLessonId(assignment.getLesson() != null ? assignment.getLesson().getId() : null);
        form.setDueDate(assignment.getDueDate());
        form.setMaxScore(assignment.getMaxScore());
        return form;
    }
}
