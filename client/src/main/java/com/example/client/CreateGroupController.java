package com.example.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Controller for create group dialog
public class CreateGroupController {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private GroupsController parentController;
    private Stage stage;

    // Parent controller setter
    public void setParentController(GroupsController parentController) {
        this.parentController = parentController;
    }

    // Stage setter
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void onCreateClick() {
        String name = nameField.getText();
        String description = descriptionField.getText();

        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            messageLabel.setText("No logged-in user.");
            return;
        }

        messageLabel.setText("Creating group...");

        new Thread(() -> {
            try {
                GroupCreateRequest body = new GroupCreateRequest(name, description);
                String jsonBody = objectMapper.writeValueAsString(body);

                String url = "http://localhost:8080/api/groups?creatorId=" + userId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    // Success: refresh groups and close dialog
                    Platform.runLater(() -> {
                        if (parentController != null) {
                            parentController.reloadGroups();
                        }
                        if (stage != null) {
                            stage.close();
                        }
                    });
                } else {
                    String msg = "Failed to create group. Status: " + response.statusCode();
                    Platform.runLater(() -> messageLabel.setText(msg));
                }
            } catch (Exception e) {
                Platform.runLater(() -> messageLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onCancelClick() {
        if (stage != null) {
            stage.close();
        }
    }

    // DTO for create group request
    public static class GroupCreateRequest {
        private String name;
        private String description;

        public GroupCreateRequest() {
        }

        public GroupCreateRequest(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

