package com.smartdining.websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.utils.BugLog;
import com.smartdining.utils.SecurityUtil;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/ws/kitchen", configurator = EmployeeEndpointConfigurator.class)
public class KitchenWebSocketServer {
    private static final Set<Session> SESSIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        if (!SecurityUtil.hasRole(EmployeeEndpointConfigurator.getEmployee(session),
                EmployeeRole.ADMIN, EmployeeRole.KITCHEN)) {
            session.close();
            return;
        }
        SESSIONS.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        SESSIONS.remove(session);
    }

    public static void broadcast(Object payload) {
        broadcastToSessions(SESSIONS, payload);
    }

    static void broadcastToSessions(Set<Session> sessions, Object payload) {
        final String jsonMessage;
        try {
            jsonMessage = MAPPER.writeValueAsString(payload);
        } catch (Exception ignored) {
            return;
        }

        for (Session session : sessions) {
            try {
                if (!session.isOpen()) {
                    sessions.remove(session);
                    continue;
                }
                session.getAsyncRemote().sendText(jsonMessage, result -> {
                    if (!result.isOK()) {
                        sessions.remove(session);
                    }
                });
            } catch (IllegalStateException ex) {
                sessions.remove(session);
                BugLog.warn(4, "WebSocket " + session.getRequestURI().getPath() + " (phien " + session.getId()
                        + ") dang gui tin truoc chua xong thi bi gui chong -> " + ex.getMessage()
                        + ". Phien bi loai khoi danh sach nhung KET NOI VAN MO: man hinh nay se khong nhan su kien nao nua.");
            }
        }
    }
}
