package com.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

// Controller for the "Groups" tab
public class GroupsController {

    @FXML
    private TableView<GroupDto> groupsTable;

    @FXML
    private TableColumn<GroupDto, Long> idColumn;

    @FXML
    private TableColumn<GroupDto, String> nameColumn;

    @FXML
    private TableColumn<GroupDto, String> descriptionColumn;

    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<GroupDto> groupsData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("groupId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        groupsTable.setItems(groupsData);

        statusLabel.setText("");

        // Load groups at startup
        loadGroups();
    }

    @FXML
    private void onRefreshClick() {
        loadGroups();
    }

    private void loadGroups() {
        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading groups...");

        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("No logged-in user.");
            return;
        }

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/groups/by-creator/" + userId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<GroupDto> groupList = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<GroupDto>>() {}
                    );

                    Platform.runLater(() -> {
                        groupsData.setAll(groupList);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + groupList.size() + " groups.");
                    });
                } else {
                    String errorText = "Failed to load groups. Status: " + response.statusCode();
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText(errorText);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }
}

