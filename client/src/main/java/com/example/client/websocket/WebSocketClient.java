package com.example.client.websocket;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WebSocketClient implements WebSocket.Listener {

    private WebSocket webSocket;
    private Consumer<String> onMessage;

    public void connect(String url, Consumer<String> onMessage) {
        this.onMessage = onMessage;

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("WS connected to " + url);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WS onOpen");
        webSocket.request(1); // просимо перше повідомлення
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String text = data.toString();
        System.out.println("WS received: " + text);

        if (onMessage != null) {
            // якщо треба оновлювати UI – робимо через Platform.runLater
            Platform.runLater(() -> onMessage.accept(text));
        }

        webSocket.request(1); // просимо наступне повідомлення
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.out.println("WS error: " + error.getMessage());
        error.printStackTrace();
    }
}

