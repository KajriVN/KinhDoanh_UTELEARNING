package com.utelearn.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.utelearn.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Upload file lên Cloudinary.
// Nếu chưa điền cloudinary.* trong application.properties thì lưu tạm vào thư mục uploads/ (để chạy demo).
@Service
public class FileUploadService {

    private static final List<String> IMAGE_EXT = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final List<String> VIDEO_EXT = List.of(".mp4", ".webm", ".mov");

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        checkExtension(file, IMAGE_EXT, "Chỉ chấp nhận ảnh jpg, png, gif, webp");
        return upload(file, folder);
    }

    public String uploadVideo(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        checkExtension(file, VIDEO_EXT, "Chỉ chấp nhận video mp4, webm, mov");
        return upload(file, folder);
    }

    public boolean isCloudinaryEnabled() {
        return cloudName != null && !cloudName.isBlank();
    }

    private String upload(MultipartFile file, String folder) {
        try {
            if (isCloudinaryEnabled()) {
                return uploadToCloudinary(file, folder);
            }
            return saveToLocal(file, folder);
        } catch (IOException e) {
            throw new BusinessException("Upload file thất bại: " + e.getMessage());
        }
    }

    private String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
        }
        // resource_type = auto -> Cloudinary tự nhận ảnh hay video
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "utelearn/" + folder,
                "resource_type", "auto"));
        return (String) result.get("secure_url");
    }

    private String saveToLocal(MultipartFile file, String folder) throws IOException {
        String fileName = UUID.randomUUID() + getExtension(file);
        Path dir = Paths.get(uploadDir, folder);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + folder + "/" + fileName;
    }

    private void checkExtension(MultipartFile file, List<String> allowed, String message) {
        if (!allowed.contains(getExtension(file))) {
            throw new BusinessException(message);
        }
    }

    private String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.')).toLowerCase();
    }
}
