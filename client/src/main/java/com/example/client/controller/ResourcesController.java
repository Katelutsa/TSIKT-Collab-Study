package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.CurrentUser;
import com.example.client.GroupListItem;
import com.example.client.dto.GroupDto;
import com.example.client.dto.ResourceDto;

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

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;

public class ResourcesController {

    // ===== Static instance for MainController (WebSocket updates) =====
    private static ResourcesController instance;

    public ResourcesController() {
        instance = this;
    }

    public static ResourcesController getInstance() {
        return instance;
    }

    // ===== UI elements =====
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

    // ===== Data and HTTP =====
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<ResourceDto> resourcesData = FXCollections.observableArrayList();
    private final ObservableList<GroupListItem> groupsData = FXCollections.observableArrayList();

    // ===== Initialization =====
    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("resourceId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("pathOrUrl"));
        uploadedAtColumn.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));

        resourcesTable.setItems(resourcesData);
        groupComboBox.setItems(groupsData);

        statusLabel.setText("");

        loadGroupsForCurrentUser();
    }

    // ===========================
    //       WebSocket Reload
    // ===========================

    public void reloadResourcesForSelectedGroup() {
        GroupListItem selected = groupComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadResourcesByGroup(selected.getGroupId());
        }
    }

    // ===========================
    //       Load Groups
    // ===========================

    private void loadGroupsForCurrentUser() {

        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            statusLabel.setText("No logged-in user.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Loading groups...");
        statusLabel.setStyle("-fx-text-fill: black;");

        new Thread(() -> {
            try {
                String urlCreated = "http://localhost:8080/api/groups/by-creator/" + userId;
                String urlMember  = "http://localhost:8080/api/groups/by-member/" + userId;

                HttpResponse<String> respCreated =
                        httpClient.send(HttpRequest.newBuilder().uri(URI.create(urlCreated)).GET().build(),
                                HttpResponse.BodyHandlers.ofString());

                HttpResponse<String> respMember =
                        httpClient.send(HttpRequest.newBuilder().uri(URI.create(urlMember)).GET().build(),
                                HttpResponse.BodyHandlers.ofString());

                List<GroupDto> createdList = respCreated.statusCode() == 200
                        ? objectMapper.readValue(respCreated.body(), new TypeReference<>() {})
                        : List.of();

                List<GroupDto> memberList = respMember.statusCode() == 200
                        ? objectMapper.readValue(respMember.body(), new TypeReference<>() {})
                        : List.of();

                Map<Long, GroupListItem> map = new LinkedHashMap<>();

                for (GroupDto g : createdList)
                    map.put(g.getGroupId(), new GroupListItem(g.getGroupId(), g.getName()));

                for (GroupDto g : memberList)
                    map.putIfAbsent(g.getGroupId(), new GroupListItem(g.getGroupId(), g.getName()));

                ObservableList<GroupListItem> items = FXCollections.observableArrayList(map.values());

                Platform.runLater(() -> {
                    groupsData.setAll(items);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + items.size() + " groups.");

                    if (!groupsData.isEmpty()) {
                        groupComboBox.getSelectionModel().selectFirst();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading groups: " + e.getMessage());
                });
            }
        }).start();
    }

    // ===========================
    //       Load Resources
    // ===========================

    private void loadResourcesByGroup(Long groupId) {

        statusLabel.setText("Loading resources...");
        statusLabel.setStyle("-fx-text-fill: black;");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/resources/by-group/" + groupId;

                HttpResponse<String> response =
                        httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {

                    List<ResourceDto> list = objectMapper.readValue(
                            response.body(),
                            new TypeReference<>() {}
                    );

                    Platform.runLater(() -> {
                        resourcesData.setAll(list);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + list.size() + " resources.");
                    });

                } else {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("Failed to load resources. Status: " + response.statusCode());
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

    // ===========================
    //   Handlers for FXML buttons
    // ===========================

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

    // ===========================
    //     Create / Edit / Delete
    // ===========================

    @FXML
    private void onCreateResourceClick() {

        GroupListItem group = groupComboBox.getSelectionModel().getSelectedItem();
        if (group == null) {
            statusLabel.setText("Please select a group.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("create-resource-view.fxml"));
            Parent root = loader.load();

            CreateResourceController controller = loader.getController();
            controller.setParentController(this);
            controller.setGroup(group);

            Stage dialog = new Stage();
            dialog.setTitle("Create Resource");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));

            controller.setStage(dialog);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void onEditResourceClick() {

        GroupListItem group = groupComboBox.getSelectionModel().getSelectedItem();
        if (group == null) {
            statusLabel.setText("Please select a group first.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        ResourceDto selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a resource to edit.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("edit-resource-view.fxml"));
            Parent root = loader.load();

            EditResourceController controller = loader.getController();
            controller.setParentController(this);
            controller.setContext(selected, group);

            Stage dialog = new Stage();
            dialog.setTitle("Edit Resource");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            controller.setStage(dialog);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    public void reloadResourcesForGroup(Long groupId) {
        loadResourcesByGroup(groupId);
    }

    @FXML
    private void onDeleteResourceClick() {

        GroupListItem group = groupComboBox.getSelectionModel().getSelectedItem();
        if (group == null) {
            statusLabel.setText("Please select a group first.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        ResourceDto selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a resource to delete.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Resource");
        alert.setHeaderText("Delete resource \"" + selected.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK)
            return;

        statusLabel.setText("Deleting...");
        statusLabel.setStyle("-fx-text-fill: black;");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/resources/" + selected.getResourceId();

                HttpResponse<String> response =
                        httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).DELETE().build(),
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 204) {

                    Platform.runLater(() -> {
                        reloadResourcesForGroup(group.getGroupId());
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Resource deleted.");
                    });

                } else {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("Failed to delete. Status: " + response.statusCode());
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

