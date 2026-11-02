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

@ServerEndpoint(value = "/ws/staff", configurator = EmployeeEndpointConfigurator.class)
public class StaffWebSocketServer {
    private static final Set<Session> SESSIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @OnOpen
    public void onOpen(Session session) throws IOException {
        if (!SecurityUtil.hasRole(EmployeeEndpointConfigurator.getEmployee(session),
                EmployeeRole.ADMIN, EmployeeRole.STAFF)) {
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
        // Admin giam sat van hanh: nhan cung cac su kien cua nhan vien phuc vu
        // (don moi, yeu cau thanh toan/phuc vu, trang thai ban).
        AdminWebSocketServer.broadcast(payload);
    }
}
