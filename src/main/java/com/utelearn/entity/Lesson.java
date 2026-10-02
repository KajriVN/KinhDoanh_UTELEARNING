package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bài học nằm trong 1 chương
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    // nội dung bài học (text thường, xuống dòng giữ nguyên)
    @Column(columnDefinition = "TEXT")
    private String content;

    // link Youtube hoặc link video Cloudinary
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int position;

    // true = cho học thử miễn phí
    @Column(nullable = false)
    private boolean preview;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    public boolean isYoutube() {
        return videoUrl != null && (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be"));
    }

    // đổi link youtube thường sang link nhúng iframe
    public String getYoutubeEmbedUrl() {
        if (!isYoutube()) {
            return null;
        }
        String videoId;
        if (videoUrl.contains("youtu.be/")) {
            videoId = videoUrl.substring(videoUrl.indexOf("youtu.be/") + 9);
        } else if (videoUrl.contains("v=")) {
            videoId = videoUrl.substring(videoUrl.indexOf("v=") + 2);
        } else {
            return videoUrl; // đã là link embed
        }
        int cut = videoId.indexOf('&');
        if (cut == -1) {
            cut = videoId.indexOf('?');
        }
        if (cut != -1) {
            videoId = videoId.substring(0, cut);
        }
        return "https://www.youtube.com/embed/" + videoId;
    }
}
