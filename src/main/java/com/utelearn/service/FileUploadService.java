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
    private static final List<String> DOCUMENT_EXT = List.of(".pdf", ".doc", ".docx", ".zip", ".rar", ".txt");
    private static final long DOCUMENT_MAX_SIZE = 20L * 1024 * 1024;

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
        return upload(file, folder, "auto");
    }

    public String uploadVideo(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        checkExtension(file, VIDEO_EXT, "Chỉ chấp nhận video mp4, webm, mov");
        return upload(file, folder, "auto");
    }

    // file đề bài / bài nộp (pdf, word, zip...)
    public String uploadDocument(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        checkExtension(file, DOCUMENT_EXT, "Chỉ chấp nhận file pdf, doc, docx, zip, rar, txt");
        if (file.getSize() > DOCUMENT_MAX_SIZE) {
            throw new BusinessException("File tối đa 20MB");
        }
        return upload(file, folder, "raw");
    }

    public boolean isCloudinaryEnabled() {
        return cloudName != null && !cloudName.isBlank();
    }

    private String upload(MultipartFile file, String folder, String resourceType) {
        try {
            if (isCloudinaryEnabled()) {
                return uploadToCloudinary(file, folder, resourceType);
            }
            return saveToLocal(file, folder);
        } catch (IOException e) {
            throw new BusinessException("Upload file thất bại: " + e.getMessage());
        }
    }

    private String uploadToCloudinary(MultipartFile file, String folder, String resourceType) throws IOException {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
        }
        // resource_type = auto -> Cloudinary tự nhận ảnh hay video
        // file tài liệu dùng raw, phải tự đặt public_id kèm đuôi file, nếu không link tải về sẽ mất đuôi
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "utelearn/" + folder,
                "resource_type", resourceType);
        if ("raw".equals(resourceType)) {
            options.put("public_id", UUID.randomUUID() + getExtension(file));
        }
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
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
