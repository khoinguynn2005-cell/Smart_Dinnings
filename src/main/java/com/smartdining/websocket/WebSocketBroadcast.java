package com.smartdining.websocket;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.utils.BugLog;

import jakarta.websocket.Session;

/**
 * Gui tin cho nhieu phien WebSocket. Tomcat chi cho phep MOT lan gui tai mot thoi diem tren moi phien,
 * nen moi phien duoc gui tuan tu (synchronized + basic remote). Loi gui tren mot phien khong duoc
 * lam mat phien khac, va chi dong phien bi loi that su.
 */
final class WebSocketBroadcast {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WebSocketBroadcast() {
    }

    static void send(Collection<Session> sessions, Object payload) {
        final String message;
        try {
            message = MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            BugLog.swallowed("WebSocket: khong tao duoc noi dung tin", ex);
            return;
        }
        for (Session session : sessions) {
            send(session, message);
        }
    }

    static void send(Session session, String message) {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException | IllegalStateException ex) {
                BugLog.swallowed("WebSocket " + session.getId() + " gui that bai", ex);
                closeQuietly(session);
            }
        }
    }

    private static void closeQuietly(Session session) {
        try {
            session.close();
        } catch (IOException | IllegalStateException ignored) {
            // Phien da dong; onClose se go no khoi danh sach.
        }
    }
}
