package com.smartdining.service;

import java.math.BigDecimal;
import java.util.List;

import com.smartdining.entity.Order;
import com.smartdining.entity.Product;

/** Ban mang ve tai quay: thanh toan truoc roi moi gui bep. */
public interface TakeawayService {
    /** Mon dang ban (kem danh muc) de nhan vien chon. */
    List<Product> getMenu();

    /** Tao don mang ve: kiem tra mon, thanh toan, roi gui bep. */
    CreateResult create(TakeawayInput input, String employeeId);

    /** Don mang ve trong ngay (kem mon), moi nhat len dau. */
    List<Order> getTodayOrders();

    /** Giao do cho khach: chi khi moi mon (khong tinh mon da huy) da xong. */
    DeliverResult deliver(String orderId, String employeeId);

    record TakeawayItem(String productId, int quantity, String note) {
    }

    record TakeawayInput(List<TakeawayItem> items, String customerName, String customerPhone,
            String pickupTime, String paymentMethod, String image) {
    }

    enum Status { OK, EMPTY, INVALID_ITEM, INVALID_CUSTOMER, INVALID_PICKUP_TIME, INVALID_METHOD,
        IMAGE_REQUIRED, INVALID_IMAGE, ERROR }

    record CreateResult(Status status, String orderId, Integer pickupCode, BigDecimal total) {
        public static CreateResult error(Status status) {
            return new CreateResult(status, null, null, null);
        }
    }

    enum DeliverResult { OK, NOT_FOUND, NOT_READY, ALREADY_DELIVERED, ERROR }
}
