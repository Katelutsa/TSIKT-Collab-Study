package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.GroupListItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Controller for create task dialog
public class CreateTaskController {

    @FXML
    private Label groupLabel;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField deadlineField;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private TasksController parentController;
    private Stage stage;
    private Long groupId;

    // Called by parent controller
    public void setParentController(TasksController parentController) {
        this.parentController = parentController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setGroup(GroupListItem group) {
        if (group != null) {
            this.groupId = group.getGroupId();
            groupLabel.setText(group.getName() + " (id=" + group.getGroupId() + ")");
        }
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
        statusComboBox.setItems(FXCollections.observableArrayList(
                "OPEN", "IN_PROGRESS", "DONE"
        ));
        statusComboBox.getSelectionModel().select("OPEN");
    }

    @FXML
    private void onCreateClick() {
        String title = titleField.getText();
        String description = descriptionField.getText();
        String status = statusComboBox.getSelectionModel().getSelectedItem();
        String deadlineText = deadlineField.getText();

        if (title == null || title.isBlank()) {
            messageLabel.setText("Title is required.");
            return;
        }

        if (groupId == null) {
            messageLabel.setText("No group selected.");
            return;
        }

        Long creatorId = CurrentUser.getUserId();
        if (creatorId == null) {
            messageLabel.setText("No logged-in user.");
            return;
        }

        messageLabel.setText("Creating task...");

        new Thread(() -> {
            try {
                TaskCreateRequest body = new TaskCreateRequest();
                body.setTitle(title);
                body.setDescription(description == null ? "" : description);
                body.setStatus(status);
                // If deadline is empty, keep null (backend will accept null)
                body.setDeadline(deadlineText == null || deadlineText.isBlank() ? null : deadlineText);

                String jsonBody = objectMapper.writeValueAsString(body);

                String url = "http://localhost:8080/api/tasks?groupId=" + groupId + "&creatorId=" + creatorId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200 || statusCode == 201) {
                    Platform.runLater(() -> {
                        if (parentController != null) {
                            parentController.reloadTasksForGroup(groupId);
                        }
                        if (stage != null) {
                            stage.close();
                        }
                    });
                } else {
                    String msg = "Failed to create task. Status: " + statusCode;
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

    // DTO for create task request body
    public static class TaskCreateRequest {
        private String title;
        private String description;
        private String status;
        private String deadline; // ISO datetime or null

        public TaskCreateRequest() {
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }
    }
}

