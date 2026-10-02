package com.utelearn.dto;

import com.utelearn.entity.Lesson;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class LessonForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập tên bài học")
    @Size(max = 200)
    private String title;

    private String content;

    // dán link Youtube / link video có sẵn
    @Size(max = 500)
    private String videoUrl;

    // hoặc upload file video (ưu tiên file nếu có)
    private MultipartFile videoFile;

    @Min(value = 0, message = "Thời lượng không hợp lệ")
    @Max(value = 600, message = "Thời lượng tối đa 600 phút")
    private int durationMinutes;

    private boolean preview;

    public static LessonForm fromEntity(Lesson lesson) {
        LessonForm form = new LessonForm();
        form.setId(lesson.getId());
        form.setTitle(lesson.getTitle());
        form.setContent(lesson.getContent());
        form.setVideoUrl(lesson.getVideoUrl());
        form.setDurationMinutes(lesson.getDurationMinutes());
        form.setPreview(lesson.isPreview());
        return form;
    }
}
