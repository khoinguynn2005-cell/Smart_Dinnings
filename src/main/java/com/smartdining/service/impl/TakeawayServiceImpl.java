package com.smartdining.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.smartdining.entity.Employee;
import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.Payment;
import com.smartdining.entity.Product;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.OrderType;
import com.smartdining.enums.PaymentMethod;
import com.smartdining.enums.PaymentStatus;
import com.smartdining.enums.ProductStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.service.TakeawayService;
import com.smartdining.utils.JpaUtil;
import com.smartdining.utils.PaymentProofUtil;
import com.smartdining.websocket.AdminWebSocketServer;
import com.smartdining.websocket.KitchenWebSocketServer;
import com.smartdining.websocket.StaffWebSocketServer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class TakeawayServiceImpl implements TakeawayService {
    private static final int MAX_LINES = 50;

    @Override
    public List<Product> getMenu() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = :status "
                    + "ORDER BY p.category.name ASC, p.name ASC", Product.class)
                    .setParameter("status", ProductStatus.AVAILABLE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public CreateResult create(TakeawayInput input, String employeeId) {
        if (input == null || input.items() == null || input.items().isEmpty()) {
            return CreateResult.error(Status.EMPTY);
        }
        if (input.items().size() > MAX_LINES) {
            return CreateResult.error(Status.INVALID_ITEM);
        }
        String customerName = trimToNull(input.customerName());
        String customerPhone = trimToNull(input.customerPhone());
        if ((customerName != null && customerName.length() > 100)
                || (customerPhone != null && !customerPhone.matches("^[0-9+ .-]{8,15}$"))) {
            return CreateResult.error(Status.INVALID_CUSTOMER);
        }
        LocalDateTime pickupTime;
        try {
            pickupTime = parsePickupTime(input.pickupTime());
        } catch (DateTimeParseException ex) {
            return CreateResult.error(Status.INVALID_PICKUP_TIME);
        }
        PaymentMethod method = PaymentMethod.parse(input.paymentMethod());
        if (method == null) {
            return CreateResult.error(Status.INVALID_METHOD);
        }
        PaymentProofUtil.Problem proofProblem = PaymentProofUtil.validate(method, input.image());
        if (proofProblem != null) {
            return CreateResult.error(proofProblem == PaymentProofUtil.Problem.IMAGE_REQUIRED
                    ? Status.IMAGE_REQUIRED : Status.INVALID_IMAGE);
        }

        List<Map<String, Object>> kitchenItems = new ArrayList<>();
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Employee employee = employeeId == null ? null : em.find(Employee.class, employeeId);
            if (employee == null) {
                rollback(transaction);
                return CreateResult.error(Status.ERROR);
            }
            LocalDateTime now = LocalDateTime.now();
            Order order = new Order();
            order.setId("HD-" + UUID.randomUUID());
            order.setOrderType(OrderType.TAKEAWAY);
            order.setTable(null);
            order.setEmployee(employee);
            order.setStartTime(now);
            order.setStatus(OrderStatus.PAID);
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);
            order.setPickupTime(pickupTime);
            order.setPickupCode(nextPickupCode(em));

            List<OrderDetail> details = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (TakeawayItem item : input.items()) {
                String note = item.note() == null ? "" : item.note().trim();
                Product product = item.productId() == null ? null : em.find(Product.class, item.productId());
                if (product == null || product.getStatus() != ProductStatus.AVAILABLE || product.getPrice() == null
                        || item.quantity() <= 0 || item.quantity() > 100 || note.length() > 255) {
                    rollback(transaction);
                    return CreateResult.error(Status.INVALID_ITEM);
                }
                OrderDetail detail = new OrderDetail();
                detail.setOrderDetailId("CT-" + UUID.randomUUID());
                detail.setOrder(order);
                detail.setProduct(product);
                detail.setQuantity(item.quantity());
                detail.setUnitPrice(product.getPrice());
                detail.setNote(note);
                detail.setCreatedTime(now);
                // Da thanh toan nen gui thang xuong bep
                detail.setStatus(OrderDetailStatus.PENDING);
                details.add(detail);
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
            }
            order.setTotalAmount(total);
            em.persist(order);
            for (OrderDetail detail : details) {
                em.persist(detail);
            }

            Payment payment = new Payment();
            payment.setPaymentId("PAY-" + UUID.randomUUID());
            payment.setOrder(order);
            payment.setPaymentMethod(method.name());
            payment.setAmount(total);
            payment.setPayTime(now);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setImage(method == PaymentMethod.TRANSFER ? input.image() : null);
            em.persist(payment);
            transaction.commit();

            for (OrderDetail detail : details) {
                Map<String, Object> kitchenItem = new HashMap<>();
                kitchenItem.put("id", detail.getOrderDetailId());
                kitchenItem.put("productName", detail.getProduct().getName());
                kitchenItem.put("quantity", detail.getQuantity());
                kitchenItem.put("note", detail.getNote());
                kitchenItem.put("status", detail.getStatus().name());
                kitchenItem.put("tableId", null);
                kitchenItem.put("groupKey", "order-" + order.getId());
                kitchenItem.put("groupName", order.getDisplayName());
                kitchenItem.put("orderType", OrderType.TAKEAWAY.name());
                kitchenItem.put("pickupTime", pickupTime == null ? null : pickupTime.toString());
                kitchenItem.put("createdTime", now.toString());
                kitchenItems.add(kitchenItem);
            }
            Map<String, Object> kitchenPayload = new HashMap<>();
            kitchenPayload.put("status", WebSocketEvent.NEW_ORDER.name());
            kitchenPayload.put("data", kitchenItems);
            KitchenWebSocketServer.broadcast(kitchenPayload);
            AdminWebSocketServer.broadcast(Map.of("status", WebSocketEvent.ORDER_PAID.name(), "orderId", order.getId()));
            StaffWebSocketServer.broadcast(Map.of("status", WebSocketEvent.TAKEAWAY_UPDATED.name(), "orderId", order.getId()));
            return new CreateResult(Status.OK, order.getId(), order.getPickupCode(), total);
        } catch (Exception ex) {
            rollback(transaction);
            return CreateResult.error(Status.ERROR);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Order> getTodayOrders() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            return em.createQuery("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product "
                    + "LEFT JOIN FETCH o.employee WHERE o.orderType = :takeaway AND o.startTime >= :start "
                    + "ORDER BY o.startTime DESC", Order.class)
                    .setParameter("takeaway", OrderType.TAKEAWAY)
                    .setParameter("start", start)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public DeliverResult deliver(String orderId, String employeeId) {
        if (orderId == null || orderId.isBlank()) {
            return DeliverResult.NOT_FOUND;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Order order = em.find(Order.class, orderId);
            if (order == null || order.getOrderType() != OrderType.TAKEAWAY) {
                rollback(transaction);
                return DeliverResult.NOT_FOUND;
            }
            List<OrderDetail> details = em.createQuery(
                    "SELECT od FROM OrderDetail od WHERE od.order.id = :orderId", OrderDetail.class)
                    .setParameter("orderId", orderId)
                    .getResultList();
            boolean anyReady = false;
            for (OrderDetail detail : details) {
                if (detail.getStatus() == OrderDetailStatus.PENDING || detail.getStatus() == OrderDetailStatus.PREPARING) {
                    rollback(transaction);
                    return DeliverResult.NOT_READY;
                }
                anyReady |= detail.getStatus() == OrderDetailStatus.READY;
            }
            if (!anyReady) {
                rollback(transaction);
                return DeliverResult.ALREADY_DELIVERED;
            }
            Employee employee = employeeId == null ? null : em.find(Employee.class, employeeId);
            LocalDateTime now = LocalDateTime.now();
            for (OrderDetail detail : details) {
                if (detail.getStatus() == OrderDetailStatus.READY) {
                    detail.setStatus(OrderDetailStatus.SERVED);
                    detail.setServedBy(employee);
                    detail.setServedTime(now);
                }
            }
            transaction.commit();
            StaffWebSocketServer.broadcast(Map.of("status", WebSocketEvent.TAKEAWAY_UPDATED.name(), "orderId", orderId));
            KitchenWebSocketServer.broadcast(Map.of("status", WebSocketEvent.KITCHEN_TASK_UPDATED.name()));
            return DeliverResult.OK;
        } catch (Exception ex) {
            rollback(transaction);
            return DeliverResult.ERROR;
        } finally {
            em.close();
        }
    }

    /** Ma lay do tang dan trong ngay, bat dau lai tu 1 moi ngay. */
    private Integer nextPickupCode(EntityManager em) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Integer max = em.createQuery("SELECT MAX(o.pickupCode) FROM Order o WHERE o.orderType = :takeaway "
                + "AND o.startTime >= :start", Integer.class)
                .setParameter("takeaway", OrderType.TAKEAWAY)
                .setParameter("start", start)
                .getSingleResult();
        return max == null ? 1 : max + 1;
    }

    /** "HH:mm" -> hom nay (hoac ngay mai neu gio da qua); rong = lay ngay. */
    private LocalDateTime parsePickupTime(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        LocalTime time = LocalTime.parse(text);
        LocalDateTime pickup = LocalDate.now().atTime(time);
        return pickup.isBefore(LocalDateTime.now().minusMinutes(5)) ? pickup.plusDays(1) : pickup;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }
}
