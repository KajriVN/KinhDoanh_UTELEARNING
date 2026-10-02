package com.utelearn.entity;

public enum CourseLevel {
    BEGINNER("Cơ bản"),
    INTERMEDIATE("Trung cấp"),
    ADVANCED("Nâng cao");

    private final String label;

    CourseLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
