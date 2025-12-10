package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.GroupListItem;
import com.example.client.dto.TaskDto;
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

// Controller for edit task dialog
public class EditTaskController {

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
    private TaskDto task;
    private Long groupId;

    public void setParentController(TasksController parentController) {
        this.parentController = parentController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setContext(TaskDto task, GroupListItem group) {
        this.task = task;
        if (group != null) {
            this.groupId = group.getGroupId();
            groupLabel.setText(group.getName() + " (id=" + group.getGroupId() + ")");
        }
        if (task != null) {
            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription());
            deadlineField.setText(task.getDeadline());
            statusComboBox.getSelectionModel().select(task.getStatus());
        }
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: red;");

        statusComboBox.setItems(FXCollections.observableArrayList(
                "OPEN", "IN_PROGRESS", "DONE"
        ));
        statusComboBox.getSelectionModel().select("OPEN");
    }

    @FXML
    private void onSaveClick() {
        if (task == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("No task selected.");
            return;
        }

        String title = titleField.getText();
        String description = descriptionField.getText();
        String status = statusComboBox.getSelectionModel().getSelectedItem();
        String deadlineText = deadlineField.getText();

        if (title == null || title.isBlank()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Title is required.");
            return;
        }

        Long actorId = CurrentUser.getUserId();
        if (actorId == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("No logged-in user (actorId is null).");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: black;");
        messageLabel.setText("Updating task...");

        new Thread(() -> {
            try {
                TaskUpdateRequest body = new TaskUpdateRequest();
                body.setTitle(title);
                body.setDescription(description == null ? "" : description);
                body.setStatus(status);
                body.setDeadline(
                        deadlineText == null || deadlineText.isBlank()
                                ? null
                                : deadlineText
                );

                String jsonBody = objectMapper.writeValueAsString(body);

                // 🔹 додаємо actorId до URL
                String url = "http://localhost:8080/api/tasks/"
                        + task.getTaskId()
                        + "?actorId=" + actorId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    Platform.runLater(() -> {
                        if (parentController != null && groupId != null) {
                            parentController.reloadTasksForGroup(groupId);
                        }
                        if (stage != null) {
                            stage.close();
                        }
                    });
                } else {
                    String msg = "Failed to update task. Status: " + statusCode;
                    String bodyText = response.body();
                    if (bodyText != null && !bodyText.isBlank()) {
                        msg += "\n" + bodyText;
                    }
                    String finalMsg = msg;
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText(finalMsg);
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

    // DTO for update task request
    public static class TaskUpdateRequest {
        private String title;
        private String description;
        private String status;
        private String deadline;

        public TaskUpdateRequest() {
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


