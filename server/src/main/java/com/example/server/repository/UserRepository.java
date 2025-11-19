package com.example.server.repository;

import com.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Знайти користувача по email (повертає Optional, бо може не існувати)
    Optional<User> findByEmail(String email);

    // Перевірити, чи існує користувач з таким email
    boolean existsByEmail(String email);
}

