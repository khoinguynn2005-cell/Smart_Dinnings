package com.smartdining.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.dao.TableShopDAO;
import com.smartdining.dao.impl.TableShopDAOImpl;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.TableStatus;
import com.smartdining.service.KitchenService;
import com.smartdining.service.impl.KitchenServiceImpl;
import com.smartdining.utils.SecurityUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/api/kitchen/*")
public class KitchenApiServlet extends HttpServlet {
    private final KitchenService kitchenService = new KitchenServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();
    private final TableShopDAO tableShopDAO = new TableShopDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJson(response);
        if (!SecurityUtil.hasRole(request, EmployeeRole.ADMIN, EmployeeRole.KITCHEN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if ("/tables".equals(request.getPathInfo())) {
            // Danh sach ban dang su dung, de man hinh bep tai lai khi admin them / doi ten / ngung ban.
            List<Map<String, Object>> tables = new ArrayList<>();
            for (TableShop table : tableShopDAO.findAll()) {
                if (table.getStatus() == TableStatus.INACTIVE) {
                    continue;
                }
                tables.add(Map.of("id", table.getId(), "name", table.getTableName()));
            }
            tables.sort(java.util.Comparator.comparing(item -> (Integer) item.get("id")));
            mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", tables));
            return;
        }
        if (!"/tasks".equals(request.getPathInfo())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (OrderDetail task : kitchenService.getActiveTasks()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", task.getOrderDetailId());
                item.put("productName", task.getProduct().getName());
                item.put("quantity", task.getQuantity());
                item.put("note", task.getNote());
                item.put("status", task.getStatus().name());
                var order = task.getOrder();
                // Don mang ve khong co ban: gom nhom theo hoa don ("Mang ve #12")
                item.put("tableId", order.getTable() == null ? null : order.getTable().getId());
                item.put("groupKey", order.getTable() == null ? "order-" + order.getId() : "table-" + order.getTable().getId());
                item.put("groupName", order.getDisplayName());
                item.put("orderType", order.getOrderType().name());
                item.put("pickupTime", order.getPickupTime() == null ? null : order.getPickupTime().toString());
                item.put("createdTime", task.getCreatedTime() == null ? null : task.getCreatedTime().toString());
                tasks.add(item);
            }
            mapper.writeValue(response.getWriter(), Map.of("status", "success", "data", tasks));
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "Khong the tai danh sach mon"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJson(response);
        if (!SecurityUtil.hasRole(request, EmployeeRole.ADMIN, EmployeeRole.KITCHEN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            JsonNode body = mapper.readTree(request.getInputStream());
            String detailId = body.path("orderDetailId").asText();
            var employee = SecurityUtil.getEmployee(request);
            String employeeId = employee == null ? null : employee.getId();
            boolean updated;
            if ("/update-status".equals(request.getPathInfo())) {
                OrderDetailStatus status = OrderDetailStatus.valueOf(body.path("status").asText());
                updated = kitchenService.updateTaskStatus(detailId, status, employeeId);
            } else if ("/cancel-item".equals(request.getPathInfo())) {
                updated = kitchenService.cancelTask(detailId, employeeId);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!updated) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            mapper.writeValue(response.getWriter(), Map.of("status", updated ? "success" : "error"));
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("status", "error", "message", "Du lieu khong hop le"));
        }
    }

    private void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }
}
