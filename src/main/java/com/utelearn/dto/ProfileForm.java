package com.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100)
    private String fullName;

    // cho phép để trống, nếu nhập thì phải là 9-11 chữ số
    @Pattern(regexp = "^$|^[0-9]{9,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 1000, message = "Giới thiệu tối đa 1000 ký tự")
    private String bio;

    private MultipartFile avatar;
}
