package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.User;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.UserRepository;
import com.example.server.service.ActivityLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public GroupController(GroupRepository groupRepository,
                           UserRepository userRepository,
                           ActivityLogService activityLogService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    // GET /api/groups — всі групи
    @GetMapping
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    // GET /api/groups/{id} — одна група по id
    @GetMapping("/{id}")
    public Group getGroupById(@PathVariable("id") Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + id + " not found"));
    }

    // GET /api/groups/by-creator/{userId} — групи, які створив користувач
    @GetMapping("/by-creator/{userId}")
    public List<Group> getGroupsByCreator(@PathVariable("userId") Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + userId + " not found"));

        return groupRepository.findByCreatedBy(user);
    }

    // GET /api/groups/by-member/{userId}
    @GetMapping("/by-member/{userId}")
    public List<Group> getGroupsByMember(@PathVariable("userId") Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + userId + " not found"));

        return groupRepository.findAllByMember(user);
    }

    // POST /api/groups?creatorId=1 — створення групи
    @PostMapping
    public Group createGroup(@RequestParam("creatorId") Long creatorId,
                             @Valid @RequestBody Group group) {

        User user = userRepository.findById(creatorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + creatorId + " not found"));

        group.setCreatedBy(user);

        if (group.getCreatedAt() == null) {
            group.setCreatedAt(LocalDateTime.now());
        }

        Group saved = groupRepository.save(group);

        // 🔹 лог активності
        activityLogService.log(
                creatorId,
                "GROUP_CREATED",
                "Created group '" + saved.getName() + "' (id=" + saved.getGroupId() + ")"
        );

        return saved;
    }

    @PutMapping("/{id}")
    public Group updateGroup(
            @PathVariable("id") Long id,
            @Valid @RequestBody Group updatedGroup
    ) {
        Group existingGroup = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + id + " not found"));

        existingGroup.setName(updatedGroup.getName());
        existingGroup.setDescription(updatedGroup.getDescription());

        Group saved = groupRepository.save(existingGroup);

        Long ownerId = (saved.getCreatedBy() != null)
                ? saved.getCreatedBy().getUserId()
                : null;

        activityLogService.log(
                ownerId,
                "GROUP_UPDATED",
                "Updated group '" + saved.getName() + "' (id=" + saved.getGroupId() + ")"
        );

        return saved;
    }

    // DELETE /api/groups/{id}
    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable("id") Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + id + " not found"));

        Long ownerId = (group.getCreatedBy() != null)
                ? group.getCreatedBy().getUserId()
                : null;

        groupRepository.delete(group);

        activityLogService.log(
                ownerId,
                "GROUP_DELETED",
                "Deleted group '" + group.getName() + "' (id=" + group.getGroupId() + ")"
        );
    }
}


