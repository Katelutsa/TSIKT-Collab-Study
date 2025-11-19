package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.Task;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.TaskRepository;
import com.example.server.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.server.exception.ResourceNotFoundException;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository,
                          GroupRepository groupRepository,
                          UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    // GET /api/tasks/{id} — одна задача
    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable("id") Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    // GET /api/tasks/by-group/{groupId} — всі задачі групи
    @GetMapping("/by-group/{groupId}")
    public List<Task> getTasksByGroup(@PathVariable("groupId") Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return taskRepository.findByGroupOrderByDeadlineAsc(group);
    }

    // GET /api/tasks/by-group/{groupId}/by-status?status=OPEN
    @GetMapping("/by-group/{groupId}/by-status")
    public List<Task> getTasksByGroupAndStatus(
            @PathVariable Long groupId,
            @RequestParam String status
    ) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return taskRepository.findByGroupAndStatus(group, status);
    }

    // GET /api/tasks/by-group/{groupId}/due-before?deadline=2025-01-20T23:59:59
    @GetMapping("/by-group/{groupId}/due-before")
    public List<Task> getTasksByGroupAndDeadlineBefore(
            @PathVariable Long groupId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime deadline
    ) {
        Optional<Group> optionalGroup = groupRepository.findById(groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return taskRepository.findByGroupAndDeadlineBefore(group, deadline);
    }
    // POST /api/tasks?groupId=...&creatorId=...
        @PostMapping
        public Task createTask(
                @RequestParam("groupId") Long groupId,
                @RequestParam("creatorId") Long creatorId,
                @RequestBody Task task
) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Group with id " + groupId + " not found"));

            User creator = userRepository.findById(creatorId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User with id " + creatorId + " not found"));

            task.setGroup(group);
            task.setCreatedBy(creator);

            if (task.getCreatedAt() == null) {
                task.setCreatedAt(LocalDateTime.now());
            }
            if (task.getStatus() == null) {
                task.setStatus("OPEN");
            }

            return taskRepository.save(task);
        }

    // PATCH /api/tasks/{id}/status?status=DONE
    @PatchMapping("/{id}/status")
    public Task updateTaskStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status
    ) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task with id " + id + " not found"));

        task.setStatus(status);
        return taskRepository.save(task);
    }
}

