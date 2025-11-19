package com.example.server.repository;

import com.example.server.entity.Group;
import com.example.server.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Усі задачі в групі
    List<Task> findByGroup(Group group);

    // Усі задачі в групі з певним статусом
    List<Task> findByGroupAndStatus(Group group, String status);

    // Задачі з дедлайном до певного часу
    List<Task> findByGroupAndDeadlineBefore(Group group, LocalDateTime deadline);

    // Задачі в групі, відсортовані за дедлайном зростанням
    List<Task> findByGroupOrderByDeadlineAsc(Group group);
}

