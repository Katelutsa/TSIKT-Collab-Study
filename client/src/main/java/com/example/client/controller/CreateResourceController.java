package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.GroupListItem;
import com.example.client.dto.ResourceDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Controller for Create Resource dialog
public class CreateResourceController {

    @FXML
    private Label groupLabel;

    @FXML
    private TextField titleField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField urlField;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ResourcesController parentController;
    private Stage stage;
    private Long groupId;

    public void setParentController(ResourcesController parentController) {
        this.parentController = parentController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setGroup(GroupListItem group) {
        this.groupId = group.getGroupId();
        groupLabel.setText(group.getName() + " (id=" + group.getGroupId() + ")");
    }

    @FXML
    private void onCreateClick() {
        String title = titleField.getText();
        String type = typeField.getText();
        String url = urlField.getText();

        if (title == null || title.isBlank()) {
            messageLabel.setText("Title is required.");
            return;
        }
        if (type == null || type.isBlank()) {
            messageLabel.setText("Type is required.");
            return;
        }
        if (url == null || url.isBlank()) {
            messageLabel.setText("URL or path is required.");
            return;
        }

        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            messageLabel.setText("No logged-in user.");
            return;
        }

        messageLabel.setText("Uploading...");

        new Thread(() -> {
            try {
                ResourceCreateRequest body = new ResourceCreateRequest();
                body.setTitle(title);
                body.setType(type);
                body.setPathOrUrl(url);

                String jsonBody = objectMapper.writeValueAsString(body);

                String endpoint = "http://localhost:8080/api/resources"
                        + "?groupId=" + groupId
                        + "&uploadedById=" + userId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int code = response.statusCode();
                if (code == 200 || code == 201) {
                    Platform.runLater(() -> {
                        parentController.reloadResourcesForGroup(groupId);
                        stage.close();
                    });
                } else {
                    Platform.runLater(() ->
                            messageLabel.setText("Failed: " + code)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() -> messageLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onCancelClick() {
        stage.close();
    }

    // Request body for backend
    public static class ResourceCreateRequest {
        private String title;
        private String type;
        private String pathOrUrl;

        public String getTitle() { return title; }
        public String getType() { return type; }
        public String getPathOrUrl() { return pathOrUrl; }

        public void setTitle(String title) { this.title = title; }
        public void setType(String type) { this.type = type; }
        public void setPathOrUrl(String pathOrUrl) { this.pathOrUrl = pathOrUrl; }
    }
}

