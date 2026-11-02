package com.smartdining.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.TableStatus;
import com.smartdining.service.TableShopService;
import com.smartdining.service.impl.TableShopServiceImpl;
import com.smartdining.utils.BugLog;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Khach ket noi bang ma QR cua ban (qr=...), khong bang so ban: ma QR la chuoi ngau nhien,
 * nen khach chi nhan duoc su kien cua dung ban minh dang ngoi.
 */
@ServerEndpoint(value = "/ws/customer", configurator = EmployeeEndpointConfigurator.class)
public class CustomerWebSocketServer {
    private static final Map<Session, Integer> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TableShopService TABLE_SERVICE = new TableShopServiceImpl();

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
                if (tableId.equals(sessionTableId)) {
                    WebSocketBroadcast.send(session, jsonMessage);
                }
            });
        } catch (Exception ex) {
            BugLog.swallowed("WebSocket khach ban " + tableId, ex);
        }
    }

    public static void broadcast(Object payload) {
        WebSocketBroadcast.send(SESSIONS.keySet(), payload);
    }

    private Integer getTableIdFromRequest(Session session) {
        List<String> values = session.getRequestParameterMap().get("qr");
        if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
            return null;
        }
        TableShop table = TABLE_SERVICE.findByQrCodeUrl(values.get(0));
        if (table == null || table.getStatus() == TableStatus.INACTIVE) {
            return null;
        }
        return table.getId();
    }
}
