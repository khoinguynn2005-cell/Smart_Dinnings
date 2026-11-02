package com.smartdining.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.smartdining.entity.Category;
import com.smartdining.entity.Employee;
import com.smartdining.entity.Product;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.EmployeeStatus;
import com.smartdining.enums.ProductStatus;

public interface AdminService {
    AdminOverview getOverview();

    /** Chi tiet tung o o trang Tong quan: paid | open | waiting | employees | requests (null neu type sai). */
    List<Map<String, Object>> getOverviewDetails(String type);

    /** Anh bill chuyen khoan (data URL) cua mot thanh toan. */
    String getPaymentImage(String paymentId);

    /** So phan da ban cua tung mon: productId -> so luong. */
    Map<String, Long> getProductSales();

    /** Hoat dong cua tung nhan vien trong ky (hoa don, tien thu, yeu cau, mon phuc vu, mon bep nau). */
    List<Map<String, Object>> getStaffActivity(LocalDate from, LocalDate to);

    List<Category> getCategories();

    List<Product> getProducts();

    List<TableShop> getTables();

    List<Employee> getEmployees();

    Product createProduct(ProductInput input);

    Product updateProduct(String productId, ProductInput input);

    DeleteResult deleteProduct(String productId);

    Category createCategory(String name);

    Category updateCategory(Integer categoryId, String name);

    boolean deleteCategory(Integer categoryId);

    TableShop createTable(String tableName);

    TableShop updateTable(Integer tableId, String tableName);

    boolean deactivateTable(Integer tableId);

    TableShop activateTable(Integer tableId);

    Employee createEmployee(EmployeeInput input);

    Employee updateEmployee(String employeeId, EmployeeInput input);

    AdminReport getReport(LocalDate from, LocalDate to);

    record ProductInput(String name, Integer categoryId, BigDecimal price, String imageUrl,
            String description, ProductStatus status) {
    }

    record EmployeeInput(String fullName, String username, String password, String phone,
            String email, String role, EmployeeStatus status) {
    }

    record DeleteResult(boolean deleted, boolean archived) {
    }

    record AdminOverview(BigDecimal todayRevenue, long todayPaidOrders, long openTables,
            long waitingPaymentTables, long activeEmployees, long pendingRequests) {
    }

    record DailyRevenue(LocalDate date, BigDecimal revenue, long invoiceCount) {
    }

    record BestSeller(String productName, long quantity, BigDecimal revenue) {
    }

    record AdminReport(BigDecimal revenue, long invoiceCount, BigDecimal averageInvoice,
            List<DailyRevenue> dailyRevenue, List<BestSeller> bestSellers) {
    }
}
