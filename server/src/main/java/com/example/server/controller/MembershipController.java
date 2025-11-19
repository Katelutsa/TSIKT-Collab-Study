package com.example.server.controller;

import com.example.server.entity.Group;
import com.example.server.entity.Membership;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.MembershipRepository;
import com.example.server.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.server.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public MembershipController(MembershipRepository membershipRepository,
                                GroupRepository groupRepository,
                                UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    // GET /api/memberships/by-group/{groupId}
    @GetMapping("/by-group/{groupId}")
    public List<Membership> getMembershipsByGroup(@PathVariable("groupId") Long groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return membershipRepository.findByGroup(group);
    }

    // GET /api/memberships/by-group/{groupId}/by-role?role=ADMIN
    @GetMapping("/by-group/{groupId}/by-role")
    public List<Membership> getMembershipsByGroupAndRole(
            @PathVariable("groupId") Long groupId,
            @RequestParam("role") String role
    ) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        return membershipRepository.findByGroupAndRole(group, role);
    }

    // POST /api/memberships?userId=...&groupId=...&role=MEMBER
    @PostMapping
    public Membership addMemberToGroup(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId,
            @RequestParam("role") String role
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with id " + userId + " not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group with id " + groupId + " not found"));

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(role);
        membership.setJoinedAt(LocalDateTime.now());

        return membershipRepository.save(membership);
    }
}

