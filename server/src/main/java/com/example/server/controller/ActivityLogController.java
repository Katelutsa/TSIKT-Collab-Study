package com.example.server.controller;

import com.example.server.entity.ActivityLog;
import com.example.server.entity.User;
import com.example.server.repository.ActivityLogRepository;
import com.example.server.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.server.exception.ResourceNotFoundException;
@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogController(ActivityLogRepository activityLogRepository,
                                 UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    // GET /api/activity/by-user/{userId}
    @GetMapping("/by-user/{userId}")
    public List<ActivityLog> getLogsByUser(@PathVariable("userId") Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + userId + " not found"));

        return activityLogRepository.findByUser(user);
    }

    // GET /api/activity/by-user/{userId}/range?from=...&to=...
    @GetMapping("/by-user/{userId}/range")
    public List<ActivityLog> getLogsByUserAndRange(
            @PathVariable("userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + userId + " not found"));

        return activityLogRepository.findByUserAndTimestampBetween(user, from, to);
    }

    // GET /api/activity/search?action=created
    @GetMapping("/search")
    public List<ActivityLog> searchByAction(
            @RequestParam("action") String action
    ) {
        return activityLogRepository.findByActionContainingIgnoreCase(action);
    }
}

