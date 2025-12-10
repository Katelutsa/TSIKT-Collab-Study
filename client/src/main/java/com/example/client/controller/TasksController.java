package com.example.client.controller;

import com.example.client.*;
import com.example.client.dto.GroupDto;
import com.example.client.dto.TaskDto;
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

// Controller for the "Tasks" tab
public class TasksController {

    // ===== статичний інстанс для доступу з MainController =====
    private static TasksController instance;

    public TasksController() {
        instance = this;
    }

    public static TasksController getInstance() {
        return instance;
    }

    @FXML
    private ComboBox<GroupListItem> groupComboBox;

    @FXML
    private TableView<TaskDto> tasksTable;

    @FXML
    private TableColumn<TaskDto, Long> idColumn;

    @FXML
    private TableColumn<TaskDto, String> titleColumn;

    @FXML
    private TableColumn<TaskDto, String> statusColumn;

    @FXML
    private TableColumn<TaskDto, String> deadlineColumn;

    @FXML
    private TableColumn<TaskDto, String> descriptionColumn;

    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<TaskDto> tasksData = FXCollections.observableArrayList();
    private final ObservableList<GroupListItem> groupsData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        tasksTable.setItems(tasksData);
        groupComboBox.setItems(groupsData);

        statusLabel.setText("");

        // Load groups for current user when tab is created
        loadGroupsForCurrentUser();
    }

    @FXML
    private void onLoadTasksClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group.");
            return;
        }

        loadTasksByGroup(selectedGroup.getGroupId());
    }

    @FXML
    private void onCreateTaskClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("create-task-view.fxml"));
            Parent root = loader.load();

            CreateTaskController controller = loader.getController();
            controller.setParentController(this);
            controller.setGroup(selectedGroup);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create Task");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error opening create task dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onEditTaskClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        TaskDto selectedTask = tasksTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a task to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("edit-task-view.fxml"));
            Parent root = loader.load();

            EditTaskController controller = loader.getController();
            controller.setParentController(this);
            controller.setContext(selectedTask, selectedGroup);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Task");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error opening edit task dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onChangeStatusClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        TaskDto selectedTask = tasksTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a task to change status.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("change-status-task-view.fxml"));
            Parent root = loader.load();

            ChangeTaskStatusController controller = loader.getController();
            controller.setParentController(this);
            controller.setContext(selectedTask, selectedGroup);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Change Task Status");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Error opening change status dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteTaskClick() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a group first.");
            return;
        }

        TaskDto selectedTask = tasksTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a task to delete.");
            return;
        }

        Long actorId = CurrentUser.getUserId();
        if (actorId == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("No logged-in user (actorId is null).");
            return;
        }

        // Confirm dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Delete task \"" + selectedTask.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // User canceled
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Deleting task...");

        Long groupId = selectedGroup.getGroupId();

        new Thread(() -> {
            try {
                // 🔹 додаємо actorId до запиту
                String url = "http://localhost:8080/api/tasks/"
                        + selectedTask.getTaskId()
                        + "?actorId=" + actorId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200 || statusCode == 204) {
                    Platform.runLater(() -> {
                        reloadTasksForGroup(groupId);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Task deleted.");
                    });
                } else {
                    String msg = "Failed to delete task. Status: " + statusCode;
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

    @FXML
    private void onReloadGroupsClick() {
        loadGroupsForCurrentUser();
    }

    // Called from CreateTaskController/EditTaskController
    public void reloadTasksForGroup(Long groupId) {
        loadTasksByGroup(groupId);
    }

    // Викликається з WebSocket (через MainController)
    public void reloadTasksForSelectedGroup() {
        GroupListItem selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            loadTasksByGroup(selectedGroup.getGroupId());
        }
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
                String urlCreated = "http://localhost:8080/api/groups/by-creator/" + userId;
                String urlMember  = "http://localhost:8080/api/groups/by-member/" + userId;

                HttpRequest reqCreated = HttpRequest.newBuilder()
                        .uri(URI.create(urlCreated))
                        .GET()
                        .build();

                HttpRequest reqMember = HttpRequest.newBuilder()
                        .uri(URI.create(urlMember))
                        .GET()
                        .build();

                HttpResponse<String> respCreated =
                        httpClient.send(reqCreated, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> respMember  =
                        httpClient.send(reqMember, HttpResponse.BodyHandlers.ofString());

                List<GroupDto> createdList = List.of();
                List<GroupDto> memberList  = List.of();

                if (respCreated.statusCode() == 200) {
                    createdList = objectMapper.readValue(
                            respCreated.body(),
                            new TypeReference<List<GroupDto>>() {}
                    );
                }

                if (respMember.statusCode() == 200) {
                    memberList = objectMapper.readValue(
                            respMember.body(),
                            new TypeReference<List<GroupDto>>() {}
                    );
                }

                // Обʼєднуємо без дублікатів (key = groupId)
                Map<Long, GroupListItem> map = new LinkedHashMap<>();
                for (GroupDto g : createdList) {
                    map.put(g.getGroupId(), new GroupListItem(g.getGroupId(), g.getName()));
                }
                for (GroupDto g : memberList) {
                    map.putIfAbsent(g.getGroupId(), new GroupListItem(g.getGroupId(), g.getName()));
                }

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

    private void loadTasksByGroup(Long groupId) {
        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading tasks...");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/tasks/by-group/" + groupId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<TaskDto> taskList = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<TaskDto>>() {}
                    );

                    Platform.runLater(() -> {
                        tasksData.setAll(taskList);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + taskList.size() + " tasks.");
                    });
                } else {
                    String errorText = "Failed to load tasks. Status: " + response.statusCode();
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText(errorText);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading tasks: " + e.getMessage());
                });
            }
        }).start();
    }
}






