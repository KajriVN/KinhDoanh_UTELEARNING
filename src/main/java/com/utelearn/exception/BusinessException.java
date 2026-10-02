package com.utelearn.exception;

// Lỗi nghiệp vụ (vd: email đã tồn tại, giỏ hàng trống...).
// Controller bắt lỗi này rồi hiện message cho người dùng.
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
