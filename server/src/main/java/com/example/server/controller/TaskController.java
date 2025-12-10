package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.Task;
import com.example.server.entity.User;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.TaskRepository;
import com.example.server.repository.UserRepository;
import com.example.server.service.ActivityLogService;
import com.example.server.websocket.WebSocketEventController;
import com.example.server.websocket.dto.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final WebSocketEventController webSocketEventController;
    private final ObjectMapper objectMapper;

    public TaskController(TaskRepository taskRepository,
                          GroupRepository groupRepository,
                          UserRepository userRepository,
                          ActivityLogService activityLogService,
                          WebSocketEventController webSocketEventController,
                          ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.webSocketEventController = webSocketEventController;
        this.objectMapper = objectMapper;
    }

    // === HELPERS ===

    private void sendWsEvent(String type, Long taskId) {
        try {
            String json = objectMapper.writeValueAsString(
                    new WsMessage(type, taskId.toString())
            );
            webSocketEventController.broadcast(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            @PathVariable("groupId") Long groupId,
            @RequestParam("status") String status
    ) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return taskRepository.findByGroupAndStatus(group, status);
    }

    // GET /api/tasks/by-group/{groupId}/due-before?deadline=2025-01-20T23:59:59
    @GetMapping("/by-group/{groupId}/due-before")
    public List<Task> getTasksByGroupAndDeadlineBefore(
            @PathVariable("groupId") Long groupId,
            @RequestParam("deadline")
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

        Task saved = taskRepository.save(task);

        // 🔹 лог активності
        activityLogService.log(
                creatorId,
                "TASK_CREATED",
                "Created task '" + saved.getTitle() + "' (id=" + saved.getTaskId() + ") in group '" + group.getName() + "'"
        );

        // 🔹 WebSocket подія
        sendWsEvent("TASK_CREATED", saved.getTaskId());

        return saved;
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

        Task saved = taskRepository.save(existingTask);

        Long actorId = (saved.getCreatedBy() != null)
                ? saved.getCreatedBy().getUserId()
                : null;

        activityLogService.log(
                actorId,
                "TASK_UPDATED",
                "Updated task '" + saved.getTitle() + "' (id=" + saved.getTaskId() + ")"
        );

        sendWsEvent("TASK_UPDATED", saved.getTaskId());

        return saved;
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

        Task saved = taskRepository.save(task);

        Long actorId = (saved.getCreatedBy() != null)
                ? saved.getCreatedBy().getUserId()
                : null;

        activityLogService.log(
                actorId,
                "TASK_STATUS_CHANGED",
                "Changed status of task '" + saved.getTitle() + "' (id=" + saved.getTaskId() + ") to " + status
        );

        sendWsEvent("TASK_STATUS_CHANGED", saved.getTaskId());

        return saved;
    }

    // DELETE /api/tasks/{id}
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable("id") Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        Long actorId = (task.getCreatedBy() != null)
                ? task.getCreatedBy().getUserId()
                : null;

        String title = task.getTitle();
        Long taskId = task.getTaskId();

        taskRepository.delete(task);

        activityLogService.log(
                actorId,
                "TASK_DELETED",
                "Deleted task '" + title + "' (id=" + taskId + ")"
        );

        sendWsEvent("TASK_DELETED", taskId);
    }
}