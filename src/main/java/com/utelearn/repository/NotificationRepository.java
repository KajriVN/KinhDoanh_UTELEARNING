package com.utelearn.repository;

import com.utelearn.entity.Notification;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// TODO (hand-off): dùng cho chức năng thông báo realtime
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);
}
