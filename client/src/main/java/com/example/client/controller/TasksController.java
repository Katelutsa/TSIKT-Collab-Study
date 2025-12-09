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
import javafx.scene.control.ComboBox;
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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

// Controller for the "Tasks" tab
public class TasksController {

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
                String url = "http://localhost:8080/api/tasks/" + selectedTask.getTaskId();

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



