package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.Resource;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.ResourceRepository;
import com.example.server.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.server.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ResourceController(ResourceRepository resourceRepository,
                              GroupRepository groupRepository,
                              UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
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
            @RequestBody Resource resource
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

        return resourceRepository.save(resource);
    }
}

