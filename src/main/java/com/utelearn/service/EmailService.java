package com.utelearn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Gửi email qua SMTP (Gmail App Password). Chưa cấu hình MAIL_USERNAME thì chỉ in nội dung ra log để demo.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String username;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username == null || username.isBlank()) {
            log.info("[DEV - chưa cấu hình MAIL_USERNAME] Email gửi tới {} | {} | {}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(username);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            // không báo lỗi cho người dùng (tránh lộ email có tồn tại hay không), chỉ ghi log
            log.warn("Gửi email tới {} thất bại: {}", to, e.getMessage());
        }
    }
}
