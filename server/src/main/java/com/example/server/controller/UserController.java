package com.example.server.controller;

import com.example.server.entity.User;
import com.example.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.server.exception.ResourceNotFoundException;


import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    // Spring сам передасть реалізацію UserRepository в конструктор
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // GET /api/users — повертає всіх користувачів
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // GET /api/users/{id} — один користувач по id
    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }


    // GET /api/users/by-email?email=...
    @GetMapping("/by-email")
    public User getUserByEmail(@RequestParam("email") String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }


    // POST /api/users — створення нового користувача
    @PostMapping
    public User createUser(@RequestBody User user) {
        // тут потім додаси хешування пароля, валідацію тощо
        return userRepository.save(user);
    }

    // DELETE /api/users/{id} — видалити користувача
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

