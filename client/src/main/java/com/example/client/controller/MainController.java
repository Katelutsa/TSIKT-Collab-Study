package com.example.client.controller;

import com.example.client.CurrentUser;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

// Controller for the main dashboard window
public class MainController {

    @FXML
    private TabPane tabPane;

    @FXML
    private void initialize() {
        System.out.println("Main screen loaded.");
        System.out.println("Current user id = " + CurrentUser.getUserId());
        System.out.println("Current email = " + CurrentUser.getEmail());
    }
}

