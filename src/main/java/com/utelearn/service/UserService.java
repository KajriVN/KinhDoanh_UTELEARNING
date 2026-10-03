package com.utelearn.service;

import com.utelearn.dto.ChangePasswordForm;
import com.utelearn.dto.ProfileForm;
import com.utelearn.dto.RegisterForm;
import com.utelearn.entity.Role;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
    }

    // ===== Đăng ký =====
    @Transactional
    public User register(RegisterForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email này đã được đăng ký");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        // mặc định là học viên; tích "làm giảng viên" thì là giảng viên (khóa học vẫn phải qua admin duyệt)
        user.setRole(form.isInstructor() ? Role.INSTRUCTOR : Role.STUDENT);
        return userRepository.save(user);
    }

    // Học viên đang dùng tài khoản muốn trở thành giảng viên
    @Transactional
    public void becomeInstructor(User user) {
        if (user.getRole() != Role.STUDENT) {
            throw new BusinessException("Tài khoản của bạn đã là giảng viên hoặc quản trị viên");
        }
        user.setRole(Role.INSTRUCTOR);
        userRepository.save(user);
    }

    // ===== Tìm user =====
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    // lấy user đang đăng nhập, chưa đăng nhập thì trả về null
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    // ===== Hồ sơ cá nhân =====
    @Transactional
    public void updateProfile(User user, ProfileForm form) {
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone());
        user.setBio(form.getBio());

        String avatarUrl = fileUploadService.uploadImage(form.getAvatar(), "avatars");
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordForm form) {
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
    }

    // ===== Admin quản lý người dùng =====
    public Page<User> search(String keyword, Role role, int page) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return userRepository.search(keyword, role, pageable);
    }

    @Transactional
    public void toggleEnabled(Long userId, User admin) {
        User user = findById(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BusinessException("Không thể tự khóa tài khoản của mình");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void changeRole(Long userId, Role role, User admin) {
        User user = findById(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BusinessException("Không thể tự đổi vai trò của mình");
        }
        user.setRole(role);
        userRepository.save(user);
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
