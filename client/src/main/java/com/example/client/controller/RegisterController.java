package com.example.client.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Controller for user registration window
public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void onRegisterClick() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (name == null || name.isBlank()) {
            messageLabel.setText("Name is required.");
            return;
        }
        if (email == null || email.isBlank()) {
            messageLabel.setText("Email is required.");
            return;
        }
        if (password == null || password.isBlank()) {
            messageLabel.setText("Password is required.");
            return;
        }
        if (!password.equals(confirm)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        messageLabel.setText("Registering...");

        new Thread(() -> {
            try {
                // Backend expects User JSON with password in passwordHash field
                RegisterRequest body = new RegisterRequest();
                body.setName(name);
                body.setEmail(email);
                body.setPasswordHash(password);

                String jsonBody = objectMapper.writeValueAsString(body);

                String url = "http://localhost:8080/api/users";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status == 200 || status == 201) {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Registration successful. You can log in now.");
                        // Закриємо вікно через секунду, щоб користувач встиг прочитати
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) { }
                            Platform.runLater(() -> {
                                if (stage != null) {
                                    stage.close();
                                }
                            });
                        }).start();
                    });
                } else {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Failed: HTTP " + status);
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: red;");
                    messageLabel.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onCancelClick() {
        if (stage != null) {
            stage.close();
        }
    }

    // DTO for registration request body
    public static class RegisterRequest {
        private String name;
        private String email;
        // backend uses field passwordHash, but we send plain password here
        private String passwordHash;

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }

        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    }
}

