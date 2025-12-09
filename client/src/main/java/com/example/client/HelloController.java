package com.example.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {

    @FXML
    private Label statusLabel;

    @FXML
    private void onTestBackendClick() {
        statusLabel.setText("Button clicked!");
    }
}

