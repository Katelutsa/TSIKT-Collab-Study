package com.example.client.controller;

import com.example.client.CurrentUser;
import com.example.client.websocket.WebSocketClient;
import com.example.client.websocket.WsMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML
    private TabPane tabPane;

    private WebSocketClient webSocketClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @FXML
    private void initialize() {
        System.out.println("Main screen loaded.");
        System.out.println("Current user id = " + CurrentUser.getUserId());
        System.out.println("Current email = " + CurrentUser.getEmail());

        startWebSocket();
    }

    private void startWebSocket() {
        webSocketClient = new WebSocketClient();

        String url = "ws://localhost:8080/ws/updates";
        System.out.println("Connecting WebSocket to: " + url);

        webSocketClient.connect(url, this::onWebSocketMessage);
    }

    private void onWebSocketMessage(String json) {
        System.out.println("WS message in MainController: " + json);

        try {
            WsMessage msg = objectMapper.readValue(json, WsMessage.class);

            if (msg.type == null) {
                return;
            }

            switch (msg.type) {
                // ------- TASKS -------
                case "TASK_CREATED":
                case "TASK_UPDATED":
                case "TASK_STATUS_CHANGED":
                case "TASK_DELETED":
                    System.out.println("WS: task-related event = " + msg.type
                            + ", payload=" + msg.payload);

                    TasksController tasksController = TasksController.getInstance();
                    if (tasksController != null) {
                        tasksController.reloadTasksForSelectedGroup();
                    } else {
                        System.out.println("WS: TasksController instance is null");
                    }
                    break;

                // ------- RESOURCES -------
                case "RESOURCE_CREATED":
                case "RESOURCE_UPDATED":
                case "RESOURCE_DELETED":
                    System.out.println("WS: resource-related event = " + msg.type
                            + ", payload=" + msg.payload);

                    ResourcesController resourcesController = ResourcesController.getInstance();
                    if (resourcesController != null) {
                        resourcesController.reloadResourcesForSelectedGroup();
                    } else {
                        System.out.println("WS: ResourcesController instance is null");
                    }
                    break;

                default:
                    System.out.println("WS: unknown type " + msg.type);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse WS message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}




