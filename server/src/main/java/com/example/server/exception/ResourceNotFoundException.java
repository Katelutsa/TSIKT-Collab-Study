package com.example.server.exception;

// Проста кастомна RuntimeException для 404
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
