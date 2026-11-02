package com.smartdining.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.TableRequest;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.TableStatus;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.entity.Product;
import com.smartdining.service.StaffService;
import com.smartdining.service.TakeawayService;
import com.smartdining.service.impl.TakeawayServiceImpl;
import com.smartdining.service.impl.StaffServiceImpl;
import com.smartdining.utils.SecurityUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/api/staff/*")
public class StaffApiServlet extends HttpServlet {
    private final StaffService staffService = new StaffServiceImpl();
    private final TakeawayService takeawayService = new TakeawayServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if ("/tasks".equals(request.getPathInfo())) {
            writeReadyTasks(response);
            return;
        }
        if ("/tables".equals(request.getPathInfo())) {
            writeOpenTables(response);
            return;
        }
        if ("/table-details".equals(request.getPathInfo())) {
            writeTableDetails(request, response);
            return;
        }
        if ("/requests".equals(request.getPathInfo())) {
            writePendingRequests(response);
            return;
        }
        if ("/bill".equals(request.getPathInfo())) {
            writeBill(request, response);
            return;
        }
        if ("/menu".equals(request.getPathInfo())) {
            writeMenu(response);
            return;
        }
        if ("/takeaways".equals(request.getPathInfo())) {
            writeTakeaways(response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void writeReadyTasks(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (OrderDetail detail : staffService.getReadyTasks()) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", detail.getOrderDetailId());
            task.put("tableId", detail.getOrder().getTable().getId());
            task.put("tableName", detail.getOrder().getTable().getTableName());
            task.put("productName", detail.getProduct().getName());
            task.put("quantity", detail.getQuantity());
            task.put("note", detail.getNote());
            task.put("createdTime", detail.getCreatedTime() == null ? null : detail.getCreatedTime().toString());
            tasks.add(task);
        }
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", tasks));
    }

    private void writeOpenTables(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> tables = new ArrayList<>();
        Map<Integer, Order> openOrdersByTable = new HashMap<>();
        for (Order order : staffService.getOpenOrders()) {
            if (order.getTable() != null && order.getTable().getId() != null) {
                openOrdersByTable.put(order.getTable().getId(), order);
            }
        }
        for (TableShop table : staffService.getAllTables()) {
            Order order = openOrdersByTable.get(table.getId());
            tables.add(order == null ? toEmptyTableSummary(table) : toTableSummary(order));
        }
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", tables));
    }

    private void writeTableDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer tableId = parseTableId(request.getParameter("tableId"));
        if (tableId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "tableId khong hop le"));
            return;
        }

        Order order = staffService.getOpenOrderByTable(tableId);
        if (order == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "Ban khong co hoa don dang mo"));
            return;
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", detail.getOrderDetailId());
            item.put("productName", detail.getProduct().getName());
            item.put("quantity", detail.getQuantity());
            item.put("unitPrice", detail.getUnitPrice());
            item.put("note", detail.getNote());
            item.put("status", detail.getStatus().name());
            item.put("createdTime", detail.getCreatedTime() == null ? null : detail.getCreatedTime().toString());
            details.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("table", toTableSummary(order));
        data.put("items", details);
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", data));
    }

    private Map<String, Object> toTableSummary(Order order) {
        int readyCount = 0;
        int unservedCount = 0;
        int confirmedCount = 0;
        List<OrderDetail> details = order.getOrderDetails() == null ? List.of() : order.getOrderDetails();
        for (OrderDetail detail : details) {
            if (detail.getStatus() == OrderDetailStatus.READY) {
                readyCount += detail.getQuantity();
            }
            if (detail.getStatus() != OrderDetailStatus.SERVED && detail.getStatus() != OrderDetailStatus.CANCELLED) {
                unservedCount += detail.getQuantity();
            }
            if (detail.getStatus() != OrderDetailStatus.CART && detail.getStatus() != OrderDetailStatus.CANCELLED) {
                confirmedCount += detail.getQuantity();
            }
        }

        Map<String, Object> table = new HashMap<>();
        table.put("tableId", order.getTable().getId());
        table.put("tableName", order.getTable().getTableName());
        table.put("tableStatus", order.getTable().getStatus().name());
        table.put("orderId", order.getId());
        table.put("totalAmount", order.getTotalAmount());
        table.put("itemCount", details.size());
        table.put("confirmedCount", confirmedCount);
        table.put("unservedCount", unservedCount);
        table.put("readyCount", readyCount);
        return table;
    }

    private Map<String, Object> toEmptyTableSummary(TableShop table) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("tableId", table.getId());
        summary.put("tableName", table.getTableName());
        summary.put("tableStatus", table.getStatus() == null ? TableStatus.AVAILABLE.name() : table.getStatus().name());
        summary.put("orderId", null);
        summary.put("totalAmount", BigDecimal.ZERO);
        summary.put("itemCount", 0);
        summary.put("confirmedCount", 0);
        summary.put("unservedCount", 0);
        summary.put("readyCount", 0);
        return summary;
    }

    private Integer parseTableId(String rawTableId) {
        try {
            int tableId = Integer.parseInt(rawTableId);
            return tableId > 0 ? tableId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writePendingRequests(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> requests = new ArrayList<>();
        for (TableRequest tableRequest : staffService.getPendingRequests()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tableRequest.getId());
            item.put("tableId", tableRequest.getTable().getId());
            item.put("tableName", tableRequest.getTable().getTableName());
            item.put("requestType", tableRequest.getRequestType().name());
            item.put("serviceType", tableRequest.getServiceType());
            item.put("note", tableRequest.getNote());
            item.put("totalAmount", tableRequest.getTotalAmount());
            item.put("requestedTime", tableRequest.getRequestedTime() == null ? null
                    : tableRequest.getRequestedTime().toString());
            requests.add(item);
        }
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", requests));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if ("/serve".equals(request.getPathInfo())) {
            markServed(request, response);
            return;
        }
        if ("/complete-request".equals(request.getPathInfo())) {
            completeRequest(request, response);
            return;
        }
        if ("/checkout".equals(request.getPathInfo())) {
            checkout(request, response);
            return;
        }
        if ("/takeaway".equals(request.getPathInfo())) {
            createTakeaway(request, response);
            return;
        }
        if ("/takeaway/deliver".equals(request.getPathInfo())) {
            deliverTakeaway(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void markServed(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode root = mapper.readTree(request.getInputStream());
            String orderDetailId = root.path("orderDetailId").asText();
            var employee = SecurityUtil.getEmployee(request);
            boolean updated = staffService.markServed(orderDetailId, employee == null ? null : employee.getId());
            writeMutationResult(response, updated);
        } catch (Exception ex) {
            writeInvalidPayload(response);
        }
    }

    private void completeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode root = mapper.readTree(request.getInputStream());
            String requestId = root.path("requestId").asText();
            var employee = SecurityUtil.getEmployee(request);
            boolean updated = employee != null && staffService.completeRequest(requestId, employee.getId());
            writeMutationResult(response, updated);
        } catch (Exception ex) {
            writeInvalidPayload(response);
        }
    }

    private void writeBill(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer tableId = parseTableId(request.getParameter("tableId"));
        StaffService.Bill bill = tableId == null ? null : staffService.getBill(tableId);
        if (bill == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "Ban khong co hoa don dang mo"));
            return;
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (StaffService.BillItem item : bill.items()) {
            Map<String, Object> line = new HashMap<>();
            line.put("id", item.id());
            line.put("productName", item.productName());
            line.put("quantity", item.quantity());
            line.put("unitPrice", item.unitPrice());
            line.put("lineTotal", item.lineTotal());
            line.put("status", item.status());
            items.add(line);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("tableId", bill.tableId());
        data.put("tableName", bill.tableName());
        data.put("orderId", bill.orderId());
        data.put("startTime", bill.startTime() == null ? null : bill.startTime().toString());
        data.put("items", items);
        data.put("total", bill.total());
        data.put("paymentRequested", bill.paymentRequested());
        data.put("unservedCount", bill.unservedCount());
        data.put("canPay", bill.paymentRequested() && bill.unservedCount() == 0);
        var employee = SecurityUtil.getEmployee(request);
        data.put("cashier", employee == null ? null : employee.getFullName());
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", data));
    }

    private void checkout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode root = mapper.readTree(request.getInputStream());
            var employee = SecurityUtil.getEmployee(request);
            StaffService.CheckoutResult result = staffService.checkout(
                    parseTableId(root.path("tableId").asText()),
                    employee == null ? null : employee.getId(),
                    root.path("method").asText(),
                    root.hasNonNull("image") ? root.path("image").asText() : null);
            if (result != StaffService.CheckoutResult.OK) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            mapper.writeValue(response.getWriter(), Map.of(
                    "status", result == StaffService.CheckoutResult.OK ? "success" : "error",
                    "code", result.name(),
                    "message", checkoutMessage(result)));
        } catch (Exception ex) {
            writeInvalidPayload(response);
        }
    }

    private String checkoutMessage(StaffService.CheckoutResult result) {
        return switch (result) {
            case OK -> "Da thanh toan";
            case NO_OPEN_ORDER -> "Ban khong co hoa don dang mo";
            case NOT_REQUESTED -> "Khach chua yeu cau thanh toan";
            case ITEMS_NOT_SERVED -> "Con mon chua phuc vu, chua the thanh toan";
            case INVALID_METHOD -> "Phuong thuc thanh toan khong hop le";
            case IMAGE_REQUIRED -> "Chuyen khoan can anh chup bill";
            case INVALID_IMAGE -> "Anh bill khong hop le hoac qua lon (toi da 3MB)";
            case ERROR -> "Khong the thanh toan luc nay";
        };
    }

    // ---------- Ban mang ve ----------
    private void writeMenu(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> products = new ArrayList<>();
        for (Product product : takeawayService.getMenu()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("name", product.getName());
            item.put("price", product.getPrice());
            item.put("imageUrl", product.getImageUrl());
            item.put("categoryId", product.getCategory() == null ? null : product.getCategory().getId());
            item.put("categoryName", product.getCategory() == null ? null : product.getCategory().getName());
            products.add(item);
        }
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", products));
    }

    private void writeTakeaways(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Order order : takeawayService.getTodayOrders()) {
            List<Map<String, Object>> items = new ArrayList<>();
            int active = 0;
            int ready = 0;
            BigDecimal refund = BigDecimal.ZERO;
            for (OrderDetail detail : order.getOrderDetails()) {
                Map<String, Object> line = new HashMap<>();
                line.put("id", detail.getOrderDetailId());
                line.put("productName", detail.getProduct().getName());
                line.put("quantity", detail.getQuantity());
                line.put("note", detail.getNote());
                line.put("status", detail.getStatus().name());
                items.add(line);
                switch (detail.getStatus()) {
                    case PENDING, PREPARING -> active++;
                    case READY -> ready++;
                    case CANCELLED -> refund = refund.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
                    default -> { }
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", order.getId());
            data.put("pickupCode", order.getPickupCode());
            data.put("customerName", order.getCustomerName());
            data.put("customerPhone", order.getCustomerPhone());
            data.put("pickupTime", order.getPickupTime() == null ? null : order.getPickupTime().toString());
            data.put("createdTime", order.getStartTime() == null ? null : order.getStartTime().toString());
            data.put("totalAmount", order.getTotalAmount());
            data.put("refundAmount", refund);
            data.put("employee", order.getEmployee() == null ? null : order.getEmployee().getFullName());
            // preparing: bep dang lam | ready: cho giao | delivered: da giao xong
            data.put("state", active > 0 ? "preparing" : ready > 0 ? "ready" : "delivered");
            data.put("items", items);
            orders.add(data);
        }
        mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", orders));
    }

    private void createTakeaway(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode root = mapper.readTree(request.getInputStream());
            List<TakeawayService.TakeawayItem> items = new ArrayList<>();
            for (JsonNode node : root.path("items")) {
                items.add(new TakeawayService.TakeawayItem(node.path("productId").asText(),
                        node.path("quantity").asInt(), node.path("note").asText("")));
            }
            TakeawayService.TakeawayInput input = new TakeawayService.TakeawayInput(items,
                    root.path("customerName").asText(""), root.path("customerPhone").asText(""),
                    root.path("pickupTime").asText(""), root.path("method").asText(""),
                    root.hasNonNull("image") ? root.path("image").asText() : null);
            var employee = SecurityUtil.getEmployee(request);
            TakeawayService.CreateResult result = takeawayService.create(input, employee == null ? null : employee.getId());
            if (result.status() != TakeawayService.Status.OK) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("status", "error", "code", result.status().name(),
                        "message", takeawayMessage(result.status())));
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", Map.of(
                    "orderId", result.orderId(), "pickupCode", result.pickupCode(), "total", result.total())));
        } catch (Exception ex) {
            writeInvalidPayload(response);
        }
    }

    private void deliverTakeaway(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode root = mapper.readTree(request.getInputStream());
            var employee = SecurityUtil.getEmployee(request);
            TakeawayService.DeliverResult result = takeawayService.deliver(root.path("orderId").asText(),
                    employee == null ? null : employee.getId());
            if (result != TakeawayService.DeliverResult.OK) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            mapper.writeValue(response.getWriter(), Map.of(
                    "status", result == TakeawayService.DeliverResult.OK ? "success" : "error",
                    "code", result.name(),
                    "message", switch (result) {
                        case OK -> "Da giao do cho khach";
                        case NOT_FOUND -> "Khong tim thay don mang ve";
                        case NOT_READY -> "Bep chua lam xong tat ca mon";
                        case ALREADY_DELIVERED -> "Don nay da giao roi";
                        case ERROR -> "Khong the giao don luc nay";
                    }));
        } catch (Exception ex) {
            writeInvalidPayload(response);
        }
    }

    private String takeawayMessage(TakeawayService.Status status) {
        return switch (status) {
            case OK -> "Da tao don mang ve";
            case EMPTY -> "Chua chon mon nao";
            case INVALID_ITEM -> "Mon hoac so luong khong hop le (mon phai dang ban, so luong 1-100)";
            case INVALID_CUSTOMER -> "Ten hoac so dien thoai khong hop le";
            case INVALID_PICKUP_TIME -> "Gio lay do khong hop le (dinh dang HH:mm)";
            case INVALID_METHOD -> "Phuong thuc thanh toan khong hop le";
            case IMAGE_REQUIRED -> "Chuyen khoan can anh chup bill";
            case INVALID_IMAGE -> "Anh bill khong hop le hoac qua lon (toi da 3MB)";
            case ERROR -> "Khong the tao don luc nay";
        };
    }

    private void writeMutationResult(HttpServletResponse response, boolean updated) throws IOException {
        if (!updated) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        mapper.writeValue(response.getWriter(), Map.of("status", updated ? "success" : "error"));
    }

    private void writeInvalidPayload(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "Du lieu khong hop le"));
    }

}
