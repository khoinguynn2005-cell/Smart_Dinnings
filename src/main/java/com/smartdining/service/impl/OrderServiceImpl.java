package com.smartdining.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.Product;
import com.smartdining.entity.TableShop;
import com.smartdining.entity.TableRequest;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.ProductStatus;
import com.smartdining.enums.TableRequestStatus;
import com.smartdining.enums.TableRequestType;
import com.smartdining.enums.TableStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.service.OrderService;
import com.smartdining.utils.BugLog;
import com.smartdining.utils.JpaUtil;
import com.smartdining.websocket.CustomerWebSocketServer;
import com.smartdining.websocket.KitchenWebSocketServer;
import com.smartdining.websocket.StaffWebSocketServer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class OrderServiceImpl implements OrderService {
    /** Chi de ghi log BUG-2: cac mon da tung duoc gui bep (trong lan chay Tomcat nay). */
    private static final Set<String> CONFIRMED_DETAIL_IDS = ConcurrentHashMap.newKeySet();

    @Override
    public boolean create(Order order) {
        if (order == null || order.getTable() == null) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableShop table = em.find(TableShop.class, order.getTable().getId());
            if (table == null || table.getStatus() == TableStatus.INACTIVE) {
                transaction.rollback();
                return false;
            }
            prepareNewOrder(order, table);
            table.setStatus(TableStatus.FULL);
            em.persist(order);
            transaction.commit();
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public Order getActiveOrderByTable(Integer tableId) {
        if (tableId == null) {
            return null;
        }

        EntityManager em = JpaUtil.getEntityManager();
        try {
            return findActiveOrder(em, tableId);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean addItemToCart(Integer tableId, String productId, int quantity, String note) {
        if (tableId == null || productId == null || productId.isBlank() || quantity <= 0 || quantity > 100) {
            return false;
        }

        String normalizedNote = note == null ? "" : note.trim();
        if (normalizedNote.length() > 255) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Order order = findActiveOrder(em, tableId);
            boolean tableBecameOccupied = false;
            boolean createdOrder = order == null;
            if (order == null) {
                TableShop table = em.find(TableShop.class, tableId);
                if (table == null || table.getStatus() == TableStatus.INACTIVE) {
                    rollback(transaction);
                    return false;
                }
                order = new Order();
                prepareNewOrder(order, table);
                tableBecameOccupied = table.getStatus() == TableStatus.AVAILABLE;
                table.setStatus(TableStatus.FULL);
                em.persist(order);
            }

            Product product = em.find(Product.class, productId);
            if (product == null || product.getStatus() != ProductStatus.AVAILABLE || product.getPrice() == null
                    || product.getPrice().signum() < 0) {
                rollback(transaction);
                return false;
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrderDetailId(nextId("CT"));
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(product.getPrice());
            detail.setNote(normalizedNote);
            detail.setCreatedTime(LocalDateTime.now());
            detail.setStatus(OrderDetailStatus.CART);
            em.persist(detail);
            transaction.commit();

            if (createdOrder) {
                long openOrders = countOpenOrders(tableId);
                if (openOrders > 1) {
                    BugLog.warn(1, "Ban " + tableId + " co " + openOrders
                            + " hoa don dang mo cung luc (nhieu khach them mon cung luc). Mon o cac hoa don cu se bi ket.");
                }
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", WebSocketEvent.CART_UPDATED.name());
            CustomerWebSocketServer.broadcastToTable(tableId, payload);
            if (tableBecameOccupied) {
                Map<String, Object> staffPayload = new HashMap<>();
                staffPayload.put("status", WebSocketEvent.TABLE_STATUS_UPDATED.name());
                staffPayload.put("tableId", tableId);
                staffPayload.put("tableStatus", TableStatus.FULL.name());
                StaffWebSocketServer.broadcast(staffPayload);
            }
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            BugLog.swallowed("Them mon vao gio (ban " + tableId + ")", ex);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean removeCartItem(Integer tableId, String orderDetailId) {
        if (tableId == null || orderDetailId == null || orderDetailId.isBlank()) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            OrderDetail detail = em.find(OrderDetail.class, orderDetailId);
            if (detail == null || detail.getStatus() != OrderDetailStatus.CART || detail.getOrder() == null
                    || detail.getOrder().getTable() == null
                    || !tableId.equals(detail.getOrder().getTable().getId())) {
                rollback(transaction);
                return false;
            }

            Order order = detail.getOrder();
            TableShop table = order.getTable();
            em.remove(detail);
            em.flush();

            Long remainingItemCount = em.createQuery(
                    "SELECT COUNT(od) FROM OrderDetail od WHERE od.order.id = :orderId", Long.class)
                    .setParameter("orderId", order.getId())
                    .getSingleResult();
            boolean tableBecameAvailable = remainingItemCount == 0;
            if (tableBecameAvailable) {
                em.remove(order);
                if (table.getStatus() != TableStatus.INACTIVE) {
                    table.setStatus(TableStatus.AVAILABLE);
                }
            }
            transaction.commit();

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", WebSocketEvent.CART_UPDATED.name());
            CustomerWebSocketServer.broadcastToTable(tableId, payload);
            if (tableBecameAvailable) {
                Map<String, Object> staffPayload = new HashMap<>();
                staffPayload.put("status", WebSocketEvent.TABLE_STATUS_UPDATED.name());
                staffPayload.put("tableId", tableId);
                staffPayload.put("tableStatus", TableStatus.AVAILABLE.name());
                StaffWebSocketServer.broadcast(staffPayload);
            }
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean updateCartItemQuantity(Integer tableId, String orderDetailId, int quantity) {
        if (tableId == null || orderDetailId == null || orderDetailId.isBlank() || quantity <= 0 || quantity > 100) {
            return false;
        }

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            OrderDetail detail = em.find(OrderDetail.class, orderDetailId);
            // Chi sua duoc mon chua gui bep va thuoc dung ban dang goi
            if (detail == null || detail.getStatus() != OrderDetailStatus.CART || detail.getOrder() == null
                    || detail.getOrder().getTable() == null
                    || !tableId.equals(detail.getOrder().getTable().getId())) {
                rollback(transaction);
                return false;
            }
            detail.setQuantity(quantity);
            transaction.commit();

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", WebSocketEvent.CART_UPDATED.name());
            CustomerWebSocketServer.broadcastToTable(tableId, payload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean confirmOrder(Integer tableId) {
        if (tableId == null) {
            return false;
        }

        List<Map<String, Object>> kitchenItems = new ArrayList<>();
        boolean paymentRequestUpdated = false;
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Order order = findActiveOrder(em, tableId);
            if (order == null) {
                rollback(transaction);
                return false;
            }

            List<OrderDetail> cartItems = em.createQuery(
                    "SELECT od FROM OrderDetail od JOIN FETCH od.product WHERE od.order.id = :orderId AND od.status = :status",
                    OrderDetail.class)
                    .setParameter("orderId", order.getId())
                    .setParameter("status", OrderDetailStatus.CART)
                    .getResultList();
            if (cartItems.isEmpty()) {
                rollback(transaction);
                return false;
            }

            BigDecimal totalAddition = BigDecimal.ZERO;
            for (OrderDetail detail : cartItems) {
                detail.setStatus(OrderDetailStatus.PENDING);
                totalAddition = totalAddition.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));

                Map<String, Object> item = new HashMap<>();
                item.put("id", detail.getOrderDetailId());
                item.put("productName", detail.getProduct().getName());
                item.put("quantity", detail.getQuantity());
                item.put("note", detail.getNote());
                item.put("status", detail.getStatus().name());
                item.put("tableId", tableId);
                item.put("groupKey", "table-" + tableId);
                item.put("groupName", order.getTable().getTableName());
                item.put("orderType", "DINE_IN");
                item.put("createdTime", detail.getCreatedTime().toString());
                kitchenItems.add(item);
            }
            order.setTotalAmount(nonNullAmount(order.getTotalAmount()).add(totalAddition));
            paymentRequestUpdated = em.createQuery(
                    "UPDATE TableRequest tr SET tr.totalAmount = :totalAmount "
                            + "WHERE tr.table.id = :tableId AND tr.requestType = :requestType "
                            + "AND tr.status = :status")
                    .setParameter("totalAmount", order.getTotalAmount())
                    .setParameter("tableId", tableId)
                    .setParameter("requestType", TableRequestType.PAYMENT)
                    .setParameter("status", TableRequestStatus.PENDING)
                    .executeUpdate() > 0;
            transaction.commit();

            int duplicated = 0;
            for (Map<String, Object> item : kitchenItems) {
                if (!CONFIRMED_DETAIL_IDS.add((String) item.get("id"))) {
                    duplicated++;
                }
            }
            if (duplicated > 0) {
                BugLog.warn(2, "Ban " + tableId + ": " + duplicated + " mon da gui bep ROI van duoc gui them lan nua"
                        + " (bam Gui bep cung luc). Tong tien hoa don " + order.getId() + " bi cong trung, hien la "
                        + order.getTotalAmount());
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", WebSocketEvent.NEW_ORDER.name());
            payload.put("data", kitchenItems);
            KitchenWebSocketServer.broadcast(payload);

            Map<String, Object> staffOrderPayload = new HashMap<>();
            staffOrderPayload.put("status", WebSocketEvent.ORDER_CONFIRMED.name());
            staffOrderPayload.put("tableId", tableId);
            staffOrderPayload.put("tableName", order.getTable().getTableName());
            staffOrderPayload.put("itemCount", kitchenItems.size());
            staffOrderPayload.put("totalAmount", order.getTotalAmount());
            StaffWebSocketServer.broadcast(staffOrderPayload);

            if (paymentRequestUpdated) {
                Map<String, Object> staffPayload = new HashMap<>();
                staffPayload.put("status", WebSocketEvent.PAYMENT_REQUEST.name());
                staffPayload.put("tableId", tableId);
                staffPayload.put("tableName", order.getTable().getTableName());
                staffPayload.put("totalAmount", order.getTotalAmount());
                StaffWebSocketServer.broadcast(staffPayload);
            }
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public List<OrderDetail> getOrderDetailsByTable(Integer tableId) {
        if (tableId == null) {
            return List.of();
        }

        EntityManager em = JpaUtil.getEntityManager();
        try {
            Order order = findActiveOrder(em, tableId);
            if (order == null) {
                return List.of();
            }
            return em.createQuery(
                    "SELECT od FROM OrderDetail od JOIN FETCH od.product WHERE od.order.id = :orderId ORDER BY od.createdTime ASC",
                    OrderDetail.class)
                    .setParameter("orderId", order.getId())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean requestPayment(Integer tableId) {
        if (tableId == null) {
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        boolean requestCreated = false;
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Order order = findActiveOrder(em, tableId);
            if (order == null || order.getTable() == null) {
                rollback(transaction);
                return false;
            }
            order.getTable().setStatus(TableStatus.WAITING_PAYMENT);
            if (!hasPendingRequest(em, tableId, TableRequestType.PAYMENT)) {
                TableRequest tableRequest = createTableRequest(order.getTable(), TableRequestType.PAYMENT,
                        null, null, nonNullAmount(order.getTotalAmount()));
                em.persist(tableRequest);
                requestCreated = true;
                payload.put("requestId", tableRequest.getId());
            }
            transaction.commit();

            if (requestCreated) {
                payload.put("status", WebSocketEvent.PAYMENT_REQUEST.name());
                payload.put("tableId", tableId);
                payload.put("tableName", order.getTable().getTableName());
                payload.put("totalAmount", nonNullAmount(order.getTotalAmount()));
                StaffWebSocketServer.broadcast(payload);
            }
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean requestService(Integer tableId, String serviceType, String note) {
        if (tableId == null || serviceType == null) {
            return false;
        }

        String normalizedType = serviceType.trim();
        String normalizedNote = note == null ? "" : note.trim();
        if (normalizedType.isEmpty() || normalizedType.length() > 80 || normalizedNote.length() > 255) {
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableShop table = em.find(TableShop.class, tableId);
            if (table == null || table.getStatus() == TableStatus.AVAILABLE) {
                rollback(transaction);
                return false;
            }

            TableRequest tableRequest = createTableRequest(table, TableRequestType.SERVICE,
                    normalizedType, normalizedNote, null);
            em.persist(tableRequest);
            transaction.commit();

            payload.put("status", WebSocketEvent.SERVICE_REQUEST.name());
            payload.put("requestId", tableRequest.getId());
            payload.put("tableId", tableId);
            payload.put("tableName", table.getTableName());
            payload.put("type", normalizedType);
            payload.put("note", normalizedNote);
            StaffWebSocketServer.broadcast(payload);
            return true;
        } catch (Exception ex) {
            rollback(transaction);
            return false;
        } finally {
            em.close();
        }
    }

    /** Chi de ghi log BUG-1: dem hoa don UNPAID cua ban sau khi giao dich da commit. */
    private long countOpenOrders(Integer tableId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Order o WHERE o.table.id = :tableId AND o.status = :status",
                    Long.class)
                    .setParameter("tableId", tableId)
                    .setParameter("status", OrderStatus.UNPAID)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private Order findActiveOrder(EntityManager em, Integer tableId) {
        List<Order> orders = em.createQuery(
                "SELECT o FROM Order o JOIN FETCH o.table WHERE o.table.id = :tableId AND o.status = :status ORDER BY o.startTime DESC",
                Order.class)
                .setParameter("tableId", tableId)
                .setParameter("status", OrderStatus.UNPAID)
                .setMaxResults(1)
                .getResultList();
        return orders.isEmpty() ? null : orders.get(0);
    }

    private void prepareNewOrder(Order order, TableShop table) {
        order.setId(nextId("HD"));
        order.setTable(table);
        order.setStatus(OrderStatus.UNPAID);
        order.setStartTime(LocalDateTime.now());
        order.setTotalAmount(nonNullAmount(order.getTotalAmount()));
    }

    private BigDecimal nonNullAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private boolean hasPendingRequest(EntityManager em, Integer tableId, TableRequestType requestType) {
        Long count = em.createQuery(
                "SELECT COUNT(tr) FROM TableRequest tr WHERE tr.table.id = :tableId "
                        + "AND tr.requestType = :requestType AND tr.status = :status",
                Long.class)
                .setParameter("tableId", tableId)
                .setParameter("requestType", requestType)
                .setParameter("status", TableRequestStatus.PENDING)
                .getSingleResult();
        return count > 0;
    }

    private TableRequest createTableRequest(TableShop table, TableRequestType requestType,
            String serviceType, String note, BigDecimal totalAmount) {
        TableRequest tableRequest = new TableRequest();
        tableRequest.setId(nextId("REQ"));
        tableRequest.setTable(table);
        tableRequest.setRequestType(requestType);
        tableRequest.setServiceType(serviceType);
        tableRequest.setNote(note);
        tableRequest.setTotalAmount(totalAmount);
        tableRequest.setRequestedTime(LocalDateTime.now());
        tableRequest.setStatus(TableRequestStatus.PENDING);
        return tableRequest;
    }

    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }
}
