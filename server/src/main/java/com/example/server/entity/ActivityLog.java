package com.example.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log")

public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Action cannot be blank")
    @Size(min = 2, max = 100, message = "Action must be between 2 and 100 characters")
    @Column(nullable = false)
    private String action;

    @NotNull(message = "Timestamp cannot be null")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Size(max = 500, message = "Details cannot exceed 500 characters")
    private String details;

    public ActivityLog() {
    }

    public ActivityLog(User user, String action, LocalDateTime timestamp, String details) {
        this.user = user;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    // --- getters & setters ---

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
