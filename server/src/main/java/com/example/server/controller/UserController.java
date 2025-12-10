package com.example.server.controller;

import com.example.server.entity.User;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.repository.UserRepository;
import com.example.server.service.ActivityLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    // Spring сам передасть реалізацію UserRepository, BCryptPasswordEncoder і ActivityLogService
    public UserController(UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
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

    // POST /api/users/login — вхід користувача (email + пароль)
    @PostMapping("/login")
    public User login(@RequestBody com.example.server.dto.LoginRequest request) {
        // 1. Знаходимо користувача по email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        // 2. Перевіряємо пароль
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            // Спеціально не уточнюємо, що не так — email чи пароль
            throw new ResourceNotFoundException("Invalid email or password");
        }

        // 🔹 Лог активності: успішний логін
        activityLogService.log(
                user.getUserId(),
                "USER_LOGGED_IN",
                "User logged in with email " + user.getEmail()
        );

        // 3. Щоб не віддавати хеш пароля в JSON — зануляємо його
        user.setPasswordHash(null);

        return user;
    }

    // POST /api/users — створення нового користувача (реєстрація)
    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        // Беремо "сирий" пароль з поля passwordHash (тимчасово так, щоб не міняти entity/JSON)
        String rawPassword = user.getPasswordHash();

        // Хешуємо пароль перед збереженням
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPasswordHash(encodedPassword);

        User saved = userRepository.save(user);

        // 🔹 Лог активності: реєстрація
        activityLogService.log(
                saved.getUserId(),
                "USER_REGISTERED",
                "New user registered with email " + saved.getEmail()
        );

        // Щоб знову не світити хеш в JSON
        saved.setPasswordHash(null);

        return saved;
    }

    @PutMapping("/{id}")
    public User updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody User updatedUser
    ) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        // Оновлюємо поля, які дозволено змінювати
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());

        // Якщо в тілі прийшов новий пароль — оновлюємо його
        if (updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isBlank()) {
            String encodedPassword = passwordEncoder.encode(updatedUser.getPasswordHash());
            existingUser.setPasswordHash(encodedPassword);
        }

        User saved = userRepository.save(existingUser);

        // 🔹 Лог активності: оновлення профілю
        activityLogService.log(
                saved.getUserId(),
                "USER_UPDATED",
                "User profile updated (email=" + saved.getEmail() + ")"
        );

        saved.setPasswordHash(null);

        return saved;
    }

    // DELETE /api/users/{id} — видалити користувача
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        String email = user.getEmail();

        userRepository.delete(user);

        // 🔹 Лог активності: видалення користувача
        activityLogService.log(
                id,
                "USER_DELETED",
                "User with email " + email + " was deleted"
        );

        return ResponseEntity.noContent().build();
    }
}