package com.smartdining.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartdining.dao.OrderDetailDAO;
import com.smartdining.dao.impl.OrderDetailDAOImpl;
import com.smartdining.entity.Employee;
import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.service.KitchenService;
import com.smartdining.utils.JpaUtil;
import com.smartdining.websocket.CustomerWebSocketServer;
import com.smartdining.websocket.KitchenWebSocketServer;
import com.smartdining.websocket.StaffWebSocketServer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class KitchenServiceImpl implements KitchenService {
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAOImpl();

    @Override
    public List<OrderDetail> getActiveTasks() {
        return orderDetailDAO.getActiveKitchenTasks();
    }

    @Override
    public boolean updateTaskStatus(String orderDetailId, OrderDetailStatus newStatus, String employeeId) {
        if (orderDetailId == null || newStatus == null) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            OrderDetail detail = em.find(OrderDetail.class, orderDetailId);
            if (detail == null || !isKitchenTransition(detail.getStatus(), newStatus) || detail.getOrder() == null) {
                rollback(transaction);
                return false;
            }

            // Ghi lai ai nau va cac moc thoi gian (bao cao hoat dong nhan vien)
            Employee cook = employeeId == null ? null : em.find(Employee.class, employeeId);
            LocalDateTime now = LocalDateTime.now();
            if (newStatus == OrderDetailStatus.PREPARING) {
                detail.setStartedTime(now);
                detail.setPreparedBy(cook);
            } else if (newStatus == OrderDetailStatus.READY) {
                detail.setReadyTime(now);
                if (detail.getPreparedBy() == null) {
                    detail.setPreparedBy(cook);
                }
            }
            detail.setStatus(newStatus);
            Order order = detail.getOrder();
            Integer tableId = order.getTable() == null ? null : order.getTable().getId();
            String label = order.getDisplayName();
            transaction.commit();

            if (tableId != null) {
                Map<String, Object> customerPayload = new HashMap<>();
                customerPayload.put("status", WebSocketEvent.STATUS_UPDATED.name());
                customerPayload.put("orderDetailId", orderDetailId);
                customerPayload.put("newStatus", newStatus.name());
                CustomerWebSocketServer.broadcastToTable(tableId, customerPayload);
            }

            Map<String, Object> kitchenPayload = new HashMap<>();
            kitchenPayload.put("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name());
            KitchenWebSocketServer.broadcast(kitchenPayload);

            Map<String, Object> staffPayload = new HashMap<>();
            staffPayload.put("orderDetailId", orderDetailId);
            staffPayload.put("tableId", tableId);
            staffPayload.put("orderId", order.getId());
            staffPayload.put("orderType", order.getOrderType().name());
            if (newStatus == OrderDetailStatus.READY) {
                staffPayload.put("status", WebSocketEvent.ITEM_READY.name());
                staffPayload.put("tableName", label);
                staffPayload.put("productName", detail.getProduct().getName());
                staffPayload.put("quantity", detail.getQuantity());
                staffPayload.put("note", detail.getNote());
            } else {
                staffPayload.put("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name());
            }
            StaffWebSocketServer.broadcast(staffPayload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean cancelTask(String orderDetailId, String employeeId) {
        if (orderDetailId == null) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            OrderDetail detail = em.find(OrderDetail.class, orderDetailId);
            if (detail == null || (detail.getStatus() != OrderDetailStatus.PENDING
                    && detail.getStatus() != OrderDetailStatus.PREPARING) || detail.getOrder() == null) {
                rollback(transaction);
                return false;
            }

            detail.setStatus(OrderDetailStatus.CANCELLED);
            // Nguoi bao "het mon" duoc tinh vao bao cao cua bep
            if (employeeId != null) {
                detail.setPreparedBy(em.find(Employee.class, employeeId));
            }
            Order order = detail.getOrder();
            BigDecimal refund = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            BigDecimal updatedTotal = nonNullAmount(order.getTotalAmount()).subtract(refund);
            order.setTotalAmount(updatedTotal.max(BigDecimal.ZERO));
            Integer tableId = order.getTable() == null ? null : order.getTable().getId();
            transaction.commit();

            if (tableId != null) {
                Map<String, Object> customerPayload = new HashMap<>();
                customerPayload.put("status", WebSocketEvent.ITEM_CANCELLED.name());
                customerPayload.put("orderDetailId", orderDetailId);
                CustomerWebSocketServer.broadcastToTable(tableId, customerPayload);
            }

            Map<String, Object> kitchenPayload = new HashMap<>();
            kitchenPayload.put("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name());
            KitchenWebSocketServer.broadcast(kitchenPayload);

            Map<String, Object> staffPayload = new HashMap<>();
            staffPayload.put("status", WebSocketEvent.ITEM_CANCELLED.name());
            staffPayload.put("orderDetailId", orderDetailId);
            staffPayload.put("tableId", tableId);
            staffPayload.put("orderId", order.getId());
            staffPayload.put("orderType", order.getOrderType().name());
            // Don mang ve da thu tien truoc: nhan vien can hoan tien mon bi huy cho khach
            staffPayload.put("refundAmount", order.isTakeaway() ? refund : null);
            StaffWebSocketServer.broadcast(staffPayload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    private boolean isKitchenTransition(OrderDetailStatus currentStatus, OrderDetailStatus newStatus) {
        return (currentStatus == OrderDetailStatus.PENDING && newStatus == OrderDetailStatus.PREPARING)
                || (currentStatus == OrderDetailStatus.PREPARING && newStatus == OrderDetailStatus.READY);
    }

    private BigDecimal nonNullAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }
}
