package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.Resource;
import com.example.server.entity.User;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.ResourceRepository;
import com.example.server.repository.UserRepository;
import com.example.server.service.ActivityLogService;
import com.example.server.websocket.WebSocketEventController;
import com.example.server.websocket.dto.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    private final ActivityLogService activityLogService;
    private final WebSocketEventController webSocketEventController;
    private final ObjectMapper objectMapper;

    public ResourceController(ResourceRepository resourceRepository,
                              GroupRepository groupRepository,
                              UserRepository userRepository,
                              ActivityLogService activityLogService,
                              WebSocketEventController webSocketEventController,
                              ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.webSocketEventController = webSocketEventController;
        this.objectMapper = objectMapper;
    }

    // ===== helper for WebSocket =====
    private void sendWsEvent(String type, Long resourceId) {
        try {
            String json = objectMapper.writeValueAsString(
                    new WsMessage(type, resourceId.toString())
            );
            webSocketEventController.broadcast(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GET /api/resources/by-group/{groupId}
    @GetMapping("/by-group/{groupId}")
    public List<Resource> getResourcesByGroup(@PathVariable("groupId") Long groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return resourceRepository.findByGroupOrderByUploadedAtDesc(group);
    }

    // POST /api/resources?groupId=...&uploadedById=...
    @PostMapping
    public Resource uploadResource(
            @RequestParam("groupId") Long groupId,
            @RequestParam("uploadedById") Long uploadedById,
            @Valid @RequestBody Resource resource
    ) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        User uploader = userRepository.findById(uploadedById)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + uploadedById + " not found"));

        resource.setGroup(group);
        resource.setUploadedBy(uploader);

        if (resource.getUploadedAt() == null) {
            resource.setUploadedAt(LocalDateTime.now());
        }

        Resource saved = resourceRepository.save(resource);

        // 🔹 ActivityLog
        activityLogService.log(
                uploadedById,
                "RESOURCE_CREATED",
                "Uploaded resource '" + saved.getTitle() + "' (id=" + saved.getResourceId()
                        + ") in group '" + group.getName() + "'"
        );

        // 🔹 WebSocket
        sendWsEvent("RESOURCE_CREATED", saved.getResourceId());

        return saved;
    }

    // PUT /api/resources/{id} — оновлення ресурсу
    @PutMapping("/{id}")
    public Resource updateResource(
            @PathVariable("id") Long id,
            @Valid @RequestBody Resource updatedResource
    ) {
        Resource existing = resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource with id " + id + " not found"));

        existing.setTitle(updatedResource.getTitle());
        existing.setType(updatedResource.getType());
        existing.setPathOrUrl(updatedResource.getPathOrUrl());

        Resource saved = resourceRepository.save(existing);

        // хто "власник" логів — той, хто завантажив
        Long actorId = Optional.ofNullable(saved.getUploadedBy())
                .map(User::getUserId)
                .orElse(null);

        activityLogService.log(
                actorId,
                "RESOURCE_UPDATED",
                "Updated resource '" + saved.getTitle() + "' (id=" + saved.getResourceId() + ")"
        );

        sendWsEvent("RESOURCE_UPDATED", saved.getResourceId());

        return saved;
    }

    // DELETE /api/resources/{id} — видалити ресурс
    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable("id") Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource with id " + id + " not found"));

        Long actorId = Optional.ofNullable(resource.getUploadedBy())
                .map(User::getUserId)
                .orElse(null);

        String title = resource.getTitle();
        Long resourceId = resource.getResourceId();
        String groupName = resource.getGroup() != null ? resource.getGroup().getName() : null;

        resourceRepository.delete(resource);

        activityLogService.log(
                actorId,
                "RESOURCE_DELETED",
                "Deleted resource '" + title + "' (id=" + resourceId
                        + (groupName != null ? ("), from group '" + groupName + "'") : "')")
        );

        sendWsEvent("RESOURCE_DELETED", resourceId);
    }
}