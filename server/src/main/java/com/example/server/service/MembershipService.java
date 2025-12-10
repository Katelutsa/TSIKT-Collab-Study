package com.example.server.service;

import com.example.server.entity.Group;
import com.example.server.entity.Membership;
import com.example.server.entity.User;
import com.example.server.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    private boolean isOwnerByMembership(User user, Group group) {
        Optional<Membership> membershipOpt = membershipRepository.findByUserAndGroup(user, group);
        return membershipOpt
                .map(m -> "OWNER".equalsIgnoreCase(m.getRole()))
                .orElse(false);
    }

    public boolean isOwner(User user, Group group) {
        if (user == null || group == null) {
            return false;
        }
        // Власник групи — той, хто її створив
        if (group.getCreatedBy() != null &&
                group.getCreatedBy().getUserId().equals(user.getUserId())) {
            return true;
        }
        // Або роль OWNER у membership
        return isOwnerByMembership(user, group);
    }

    public boolean isAdminOrOwner(User user, Group group) {
        if (isOwner(user, group)) {
            return true;
        }
        if (user == null || group == null) {
            return false;
        }
        return membershipRepository.findByUserAndGroup(user, group)
                .map(m -> {
                    String role = m.getRole();
                    return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
                })
                .orElse(false);
    }

    public boolean isMember(User user, Group group) {
        if (user == null || group == null) {
            return false;
        }
        // Owner вважається членом групи
        if (group.getCreatedBy() != null &&
                group.getCreatedBy().getUserId().equals(user.getUserId())) {
            return true;
        }
        return membershipRepository.existsByUserAndGroup(user, group);
    }
}

