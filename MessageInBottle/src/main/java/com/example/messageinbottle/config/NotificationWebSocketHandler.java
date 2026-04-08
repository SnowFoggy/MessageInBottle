package com.example.messageinbottle.config;

import com.example.messageinbottle.dto.NotificationEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<Long, List<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session.getUri());
        if (userId == null) {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException ignored) {
            }
            return;
        }
        sessionMap.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        removeSession(session);
    }

    public void push(Long userId, NotificationEnvelope envelope) {
        List<WebSocketSession> sessions = sessionMap.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private Long extractUserId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String[] queries = uri.getQuery().split("&");
        for (String query : queries) {
            String[] parts = query.split("=");
            if (parts.length == 2 && "userId".equals(parts[0])) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private void removeSession(WebSocketSession session) {
        sessionMap.values().forEach(sessions -> sessions.remove(session));
    }
}

