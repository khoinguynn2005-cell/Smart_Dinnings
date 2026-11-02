package com.smartdining.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.Order;
import com.smartdining.entity.Employee;
import com.smartdining.entity.TableShop;
import com.smartdining.entity.TableRequest;
import com.smartdining.entity.Payment;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.PaymentMethod;
import com.smartdining.enums.PaymentStatus;
import com.smartdining.enums.TableRequestStatus;
import com.smartdining.enums.TableRequestType;
import com.smartdining.enums.TableStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.service.StaffService;
import com.smartdining.utils.BugLog;
import com.smartdining.utils.JpaUtil;
import com.smartdining.utils.PaymentProofUtil;
import com.smartdining.websocket.AdminWebSocketServer;
import com.smartdining.websocket.CustomerWebSocketServer;
import com.smartdining.websocket.KitchenWebSocketServer;
import com.smartdining.websocket.StaffWebSocketServer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

public class StaffServiceImpl implements StaffService {
    @Override
    public List<Order> getOpenOrders() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT o FROM Order o JOIN FETCH o.table "
                            + "LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product "
                            + "WHERE o.status = :status ORDER BY o.startTime ASC",
                    Order.class)
                    .setParameter("status", OrderStatus.UNPAID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<TableShop> getAllTables() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM TableShop t WHERE t.status <> :inactive ORDER BY t.id ASC", TableShop.class)
                    .setParameter("inactive", TableStatus.INACTIVE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public Order getOpenOrderByTable(Integer tableId) {
        if (tableId == null) {
            return null;
        }

        EntityManager em = JpaUtil.getEntityManager();
        try {
            List<Order> orders = em.createQuery(
                    "SELECT DISTINCT o FROM Order o JOIN FETCH o.table "
                            + "LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product "
                            + "WHERE o.table.id = :tableId AND o.status = :status ORDER BY o.startTime DESC",
                    Order.class)
                    .setParameter("tableId", tableId)
                    .setParameter("status", OrderStatus.UNPAID)
                    .getResultList();
            return orders.isEmpty() ? null : orders.get(0);
        } finally {
            em.close();
        }
    }

    @Override
    public List<OrderDetail> getReadyTasks() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT od FROM OrderDetail od JOIN FETCH od.product JOIN FETCH od.order o JOIN FETCH o.table "
                            + "WHERE od.status = :status AND o.status = :orderStatus ORDER BY od.createdTime ASC",
                    OrderDetail.class)
                    .setParameter("status", OrderDetailStatus.READY)
                    .setParameter("orderStatus", OrderStatus.UNPAID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean markServed(String orderDetailId, String employeeId) {
        if (orderDetailId == null) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            OrderDetail detail = em.find(OrderDetail.class, orderDetailId);
            if (detail == null || detail.getStatus() != OrderDetailStatus.READY
                    || detail.getOrder() == null || detail.getOrder().getTable() == null) {
                rollback(transaction);
                return false;
            }
            detail.setStatus(OrderDetailStatus.SERVED);
            // Ghi lai ai mang mon ra va luc nao (bao cao hoat dong nhan vien)
            detail.setServedTime(LocalDateTime.now());
            detail.setServedBy(employeeId == null ? null : em.find(Employee.class, employeeId));
            Integer tableId = detail.getOrder().getTable().getId();
            transaction.commit();

            Map<String, Object> customerPayload = new HashMap<>();
            customerPayload.put("status", WebSocketEvent.STATUS_UPDATED.name());
            customerPayload.put("orderDetailId", orderDetailId);
            customerPayload.put("newStatus", OrderDetailStatus.SERVED.name());
            CustomerWebSocketServer.broadcastToTable(tableId, customerPayload);

            Map<String, Object> staffPayload = new HashMap<>();
            staffPayload.put("status", WebSocketEvent.ITEM_SERVED.name());
            staffPayload.put("orderDetailId", orderDetailId);
            staffPayload.put("tableId", tableId);
            StaffWebSocketServer.broadcast(staffPayload);

            Map<String, Object> kitchenPayload = new HashMap<>();
            kitchenPayload.put("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name());
            kitchenPayload.put("orderDetailId", orderDetailId);
            kitchenPayload.put("tableId", tableId);
            KitchenWebSocketServer.broadcast(kitchenPayload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public List<TableRequest> getPendingRequests() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT tr FROM TableRequest tr JOIN FETCH tr.table "
                            + "WHERE tr.status = :status ORDER BY tr.requestedTime ASC",
                    TableRequest.class)
                    .setParameter("status", TableRequestStatus.PENDING)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean completeRequest(String requestId, String employeeId) {
        if (requestId == null || requestId.isBlank() || employeeId == null || employeeId.isBlank()) {
            return false;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableRequest tableRequest = em.find(TableRequest.class, requestId);
            Employee employee = em.find(Employee.class, employeeId);
            // Yeu cau thanh toan phai di qua checkout() (co hoa don, phuong thuc va anh bill)
            if (tableRequest == null || employee == null || tableRequest.getStatus() != TableRequestStatus.PENDING
                    || tableRequest.getRequestType() == TableRequestType.PAYMENT) {
                rollback(transaction);
                return false;
            }
            tableRequest.setStatus(TableRequestStatus.COMPLETED);
            tableRequest.setCompletedTime(LocalDateTime.now());
            tableRequest.setCompletedBy(employee);
            transaction.commit();

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", WebSocketEvent.TABLE_REQUEST_COMPLETED.name());
            payload.put("requestId", requestId);
            StaffWebSocketServer.broadcast(payload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public Bill getBill(Integer tableId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Order order = findOpenOrder(em, tableId);
            if (order == null) {
                return null;
            }
            return buildBill(em, order);
        } finally {
            em.close();
        }
    }

    @Override
    public CheckoutResult checkout(Integer tableId, String employeeId, String paymentMethod, String image) {
        PaymentMethod method = PaymentMethod.parse(paymentMethod);
        if (method == null) {
            return CheckoutResult.INVALID_METHOD;
        }
        // Chuyen khoan bat buoc co anh bill (quy tac chung voi don mang ve)
        PaymentProofUtil.Problem proofProblem = PaymentProofUtil.validate(method, image);
        if (proofProblem != null) {
            return proofProblem == PaymentProofUtil.Problem.IMAGE_REQUIRED
                    ? CheckoutResult.IMAGE_REQUIRED : CheckoutResult.INVALID_IMAGE;
        }
        String proofImage = method == PaymentMethod.TRANSFER ? image : null;

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            // Khoa dong ban: hai yeu cau thanh toan cung luc chi duoc ghi MOT Payment.
            TableShop lockedTable = em.find(TableShop.class, tableId, LockModeType.PESSIMISTIC_WRITE);
            if (lockedTable == null) {
                rollback(transaction);
                return CheckoutResult.NO_OPEN_ORDER;
            }
            Order order = findOpenOrder(em, tableId);
            Employee employee = employeeId == null ? null : em.find(Employee.class, employeeId);
            if (order == null || employee == null) {
                rollback(transaction);
                return CheckoutResult.NO_OPEN_ORDER;
            }
            Bill bill = buildBill(em, order);
            if (!bill.paymentRequested()) {
                rollback(transaction);
                return CheckoutResult.NOT_REQUESTED;
            }
            if (bill.unservedCount() > 0) {
                rollback(transaction);
                return CheckoutResult.ITEMS_NOT_SERVED;
            }

            LocalDateTime paidAt = LocalDateTime.now();
            // Mon con trong gio (chua gui bep) khong tinh tien -> xoa khoi hoa don da chot
            em.createQuery("DELETE FROM OrderDetail od WHERE od.order.id = :orderId AND od.status = :cart")
                    .setParameter("orderId", order.getId())
                    .setParameter("cart", OrderDetailStatus.CART)
                    .executeUpdate();
            order.setTotalAmount(bill.total());
            order.setEmployee(employee);
            order.setStatus(OrderStatus.PAID);
            order.getTable().setStatus(TableStatus.AVAILABLE);
            em.createQuery("UPDATE TableRequest tr SET tr.status = :completed, tr.completedTime = :completedTime, "
                    + "tr.completedBy = :employee WHERE tr.table.id = :tableId AND tr.status = :pending")
                    .setParameter("completed", TableRequestStatus.COMPLETED)
                    .setParameter("completedTime", paidAt)
                    .setParameter("employee", employee)
                    .setParameter("tableId", tableId)
                    .setParameter("pending", TableRequestStatus.PENDING)
                    .executeUpdate();

            Payment payment = new Payment();
            payment.setPaymentId("PAY-" + java.util.UUID.randomUUID());
            payment.setOrder(order);
            payment.setPaymentMethod(method.name());
            payment.setAmount(bill.total());
            payment.setPayTime(paidAt);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setImage(proofImage);
            em.persist(payment);
            transaction.commit();

            broadcastOrderPaid(tableId);
            return CheckoutResult.OK;
        } catch (Exception ex) {
            rollback(transaction);
            BugLog.swallowed("Thanh toan ban " + tableId, ex);
            return CheckoutResult.ERROR;
        } finally {
            em.close();
        }
    }

    /** Hoa don cua ban: chi tinh mon da gui bep va chua huy. */
    private Bill buildBill(EntityManager em, Order order) {
        List<OrderDetail> details = em.createQuery(
                "SELECT od FROM OrderDetail od JOIN FETCH od.product WHERE od.order.id = :orderId ORDER BY od.createdTime ASC",
                OrderDetail.class)
                .setParameter("orderId", order.getId())
                .getResultList();
        List<BillItem> items = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int unserved = 0;
        for (OrderDetail detail : details) {
            if (detail.getStatus() == OrderDetailStatus.CART || detail.getStatus() == OrderDetailStatus.CANCELLED) {
                continue;
            }
            if (detail.getStatus() != OrderDetailStatus.SERVED) {
                unserved += detail.getQuantity();
            }
            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(detail.getQuantity()));
            total = total.add(lineTotal);
            items.add(new BillItem(detail.getOrderDetailId(), detail.getProduct().getName(), detail.getQuantity(),
                    unitPrice, lineTotal, detail.getStatus().name()));
        }
        Long paymentRequests = em.createQuery(
                "SELECT COUNT(tr) FROM TableRequest tr WHERE tr.table.id = :tableId AND tr.requestType = :type "
                        + "AND tr.status = :pending", Long.class)
                .setParameter("tableId", order.getTable().getId())
                .setParameter("type", TableRequestType.PAYMENT)
                .setParameter("pending", TableRequestStatus.PENDING)
                .getSingleResult();
        return new Bill(order.getTable().getId(), order.getTable().getTableName(), order.getId(), order.getStartTime(),
                items, total, paymentRequests > 0, unserved);
    }

    private void broadcastOrderPaid(Integer tableId) {
        Map<String, Object> customerPayload = new HashMap<>();
        customerPayload.put("status", WebSocketEvent.ORDER_PAID.name());
        customerPayload.put("tableId", tableId);
        CustomerWebSocketServer.broadcastToTable(tableId, customerPayload);

        // Admin: cap nhat doanh thu / so hoa don ngay khi thanh toan xong.
        AdminWebSocketServer.broadcast(Map.of("status", WebSocketEvent.ORDER_PAID.name(), "tableId", tableId));

        Map<String, Object> kitchenPayload = new HashMap<>();
        kitchenPayload.put("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name());
        kitchenPayload.put("tableId", tableId);
        KitchenWebSocketServer.broadcast(kitchenPayload);

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("status", WebSocketEvent.TABLE_REQUEST_COMPLETED.name());
        requestPayload.put("tableId", tableId);
        StaffWebSocketServer.broadcast(requestPayload);

        Map<String, Object> staffPayload = new HashMap<>();
        staffPayload.put("status", WebSocketEvent.TABLE_STATUS_UPDATED.name());
        staffPayload.put("tableId", tableId);
        staffPayload.put("tableStatus", TableStatus.AVAILABLE.name());
        StaffWebSocketServer.broadcast(staffPayload);
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }

    private Order findOpenOrder(EntityManager em, Integer tableId) {
        if (tableId == null) {
            return null;
        }
        List<Order> orders = em.createQuery(
                "SELECT o FROM Order o JOIN FETCH o.table WHERE o.table.id = :tableId AND o.status = :status "
                        + "ORDER BY o.startTime DESC",
                Order.class)
                .setParameter("tableId", tableId)
                .setParameter("status", OrderStatus.UNPAID)
                .setMaxResults(1)
                .getResultList();
        return orders.isEmpty() ? null : orders.get(0);
    }
}
