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

@ServerEndpoint(value = "/ws/kitchen", configurator = EmployeeEndpointConfigurator.class)
public class KitchenWebSocketServer {
    private static final Set<Session> SESSIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        WebSocketBroadcast.send(sessions, payload);
    }
}
