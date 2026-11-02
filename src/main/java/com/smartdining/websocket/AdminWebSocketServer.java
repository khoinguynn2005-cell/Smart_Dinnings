package com.smartdining.websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.smartdining.enums.EmployeeRole;
import com.smartdining.utils.SecurityUtil;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Kenh real-time cho trang quan tri: nhan cac su kien van hanh (dat mon, thanh toan, trang thai ban)
 * va su kien du lieu nen (thuc don, ban, nhan su) de tu tai lai ma khong can F5.
 */
@ServerEndpoint(value = "/ws/admin", configurator = EmployeeEndpointConfigurator.class)
public class AdminWebSocketServer {
    private static final Set<Session> SESSIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @OnOpen
    public void onOpen(Session session) throws IOException {
        if (!SecurityUtil.hasRole(EmployeeEndpointConfigurator.getEmployee(session), EmployeeRole.ADMIN)) {
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
        KitchenWebSocketServer.broadcastToSessions(SESSIONS, payload);
    }
}
