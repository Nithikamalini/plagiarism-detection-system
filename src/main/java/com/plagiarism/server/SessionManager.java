package com.plagiarism.server;

import com.plagiarism.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session manager for authenticated users.
 */
public class SessionManager {

    private static final Map<String, User> sessions = new ConcurrentHashMap<>();

    public static String createSession(User user) {
        String token = "TOKEN-" + UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public static User getUser(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public static void removeSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
