package com.example.server.service;

import com.example.server.entity.ActivityLog;
import com.example.server.entity.User;
import com.example.server.repository.ActivityLogRepository;
import com.example.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Основний хелпер: лог по userId.
     * Якщо користувача немає, лог просто пропускаємо,
     * щоб це НЕ ламало основну бізнес-операцію.
     */
    public void log(Long userId, String action, String details) {
        if (userId == null) {
            return;
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                System.out.println("ActivityLogService: user " + userId + " not found, skipping log for action " + action);
                return;
            }

            User user = userOpt.get();

            ActivityLog log = new ActivityLog();
            log.setUser(user);
            log.setAction(action);
            log.setTimestamp(LocalDateTime.now());
            log.setDetails(details);

            activityLogRepository.save(log);

        } catch (Exception e) {
            // Логування помилки, але без вильоту всієї операції
            System.out.println("ActivityLogService: failed to save activity log: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void log(User user, String action, String details) {
        if (user == null) {
            return;
        }
        log(user.getUserId(), action, details);
    }
}

