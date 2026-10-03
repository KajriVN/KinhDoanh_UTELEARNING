package com.utelearn.service;

import com.utelearn.dto.NotificationDto;
import com.utelearn.entity.Notification;
import com.utelearn.entity.User;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

// Thông báo: lưu DB + đẩy realtime tới trình duyệt qua WebSocket (chuông trên navbar)
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification notify(User user, String title, String content, String link) {
        if (user == null) {
            return null;
        }
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setContent(content != null && content.length() > 500 ? content.substring(0, 497) + "..." : content);
        n.setLink(link);
        notificationRepository.save(n);

        // chỉ đẩy sau khi giao dịch đã commit, tránh client nhận khi dữ liệu chưa lưu
        NotificationDto dto = NotificationDto.fromEntity(n);
        String email = user.getEmail();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(email, dto);
                }
            });
        } else {
            push(email, dto);
        }
        return n;
    }

    private void push(String email, NotificationDto dto) {
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", dto);
    }

    public List<Notification> getLatest(User user) {
        return notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    public Page<Notification> getPage(User user, int page) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(Math.max(page, 0), 15));
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    // bấm vào thông báo: đánh dấu đã đọc và trả về thông báo để chuyển tới link
    @Transactional
    public Notification open(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Không tìm thấy thông báo");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllRead(user);
    }

    // mở khung chat thì các thông báo tin nhắn của khung đó coi như đã đọc
    @Transactional
    public void markReadByLink(User user, String link) {
        notificationRepository.markReadByLink(user, link);
    }
}
