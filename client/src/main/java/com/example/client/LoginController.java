package com.example.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.IOException;

// Controller for login screen
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ObjectMapper is configured to ignore unknown JSON fields
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @FXML
    private void initialize() {
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    @FXML
    private void onLoginClick() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            messageLabel.setText("Please enter email and password.");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: black;");
        messageLabel.setText("Logging in...");

        new Thread(() -> {
            try {
                // Create JSON body: {"email": "...", "password": "..."}
                LoginRequest loginRequest = new LoginRequest(email, password);
                String jsonBody = objectMapper.writeValueAsString(loginRequest);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Deserialize backend User entity
                    UserDto user = objectMapper.readValue(response.body(), UserDto.class);

                    System.out.println("Logged in as: " + user.getEmail() + " (userId=" + user.getUserId() + ")");

                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Login successful!");

                        // Save user globally
                        CurrentUser.set(user.getUserId(), user.getName(), user.getEmail());

                        // Open main window
                        openMainWindow();

                        // Close login window
                        messageLabel.getScene().getWindow().hide();
                    });
                } else {
                    // Show error status or backend error body
                    String errorText = "Login failed. Status: " + response.statusCode();
                    String body = response.body();
                    if (body != null && !body.isBlank()) {
                        errorText += "\n" + body;
                    }

                    String finalErrorText = errorText;
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText(finalErrorText);
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
    private void onRegisterClick() {
        // TODO: Later: open registration view
        messageLabel.setStyle("-fx-text-fill: blue;");
        messageLabel.setText("Registration screen is not implemented yet.");
    }

    // DTO for login request body
    public static class LoginRequest {
        private String email;
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // DTO matching backend User JSON (only fields we care about)
    public static class UserDto {
        private Long userId; // matches entity field "userId"
        private String name;
        private String email;

        public UserDto() {
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);

            Stage stage = new Stage();
            stage.setTitle("Collab Study - Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


