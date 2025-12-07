package com.example.server.repository;

import com.example.server.entity.Group;
import com.example.server.entity.Membership;
import com.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // Усі членства певного користувача
    List<Membership> findByUser(User user);

    // Усі члени певної групи
    List<Membership> findByGroup(Group group);

    // Усі члени групи з конкретною роллю (наприклад "ADMIN")
    List<Membership> findByGroupAndRole(Group group, String role);

    boolean existsByUserAndGroup(User user, Group group);
}

