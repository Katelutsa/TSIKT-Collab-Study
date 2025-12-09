package com.example.server.repository;

import com.example.server.entity.Group;
import com.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // Групи, які створив конкретний користувач
    List<Group> findByCreatedBy(User createdBy);

    // Усі групи, в яких користувач є членом (через Membership)
    @Query("""
       select m.group
       from Membership m
       where m.user = :user
       """)
    List<Group> findAllByMember(@Param("user") com.example.server.entity.User user);
}

