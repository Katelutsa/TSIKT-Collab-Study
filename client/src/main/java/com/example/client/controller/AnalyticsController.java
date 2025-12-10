package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.dto.ActivityLogDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AnalyticsController {

    @FXML
    private TableView<ActivityLogDto> activityTable;

    @FXML
    private TableColumn<ActivityLogDto, Long> idColumn;

    @FXML
    private TableColumn<ActivityLogDto, String> timeColumn;

    @FXML
    private TableColumn<ActivityLogDto, String> actionColumn;

    @FXML
    private TableColumn<ActivityLogDto, String> detailsColumn;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObservableList<ActivityLogDto> activityData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(
                cell.getValue().getLogId() != null ? cell.getValue().getLogId() : 0L
        ).asObject());

        timeColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getTimestamp()));

        actionColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getAction()));

        detailsColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getDetails()));

        activityTable.setItems(activityData);

        statusLabel.setText("");

        // За замовчуванням — today в обидва DatePicker
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today);
        toDatePicker.setValue(today);

        // Одразу завантажимо всі логи поточного користувача
        loadAllForCurrentUser();
    }

    @FXML
    private void onLoadAllClick() {
        loadAllForCurrentUser();
    }

    @FXML
    private void onLoadRangeClick() {
        loadForCurrentUserByDateRange();
    }

    private void loadAllForCurrentUser() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            statusLabel.setText("No logged-in user.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Loading all activity...");
        statusLabel.setStyle("-fx-text-fill: black;");

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/activity/by-user/" + userId;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<ActivityLogDto> list = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<ActivityLogDto>>() {}
                    );

                    Platform.runLater(() -> {
                        activityData.setAll(list);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + list.size() + " activity records.");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("Failed to load activity. Status: " + response.statusCode());
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

    private void loadForCurrentUserByDateRange() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            statusLabel.setText("No logged-in user.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            statusLabel.setText("Please select both dates.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (from.isAfter(to)) {
            statusLabel.setText("Invalid range: 'from' is after 'to'.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Loading activity for date range...");
        statusLabel.setStyle("-fx-text-fill: black;");

        // Перетворюємо на LocalDateTime (00:00 і 23:59:59)
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        String fromParam = fromDateTime.toString();
        String toParam = toDateTime.toString();

        new Thread(() -> {
            try {
                String url = "http://localhost:8080/api/activity/by-user/" + userId
                        + "/range?from=" + fromParam + "&to=" + toParam;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<ActivityLogDto> list = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<ActivityLogDto>>() {}
                    );

                    Platform.runLater(() -> {
                        activityData.setAll(list);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Loaded " + list.size() + " records for range.");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("Failed to load activity. Status: " + response.statusCode());
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
