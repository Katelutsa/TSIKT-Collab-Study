package com.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

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

    // Called from CreateGroupController / EditGroupController after changes
    public void reloadGroups() {
        loadGroups();
    }

    @FXML
    private void onCreateClick() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("create-group-view.fxml"));
            Parent root = loader.load();

            CreateGroupController controller = loader.getController();
            controller.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create Group");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error opening create dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onEditClick() {
        GroupDto selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("edit-group-view.fxml"));
            Parent root = loader.load();

            EditGroupController controller = loader.getController();
            controller.setParentController(this);
            controller.setGroup(selected);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Group");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error opening edit dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteClick() {
        GroupDto selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group to delete.");
            return;
        }

        // Confirm dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Group");
        alert.setHeaderText("Delete group \"" + selected.getName() + "\"?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // User canceled
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Deleting group...");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/groups/" + selected.getGroupId();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status == 200 || status == 204) {
                    Platform.runLater(() -> {
                        reloadGroups();
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Group deleted.");
                    });
                } else {
                    String msg = "Failed to delete group. Status: " + status;
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText(msg);
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




