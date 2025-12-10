package com.example.client.websocket;

public class WsMessage {
    public String type;
    public String payload;

    public WsMessage() {
    }

    public WsMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }
}

