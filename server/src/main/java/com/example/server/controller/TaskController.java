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

import jakarta.validation.Valid;

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
                @Valid @RequestBody Task task
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

    // PUT /api/tasks/{id} — оновлення задачі
    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody Task updatedTask
    ) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setDeadline(updatedTask.getDeadline());
        existingTask.setStatus(updatedTask.getStatus());

        return taskRepository.save(existingTask);
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

    // DELETE /api/tasks/{id}
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable("id") Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        taskRepository.delete(task);
    }
}

