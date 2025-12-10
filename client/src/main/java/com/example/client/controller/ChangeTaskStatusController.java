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
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Controller for change task status dialog
public class ChangeTaskStatusController {

    @FXML
    private Label taskLabel;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    @SuppressWarnings("FieldCanBeLocal")
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
        }
        if (task != null) {
            taskLabel.setText(task.getTitle() + " (id=" + task.getTaskId() + ")");
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
        statusComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSaveClick() {
        if (task == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("No task selected.");
            return;
        }

        String newStatus = statusComboBox.getSelectionModel().getSelectedItem();
        if (newStatus == null || newStatus.isBlank()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Please choose status.");
            return;
        }

        Long actorId = CurrentUser.getUserId();
        if (actorId == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("No logged-in user (actorId is null).");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: black;");
        messageLabel.setText("Updating status...");

        new Thread(() -> {
            try {
                // 🔹 додаємо actorId до PATCH-запиту
                String url = "http://localhost:8080/api/tasks/" + task.getTaskId()
                        + "/status?status=" + newStatus
                        + "&actorId=" + actorId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method("PATCH", HttpRequest.BodyPublishers.noBody())
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
                    String msg = "Failed to update status. Status: " + statusCode;
                    String body = response.body();
                    if (body != null && !body.isBlank()) {
                        msg += "\n" + body;
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
}


