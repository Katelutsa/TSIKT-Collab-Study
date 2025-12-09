package com.example.client;

// Holds currently logged-in user data (simple static storage)
public class CurrentUser {
    private static Long userId;
    private static String name;
    private static String email;

    public static void set(Long id, String n, String e) {
        userId = id;
        name = n;
        email = e;
    }

    public static Long getUserId() {
        return userId;
    }

    public static String getName() {
        return name;
    }

    public static String getEmail() {
        return email;
    }
}

