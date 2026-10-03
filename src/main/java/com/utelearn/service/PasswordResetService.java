package com.utelearn.service;

import com.utelearn.entity.PasswordResetToken;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.repository.PasswordResetTokenRepository;
import com.utelearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// Quên mật khẩu: gửi link có token (hết hạn sau 15 phút, dùng 1 lần) qua email
@Service
public class PasswordResetService {

    private static final int EXPIRE_MINUTES = 15;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
                                PasswordEncoder passwordEncoder, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Không báo email có tồn tại hay không: email lạ thì lặng lẽ bỏ qua
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return;
        }
        // vô hiệu hóa các link cũ chưa dùng
        tokenRepository.invalidateUnusedTokens(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiredAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        tokenRepository.save(token);

        String link = baseUrl + "/auth/reset-password?token=" + token.getToken();
        emailService.send(user.getEmail(), "UTELearn - Đặt lại mật khẩu",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Bạn vừa yêu cầu đặt lại mật khẩu. Bấm vào liên kết dưới đây (có hiệu lực " + EXPIRE_MINUTES + " phút):\n"
                        + link + "\n\n"
                        + "Nếu không phải bạn yêu cầu, hãy bỏ qua email này.");
    }

    public PasswordResetToken validate(String token) {
        PasswordResetToken t = token == null ? null : tokenRepository.findByToken(token).orElse(null);
        if (t == null || t.isUsed() || t.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        return t;
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        PasswordResetToken t = validate(token);
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Mật khẩu tối thiểu 6 ký tự");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        User user = t.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        t.setUsed(true);
        tokenRepository.save(t);
    }
}
