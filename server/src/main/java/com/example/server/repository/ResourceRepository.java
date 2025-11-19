package com.example.server.repository;

import com.example.server.entity.Group;
import com.example.server.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // Усі ресурси групи
    List<Resource> findByGroup(Group group);

    // Усі ресурси групи, новіші спочатку
    List<Resource> findByGroupOrderByUploadedAtDesc(Group group);
}

