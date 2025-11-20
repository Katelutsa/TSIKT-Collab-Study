package com.example.server;

import com.example.server.entity.Group;
import com.example.server.entity.Task;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.TaskRepository;
import com.example.server.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner demoData(
            UserRepository userRepository,
            GroupRepository groupRepository,
            TaskRepository taskRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        return args -> {
            // якщо користувач з таким email вже існує — нічого не робимо
            if (userRepository.existsByEmail("test@example.com")) {
                return;
            }

            // 1) Створюємо користувача
            User user = new User();
            user.setName("Test User");
            user.setEmail("test@example.com");
            String encodedPassword = passwordEncoder.encode("password123");
            user.setPasswordHash(encodedPassword);

            user = userRepository.save(user);
            System.out.println("Створений користувач: " + user.getUserId() + " " + user.getEmail());

            // 2) Створюємо групу
            Group group = new Group();
            group.setName("Test Group");
            group.setDescription("Група для тестів");
            group.setCreatedBy(user);
            group.setCreatedAt(LocalDateTime.now());

            group = groupRepository.save(group);
            System.out.println("Створена група: " + group.getGroupId() + " " + group.getName());

            // 3) Створюємо задачу
            Task task = new Task();
            task.setGroup(group);
            task.setCreatedBy(user);
            task.setTitle("Перша задача");
            task.setDescription("Перевірка репозиторіїв");
            task.setStatus("OPEN");
            task.setDeadline(LocalDateTime.now().plusDays(3));
            task.setCreatedAt(LocalDateTime.now());

            task = taskRepository.save(task);
            System.out.println("Створена задача: " + task.getTaskId() + " " + task.getTitle());
        };
    }

}

