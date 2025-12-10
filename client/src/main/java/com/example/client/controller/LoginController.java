package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.CurrentUser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

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

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/users/login"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                java.net.http.HttpResponse<String> response =
                        httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Deserialize backend User entity
                    UserDto user = objectMapper.readValue(response.body(), UserDto.class);

                    System.out.println("Logged in as: " + user.getEmail() + " (userId=" + user.getUserId() + ")");

                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Login successful!");

                        // Save user globally
                        CurrentUser.set(user.getUserId(), user.getName(), user.getEmail());

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
        try {
            FXMLLoader loader = new FXMLLoader(
                    ClientApplication.class.getResource("register-view.fxml")
            );
            Parent root = loader.load();

            RegisterController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.setTitle("Collab Study - Register");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));

            controller.setStage(dialog);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static class UserDto {
        private Long userId;
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
            FXMLLoader loader = new FXMLLoader(
                    ClientApplication.class.getResource("main-view.fxml")
            );

            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);

            Stage stage = new Stage();
            stage.setTitle("Collab Study - Dashboard");
            stage.setScene(scene);
            stage.show();

            System.out.println("Main screen loaded (from LoginController).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



