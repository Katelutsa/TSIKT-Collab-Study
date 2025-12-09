package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.dto.GroupDto;
import com.example.client.GroupListItem;
import com.example.client.dto.ResourceDto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.example.client.ClientApplication;
import com.example.client.controller.EditResourceController;

import com.example.client.controller.CreateResourceController;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

// Controller for the "Resources" tab
public class ResourcesController {

    @FXML
    private ComboBox<GroupListItem> groupComboBox;

    @FXML
    private TableView<ResourceDto> resourcesTable;

    @FXML
    private TableColumn<ResourceDto, Long> idColumn;

    @FXML
    private TableColumn<ResourceDto, String> titleColumn;

    @FXML
    private TableColumn<ResourceDto, String> typeColumn;

    @FXML
    private TableColumn<ResourceDto, String> urlColumn;

    @FXML
    private TableColumn<ResourceDto, String> uploadedAtColumn;

    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<ResourceDto> resourcesData = FXCollections.observableArrayList();
    private final ObservableList<GroupListItem> groupsData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Table columns configuration
        idColumn.setCellValueFactory(new PropertyValueFactory<>("resourceId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("pathOrUrl"));
        uploadedAtColumn.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));

        resourcesTable.setItems(resourcesData);
        groupComboBox.setItems(groupsData);

        statusLabel.setText("");

        // Load groups once when tab is created
        loadGroupsForCurrentUser();
    }

    @FXML
    private void onReloadGroupsClick() {
        loadGroupsForCurrentUser();
    }

    @FXML
    private void onLoadResourcesClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group.");
            return;
        }

        loadResourcesByGroup(selectedGroup.getGroupId());
    }

    // Will be useful later (e.g. after create/edit/delete)
    public void reloadResourcesForGroup(Long groupId) {
        loadResourcesByGroup(groupId);
    }

    private void loadGroupsForCurrentUser() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("No logged-in user.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading groups...");

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
                        groupsData.clear();
                        for (GroupDto g : groupList) {
                            groupsData.add(new GroupListItem(g.getGroupId(), g.getName()));
                        }

                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + groupList.size() + " groups.");

                        if (!groupsData.isEmpty()) {
                            groupComboBox.getSelectionModel().selectFirst();
                        }
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
                    statusLabel.setText("Error loading groups: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadResourcesByGroup(Long groupId) {
        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading resources...");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/resources/by-group/" + groupId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<ResourceDto> resourceList = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<ResourceDto>>() {}
                    );

                    Platform.runLater(() -> {
                        resourcesData.setAll(resourceList);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + resourceList.size() + " resources.");
                    });
                } else {
                    String errorText = "Failed to load resources. Status: " + response.statusCode();
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText(errorText);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading resources: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onCreateResourceClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    ClientApplication.class.getResource("create-resource-view.fxml")
            );
            Parent root = loader.load();

            CreateResourceController controller = loader.getController();
            controller.setParentController(this);
            controller.setGroup(selectedGroup);

            Stage dialog = new Stage();
            dialog.setTitle("Create Resource");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));

            controller.setStage(dialog);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onEditResourceClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        ResourceDto selectedResource = resourcesTable.getSelectionModel().getSelectedItem();
        if (selectedResource == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a resource to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    ClientApplication.class.getResource("edit-resource-view.fxml")
            );
            Parent root = loader.load();

            EditResourceController controller = loader.getController();
            controller.setParentController(this);
            controller.setContext(selectedResource, selectedGroup);

            Stage dialog = new Stage();
            dialog.setTitle("Edit Resource");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            controller.setStage(dialog);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteResourceClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        ResourceDto selectedResource = resourcesTable.getSelectionModel().getSelectedItem();
        if (selectedResource == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a resource to delete.");
            return;
        }

        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Resource");
        alert.setHeaderText("Delete resource \"" + selectedResource.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // User canceled
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Deleting resource...");

        Long groupId = selectedGroup.getGroupId();

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/resources/" + selectedResource.getResourceId();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int code = response.statusCode();
                if (code == 200 || code == 204) {
                    Platform.runLater(() -> {
                        reloadResourcesForGroup(groupId);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Resource deleted.");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("Failed to delete resource. Status: " + code);
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

