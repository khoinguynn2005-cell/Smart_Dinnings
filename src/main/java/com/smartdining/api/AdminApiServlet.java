package com.smartdining.api;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdining.entity.Category;
import com.smartdining.entity.Employee;
import com.smartdining.entity.Product;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.EmployeeStatus;
import com.smartdining.enums.ProductStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.service.AdminService;
import com.smartdining.service.AdminService.AdminReport;
import com.smartdining.service.AdminService.DeleteResult;
import com.smartdining.service.impl.AdminServiceImpl;
import com.smartdining.utils.SecurityUtil;
import com.smartdining.websocket.AdminWebSocketServer;
import com.smartdining.websocket.CustomerWebSocketServer;
import com.smartdining.websocket.KitchenWebSocketServer;
import com.smartdining.websocket.StaffWebSocketServer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/api/admin/*")
public class AdminApiServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!hasAdminAccess(request, response)) {
            return;
        }
        prepareJson(response);
        String path = request.getPathInfo();
        if ("/overview".equals(path)) {
            mapper.writeValue(response.getWriter(), Map.of("data", adminService.getOverview()));
            return;
        }
        if ("/overview/details".equals(path)) {
            var details = adminService.getOverviewDetails(request.getParameter("type"));
            if (details == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Loại chi tiết không hợp lệ.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", details));
            return;
        }
        if (path != null && path.startsWith("/payments/") && path.endsWith("/image")) {
            String image = adminService.getPaymentImage(path.substring("/payments/".length(), path.length() - "/image".length()));
            if (image == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Không có ảnh bill cho thanh toán này.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", Map.of("image", image)));
            return;
        }
        if ("/products".equals(path)) {
            mapper.writeValue(response.getWriter(), Map.of("data", products()));
            return;
        }
        if ("/categories".equals(path)) {
            mapper.writeValue(response.getWriter(), Map.of("data", categories()));
            return;
        }
        if ("/tables".equals(path)) {
            mapper.writeValue(response.getWriter(), Map.of("data", tables()));
            return;
        }
        if ("/employees".equals(path)) {
            mapper.writeValue(response.getWriter(), Map.of("data", employees()));
            return;
        }
        if ("/reports".equals(path)) {
            LocalDate from = parseDate(request.getParameter("from"));
            LocalDate to = parseDate(request.getParameter("to"));
            if ((request.getParameter("from") != null && from == null)
                    || (request.getParameter("to") != null && to == null)
                    || (from != null && to != null && from.isAfter(to))) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Khoảng thời gian không hợp lệ.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", report(adminService.getReport(from, to))));
            return;
        }
        if ("/staff-activity".equals(path)) {
            LocalDate from = parseDate(request.getParameter("from"));
            LocalDate to = parseDate(request.getParameter("to"));
            if ((request.getParameter("from") != null && from == null)
                    || (request.getParameter("to") != null && to == null)
                    || (from != null && to != null && from.isAfter(to))) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Khoảng thời gian không hợp lệ.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", adminService.getStaffActivity(from, to)));
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!hasAdminAccess(request, response)) {
            return;
        }
        prepareJson(response);
        JsonNode payload = readPayload(request, response);
        if (payload == null) {
            return;
        }
        String path = request.getPathInfo();
        if ("/products".equals(path)) {
            Product product = adminService.createProduct(productInput(payload));
            writeEntity(response, product == null ? null : product(product), "Không thể lưu món ăn.");
            if (product != null) {
                broadcastMenuChanged();
            }
            return;
        }
        if ("/categories".equals(path)) {
            Category category = adminService.createCategory(payload.path("name").asText());
            writeEntity(response, category == null ? null : category(category), "Tên danh mục không hợp lệ hoặc đã tồn tại.");
            if (category != null) {
                broadcastMenuChanged();
            }
            return;
        }
        if ("/tables".equals(path)) {
            TableShop table = adminService.createTable(payload.path("tableName").asText());
            writeEntity(response, table == null ? null : table(table), "Không thể tạo bàn. Tên bàn có thể đã tồn tại.");
            if (table != null) {
                broadcastTablesChanged();
            }
            return;
        }
        Integer tableId = tableActionId(path, "/activate");
        if (tableId != null) {
            TableShop table = adminService.activateTable(tableId);
            writeEntity(response, table == null ? null : table(table), "Không thể kích hoạt bàn.");
            if (table != null) {
                broadcastTablesChanged();
            }
            return;
        }
        if ("/employees".equals(path)) {
            Employee employee = adminService.createEmployee(employeeInput(payload));
            writeEntity(response, employee == null ? null : employee(employee),
                    "Không thể tạo tài khoản. Kiểm tra tên đăng nhập, mật khẩu (ít nhất 8 ký tự) và email.");
            if (employee != null) {
                broadcastEmployeesChanged();
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!hasAdminAccess(request, response)) {
            return;
        }
        prepareJson(response);
        JsonNode payload = readPayload(request, response);
        if (payload == null) {
            return;
        }
        String path = request.getPathInfo();
        String id = pathId(path, "/products/");
        if (id != null) {
            Product product = adminService.updateProduct(id, productInput(payload));
            writeEntity(response, product == null ? null : product(product), "Không thể cập nhật món ăn.");
            if (product != null) {
                broadcastMenuChanged();
            }
            return;
        }
        id = pathId(path, "/categories/");
        if (id != null) {
            Category category = adminService.updateCategory(parseInteger(id), payload.path("name").asText());
            writeEntity(response, category == null ? null : category(category), "Không thể cập nhật danh mục.");
            if (category != null) {
                broadcastMenuChanged();
            }
            return;
        }
        id = pathId(path, "/tables/");
        if (id != null) {
            TableShop table = adminService.updateTable(parseInteger(id), payload.path("tableName").asText());
            writeEntity(response, table == null ? null : table(table),
                    "Chỉ có thể đổi tên bàn trống và tên bàn phải không trùng.");
            if (table != null) {
                broadcastTablesChanged();
            }
            return;
        }
        id = pathId(path, "/employees/");
        if (id != null) {
            Employee employee = adminService.updateEmployee(id, employeeInput(payload));
            writeEntity(response, employee == null ? null : employee(employee),
                    "Không thể cập nhật tài khoản. Mật khẩu mới phải có ít nhất 8 ký tự.");
            if (employee != null && employee.getStatus() == EmployeeStatus.INACTIVE) {
                SecurityUtil.invalidateEmployeeSessions(employee.getId());
            }
            if (employee != null) {
                broadcastEmployeesChanged();
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!hasAdminAccess(request, response)) {
            return;
        }
        prepareJson(response);
        String path = request.getPathInfo();
        String id = pathId(path, "/products/");
        if (id != null) {
            DeleteResult result = adminService.deleteProduct(id);
            if (!result.deleted()) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Không thể xóa món ăn.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", Map.of("archived", result.archived())));
            broadcastMenuChanged();
            return;
        }
        id = pathId(path, "/categories/");
        if (id != null) {
            if (!adminService.deleteCategory(parseInteger(id))) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Chỉ được xóa danh mục chưa có món ăn.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", Map.of("deleted", true)));
            broadcastMenuChanged();
            return;
        }
        id = pathId(path, "/tables/");
        if (id != null) {
            if (!adminService.deactivateTable(parseInteger(id))) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Chỉ được ngưng sử dụng bàn khi không có hóa đơn đang mở.");
                return;
            }
            mapper.writeValue(response.getWriter(), Map.of("data", Map.of("deactivated", true)));
            broadcastTablesChanged();
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private boolean hasAdminAccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SecurityUtil.hasRole(request, com.smartdining.enums.EmployeeRole.ADMIN)) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }

    private void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private JsonNode readPayload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            return mapper.readTree(request.getInputStream());
        } catch (Exception ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ.");
            return null;
        }
    }

    private void writeEntity(HttpServletResponse response, Object entity, String errorMessage) throws IOException {
        if (entity == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
            return;
        }
        mapper.writeValue(response.getWriter(), Map.of("data", entity));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), Map.of("message", message));
    }

    private AdminService.ProductInput productInput(JsonNode payload) {
        return new AdminService.ProductInput(
                payload.path("name").asText(),
                payload.hasNonNull("categoryId") ? payload.path("categoryId").asInt() : null,
                payload.hasNonNull("price") ? payload.path("price").decimalValue() : null,
                payload.path("imageUrl").asText(),
                payload.path("description").asText(),
                parseProductStatus(payload.path("status").asText()));
    }

    private AdminService.EmployeeInput employeeInput(JsonNode payload) {
        return new AdminService.EmployeeInput(
                payload.path("fullName").asText(),
                payload.path("username").asText(),
                payload.path("password").asText(),
                payload.path("phone").asText(),
                payload.path("email").asText(),
                payload.path("role").asText(),
                parseEmployeeStatus(payload.path("status").asText()));
    }

    private ProductStatus parseProductStatus(String value) {
        try {
            return ProductStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private EmployeeStatus parseEmployeeStatus(String value) {
        try {
            return EmployeeStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String pathId(String path, String prefix) {
        if (path == null || !path.startsWith(prefix)) {
            return null;
        }
        String id = path.substring(prefix.length());
        return id.isBlank() || id.contains("/") ? null : id;
    }

    private Integer tableActionId(String path, String actionSuffix) {
        if (path == null || !path.startsWith("/tables/") || !path.endsWith(actionSuffix)) {
            return null;
        }
        String id = path.substring("/tables/".length(), path.length() - actionSuffix.length());
        return id.contains("/") ? null : parseInteger(id);
    }

    private List<Map<String, Object>> products() {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Long> sales = adminService.getProductSales();
        for (Product product : adminService.getProducts()) {
            Map<String, Object> data = product(product);
            // So phan da ban (hoa don da thanh toan, khong tinh mon huy) cho nhan "Ban chay"
            data.put("soldQuantity", sales.getOrDefault(product.getId(), 0L));
            result.add(data);
        }
        return result;
    }

    private List<Map<String, Object>> categories() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category category : adminService.getCategories()) {
            result.add(category(category));
        }
        return result;
    }

    private List<Map<String, Object>> tables() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TableShop table : adminService.getTables()) {
            result.add(table(table));
        }
        return result;
    }

    private List<Map<String, Object>> employees() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee employee : adminService.getEmployees()) {
            result.add(employee(employee));
        }
        return result;
    }

    private Map<String, Object> product(Product product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("categoryId", product.getCategory().getId());
        data.put("categoryName", product.getCategory().getName());
        data.put("price", product.getPrice());
        data.put("imageUrl", product.getImageUrl());
        data.put("description", product.getDescription());
        data.put("status", product.getStatus().name());
        return data;
    }

    private Map<String, Object> category(Category category) {
        return Map.of("id", category.getId(), "name", category.getName());
    }

    private Map<String, Object> table(TableShop table) {
        return Map.of(
                "id", table.getId(),
                "tableName", table.getTableName(),
                "qrCodeUrl", table.getQrCodeUrl() == null ? "" : table.getQrCodeUrl(),
                "status", table.getStatus().name());
    }

    private Map<String, Object> employee(Employee employee) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", employee.getId());
        data.put("username", employee.getUsername());
        data.put("fullName", employee.getFullName());
        data.put("phone", employee.getPhone());
        data.put("email", employee.getEmail());
        data.put("role", employee.getRole().name());
        data.put("status", employee.getStatus().name());
        return data;
    }

    private Map<String, Object> report(AdminReport report) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("revenue", report.revenue());
        data.put("invoiceCount", report.invoiceCount());
        data.put("averageInvoice", report.averageInvoice());
        List<Map<String, Object>> dailyRevenue = new ArrayList<>();
        for (AdminService.DailyRevenue item : report.dailyRevenue()) {
            dailyRevenue.add(Map.of(
                    "date", item.date() == null ? "" : item.date().toString(),
                    "revenue", item.revenue(),
                    "invoiceCount", item.invoiceCount()));
        }
        List<Map<String, Object>> bestSellers = new ArrayList<>();
        for (AdminService.BestSeller item : report.bestSellers()) {
            bestSellers.add(Map.of(
                    "productName", item.productName(),
                    "quantity", item.quantity(),
                    "revenue", item.revenue()));
        }
        data.put("dailyRevenue", dailyRevenue);
        data.put("bestSellers", bestSellers);
        return data;
    }

    private void broadcastMenuChanged() {
        Map<String, String> payload = Map.of("status", WebSocketEvent.MENU_UPDATED.name());
        CustomerWebSocketServer.broadcast(payload);
        AdminWebSocketServer.broadcast(payload);
    }

    private void broadcastTablesChanged() {
        Map<String, String> payload = Map.of("status", WebSocketEvent.TABLES_UPDATED.name());
        // StaffWebSocketServer chuyen tiep su kien cho ca cac trang admin dang mo.
        StaffWebSocketServer.broadcast(payload);
        KitchenWebSocketServer.broadcast(payload);
    }

    private void broadcastEmployeesChanged() {
        AdminWebSocketServer.broadcast(Map.of("status", WebSocketEvent.EMPLOYEE_UPDATED.name()));
    }
}
