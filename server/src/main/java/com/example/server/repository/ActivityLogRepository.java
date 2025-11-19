package com.example.server.repository;

import com.example.server.entity.ActivityLog;
import com.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Весь лог активностей користувача
    List<ActivityLog> findByUser(User user);

    // Лог активностей користувача за певний час
    List<ActivityLog> findByUserAndTimestampBetween(
            User user,
            LocalDateTime from,
            LocalDateTime to
    );

    // Наприклад: пошук по частині тексту дії
    List<ActivityLog> findByActionContainingIgnoreCase(String text);
}

