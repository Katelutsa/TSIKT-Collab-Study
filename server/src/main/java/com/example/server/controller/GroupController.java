package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

import com.example.server.exception.ResourceNotFoundException;


@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupController(GroupRepository groupRepository,
                           UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
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

        return groupRepository.save(group);
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

        return groupRepository.save(existingGroup);
    }

    // DELETE /api/groups/{id}
    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable("id") Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + id + " not found"));

        groupRepository.delete(group);
    }

}


