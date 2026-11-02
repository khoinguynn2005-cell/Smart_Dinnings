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
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.service.OrderService;
import com.smartdining.service.TableShopService;
import com.smartdining.service.impl.OrderServiceImpl;
import com.smartdining.service.impl.TableShopServiceImpl;
import com.smartdining.utils.BugLog;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@SuppressWarnings("serial")
@WebServlet("/api/order/*")
public class CustomerOrderServlet extends HttpServlet {
    private final OrderService orderService = new OrderServiceImpl();
    private final TableShopService tableShopService = new TableShopServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer tableId = getTableId(request, response);
        if (tableId == null) {
            return;
        }
        if (!"/get-cart".equals(request.getPathInfo())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            List<Map<String, Object>> items = new ArrayList<>();
            BigDecimal currentOrderTotal = BigDecimal.ZERO;
            for (OrderDetail detail : orderService.getOrderDetailsByTable(tableId)) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", detail.getOrderDetailId());
                item.put("productId", detail.getProduct().getId());
                item.put("name", detail.getProduct().getName());
                item.put("price", detail.getUnitPrice());
                item.put("qty", detail.getQuantity());
                item.put("note", detail.getNote());
                item.put("status", detail.getStatus().name());
                item.put("createdTime", detail.getCreatedTime() == null ? null : detail.getCreatedTime().toString());
                items.add(item);

                if (detail.getStatus() == OrderDetailStatus.CART
                        && detail.getUnitPrice() != null && detail.getQuantity() != null) {
                    currentOrderTotal = currentOrderTotal.add(
                            detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
                }
            }
            Order order = orderService.getActiveOrderByTable(tableId);
            BigDecimal confirmedTotal = order == null || order.getTotalAmount() == null
                    ? BigDecimal.ZERO
                    : order.getTotalAmount();
            writeJson(response, HttpServletResponse.SC_OK, Map.of(
                    "status", "success",
                    "items", items,
                    "totalAmount", confirmedTotal,
                    "currentOrderTotal", currentOrderTotal,
                    "projectedTotal", confirmedTotal.add(currentOrderTotal)));
        } catch (Exception ex) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("status", "error", "message", "Khong the tai gio hang"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer tableId = getTableId(request, response);
        if (tableId == null) {
            return;
        }

        String path = request.getPathInfo();
        try {
            if ("/add-cart".equals(path)) {
                JsonNode body = mapper.readTree(request.getInputStream());
                boolean added = orderService.addItemToCart(
                        tableId,
                        body.path("productId").asText(),
                        body.path("quantity").asInt(),
                        body.path("note").asText(""));
                writeJson(response, added ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", added ? "success" : "error",
                                "message", added ? "Da them vao gio" : "Mon an hoac so luong khong hop le"));
                return;
            }

            if ("/confirm".equals(path)) {
                boolean confirmed = orderService.confirmOrder(tableId);
                writeJson(response, confirmed ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", confirmed ? "success" : "error",
                                "message", confirmed ? "Don da duoc gui xuong bep" : "Gio hang dang trong"));
                return;
            }

            if ("/remove-cart-item".equals(path)) {
                JsonNode body = mapper.readTree(request.getInputStream());
                boolean removed = orderService.removeCartItem(tableId, body.path("orderDetailId").asText());
                writeJson(response, removed ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", removed ? "success" : "error",
                                "message", removed ? "Da huy mon" : "Chi co the huy mon chua gui bep"));
                return;
            }

            if ("/update-cart-item".equals(path)) {
                JsonNode body = mapper.readTree(request.getInputStream());
                boolean updated = orderService.updateCartItemQuantity(tableId,
                        body.path("orderDetailId").asText(), body.path("quantity").asInt());
                writeJson(response, updated ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", updated ? "success" : "error",
                                "message", updated ? "Da cap nhat so luong" : "Chi doi duoc so luong mon chua gui bep (1-100)"));
                return;
            }

            if ("/request-payment".equals(path)) {
                boolean requested = orderService.requestPayment(tableId);
                writeJson(response, requested ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", requested ? "success" : "error",
                                "message", requested ? "Da gui yeu cau thanh toan toi nhan vien"
                                        : "Khong tim thay hoa don dang mo"));
                return;
            }

            if ("/request-service".equals(path)) {
                JsonNode body = mapper.readTree(request.getInputStream());
                String type = body.path("type").asText("").trim();
                String note = body.path("note").asText("").trim();
                if (type.isEmpty() || type.length() > 80 || note.length() > 255) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                            Map.of("status", "error", "message", "Noi dung yeu cau khong hop le"));
                    return;
                }

                boolean requested = orderService.requestService(tableId, type, note);
                writeJson(response, requested ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("status", requested ? "success" : "error",
                                "message", requested ? "Da gui yeu cau toi nhan vien"
                                        : "Khong the gui yeu cau luc nay"));
                return;
            }

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception ex) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("status", "error", "message", "Du lieu khong hop le"));
        }
    }

    private Integer getTableId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String requestedTableId = request.getParameter("tableId");
        if (requestedTableId != null && !requestedTableId.isBlank()) {
            try {
                int tableId = Integer.parseInt(requestedTableId);
                if (tableId > 0 && tableShopService.findById(tableId) != null) {
                    HttpSession session = request.getSession(false);
                    Object scannedTable = session == null ? null : session.getAttribute("currentTableId");
                    if (!Integer.valueOf(tableId).equals(scannedTable)) {
                        BugLog.warn(5, request.getMethod() + " " + request.getRequestURI() + "?tableId=" + tableId
                                + " tu " + request.getRemoteAddr() + " duoc chap nhan du "
                                + (scannedTable == null ? "CHUA quet QR ban nao" : "dang ngoi ban " + scannedTable)
                                + " - chi can doi tableId tren URL.");
                    }
                    return tableId;
                }
            } catch (NumberFormatException ignored) {
                // Return the standard invalid-table response below.
            }
            writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                    Map.of("status", "error", "message", "So ban khong hop le"));
            return null;
        }

        HttpSession session = request.getSession(false);
        Object tableId = session == null ? null : session.getAttribute("currentTableId");
        if (!(tableId instanceof Integer)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    Map.of("status", "error", "message", "Phien quet QR da het han"));
            return null;
        }
        return (Integer) tableId;
    }

    private void writeJson(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), payload);
    }
}
