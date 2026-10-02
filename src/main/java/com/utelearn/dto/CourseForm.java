package com.utelearn.dto;

import com.utelearn.entity.Course;
import com.utelearn.entity.CourseLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

// Dữ liệu form tạo / sửa khóa học của giảng viên
@Getter
@Setter
public class CourseForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập tên khóa học")
    @Size(max = 200, message = "Tên khóa học tối đa 200 ký tự")
    private String title;

    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
    private String shortDescription;

    private String description;

    @NotNull(message = "Vui lòng nhập giá (0 = miễn phí)")
    @Min(value = 0, message = "Giá không được âm")
    @Max(value = 100000000, message = "Giá tối đa 100.000.000 đ")
    private BigDecimal price;

    @NotNull(message = "Vui lòng chọn trình độ")
    private CourseLevel level;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    private MultipartFile thumbnail;

    // để hiện ảnh cũ khi sửa
    private String currentThumbnailUrl;

    // đổ dữ liệu từ entity ra form khi bấm "Sửa"
    public static CourseForm fromEntity(Course course) {
        CourseForm form = new CourseForm();
        form.setId(course.getId());
        form.setTitle(course.getTitle());
        form.setShortDescription(course.getShortDescription());
        form.setDescription(course.getDescription());
        form.setPrice(course.getPrice());
        form.setLevel(course.getLevel());
        form.setCategoryId(course.getCategory() != null ? course.getCategory().getId() : null);
        form.setCurrentThumbnailUrl(course.getThumbnailUrl());
        return form;
    }
}
