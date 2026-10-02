package com.utelearn.entity;

public enum Role {
    STUDENT,     // học viên
    INSTRUCTOR,  // giảng viên
    ADMIN;       // quản trị viên

    public String getLabel() {
        switch (this) {
            case ADMIN:
                return "Quản trị viên";
            case INSTRUCTOR:
                return "Giảng viên";
            default:
                return "Học viên";
        }
    }
}
