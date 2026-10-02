package com.utelearn.entity;

public enum CourseStatus {
    DRAFT("Bản nháp"),         // giảng viên đang soạn
    PENDING("Chờ duyệt"),      // đã gửi, chờ admin duyệt
    PUBLISHED("Đã xuất bản"),  // đã duyệt, hiện cho học viên
    REJECTED("Bị từ chối");    // giảng viên sửa rồi gửi lại

    private final String label;

    CourseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
