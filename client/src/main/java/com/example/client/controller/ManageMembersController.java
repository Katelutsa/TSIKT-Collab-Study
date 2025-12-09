package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.GroupListItem;
import com.example.client.dto.MembershipDto;
import com.example.client.dto.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.example.client.dto.GroupDto;

public class ManageMembersController {

    @FXML
    private Label groupLabel;

    @FXML
    private TableView<MembershipDto> membersTable;

    @FXML
    private TableColumn<MembershipDto, String> nameColumn;

    @FXML
    private TableColumn<MembershipDto, String> emailColumn;

    @FXML
    private TableColumn<MembershipDto, String> roleColumn;

    @FXML
    private TableColumn<MembershipDto, String> joinedAtColumn;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label messageLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<MembershipDto> membersData = FXCollections.observableArrayList();

    private Long groupId;
    private String groupName;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setGroup(GroupDto group) {
        this.groupId = group.getGroupId();
        this.groupName = group.getName();
        groupLabel.setText("Members of group: " + groupName + " (id=" + groupId + ")");
        loadMembers();
    }

    @FXML
    private void initialize() {
        membersTable.setItems(membersData);

        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getUser() != null ? cell.getValue().getUser().getName() : ""
                ));
        emailColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getUser() != null ? cell.getValue().getUser().getEmail() : ""
                ));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole()));
        joinedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getJoinedAt()));

        roleComboBox.setItems(FXCollections.observableArrayList("MEMBER", "ADMIN", "OWNER"));
        roleComboBox.getSelectionModel().select("MEMBER");

        messageLabel.setText("");
    }

    private void loadMembers() {
        if (groupId == null) {
            return;
        }

        messageLabel.setStyle("-fx-text-fill: black;");
        messageLabel.setText("Loading members...");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/memberships/by-group/" + groupId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<MembershipDto> list = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<MembershipDto>>() {}
                    );

                    Platform.runLater(() -> {
                        membersData.setAll(list);
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Loaded " + list.size() + " members.");
                    });
                } else {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Failed to load members. Status: " + response.statusCode());
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
    private void onAddClick() {
        String email = emailField.getText();
        String role = roleComboBox.getSelectionModel().getSelectedItem();

        if (email == null || email.isBlank()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Email is required.");
            return;
        }
        if (role == null || role.isBlank()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Role is required.");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: black;");
        messageLabel.setText("Adding member...");

        new Thread(() -> {
            try {
                // 1. Find user by email
                String findUrl = "http://localhost:8080/api/users/by-email?email=" + email;

                HttpRequest findRequest = HttpRequest.newBuilder()
                        .uri(URI.create(findUrl))
                        .GET()
                        .build();

                HttpResponse<String> findResponse =
                        httpClient.send(findRequest, HttpResponse.BodyHandlers.ofString());

                if (findResponse.statusCode() != 200) {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("User not found by this email.");
                    });
                    return;
                }

                UserDto user = objectMapper.readValue(findResponse.body(), UserDto.class);
                Long userId = user.getUserId();

                // 2. POST /api/memberships?userId=&groupId=&role=
                String postUrl = "http://localhost:8080/api/memberships"
                        + "?userId=" + userId
                        + "&groupId=" + groupId
                        + "&role=" + role;

                HttpRequest postRequest = HttpRequest.newBuilder()
                        .uri(URI.create(postUrl))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> postResponse =
                        httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

                int code = postResponse.statusCode();
                if (code == 200 || code == 201) {
                    Platform.runLater(() -> {
                        emailField.clear();
                        roleComboBox.getSelectionModel().select("MEMBER");
                        loadMembers();
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Member added.");
                    });
                } else {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Failed to add member. Status: " + code);
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
    private void onCloseClick() {
        if (stage != null) {
            stage.close();
        }
    }
}

