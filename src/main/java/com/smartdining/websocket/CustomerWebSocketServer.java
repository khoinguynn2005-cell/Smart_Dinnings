package com.smartdining.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.utils.BugLog;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/ws/customer", configurator = EmployeeEndpointConfigurator.class)
public class CustomerWebSocketServer {
    private static final Map<Session, Integer> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        Integer tableId = getTableIdFromRequest(session);
        if (tableId != null) {
            SESSIONS.put(session, tableId);
            return;
        }

        session.close();
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        SESSIONS.remove(session);
    }

    public static void broadcastToTable(Integer tableId, Object payload) {
        try {
            String jsonMessage = MAPPER.writeValueAsString(payload);
            SESSIONS.forEach((session, sessionTableId) -> {
                if (tableId.equals(sessionTableId) && session.isOpen()) {
                    session.getAsyncRemote().sendText(jsonMessage);
                }
            });
        } catch (Exception ex) {
            // Failed sockets are removed by the container's close/error callbacks.
            BugLog.warn(4, "WebSocket khach ban " + tableId + ": gui chong tin -> " + ex.getMessage()
                    + ". Vong gui bi dung giua chung, cac dien thoai con lai cua ban khong nhan duoc tin nay.");
        }
    }

    public static void broadcast(Object payload) {
        try {
            String jsonMessage = MAPPER.writeValueAsString(payload);
            SESSIONS.forEach((session, ignoredTableId) -> {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(jsonMessage);
                }
            });
        } catch (Exception ignored) {
            // Failed sockets are removed by the container's close/error callbacks.
        }
    }

    private Integer getTableIdFromRequest(Session session) {
        List<String> values = session.getRequestParameterMap().get("tableId");
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            int tableId = Integer.parseInt(values.get(0));
            return tableId > 0 ? tableId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
