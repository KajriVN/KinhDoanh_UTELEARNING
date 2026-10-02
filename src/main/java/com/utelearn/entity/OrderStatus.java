package com.utelearn.entity;

public enum OrderStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),     // đã trả tiền -> học viên được ghi danh
    CANCELLED("Đã hủy"),
    FAILED("Thất bại");        // cổng thanh toán báo lỗi

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
