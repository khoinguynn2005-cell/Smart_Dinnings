package com.smartdining.service;

import java.util.List;

import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.TableShop;
import com.smartdining.entity.TableRequest;

public interface StaffService {
    List<OrderDetail> getReadyTasks();

    List<Order> getOpenOrders();

    List<TableShop> getAllTables();

    Order getOpenOrderByTable(Integer tableId);

    boolean markServed(String orderDetailId, String employeeId);

    List<TableRequest> getPendingRequests();

    boolean completeRequest(String requestId, String employeeId);

    /** Hoa don cua ban dang mo (null neu khong co). */
    Bill getBill(Integer tableId);

    /** Chot thanh toan: chi khi khach da yeu cau va moi mon da phuc vu; chuyen khoan bat buoc anh bill. */
    CheckoutResult checkout(Integer tableId, String employeeId, String paymentMethod, String image);

    record BillItem(String id, String productName, int quantity, java.math.BigDecimal unitPrice,
            java.math.BigDecimal lineTotal, String status) {
    }

    record Bill(Integer tableId, String tableName, String orderId, java.time.LocalDateTime startTime,
            List<BillItem> items, java.math.BigDecimal total, boolean paymentRequested, int unservedCount) {
    }

    enum CheckoutResult { OK, NO_OPEN_ORDER, NOT_REQUESTED, ITEMS_NOT_SERVED, INVALID_METHOD, IMAGE_REQUIRED, INVALID_IMAGE, ERROR }
}
