package com.smartdining.service.impl;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Base64;
import java.util.Map;

import com.smartdining.entity.Category;
import com.smartdining.entity.Employee;
import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;
import com.smartdining.entity.Product;
import com.smartdining.entity.Payment;
import com.smartdining.entity.TableRequest;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.EmployeeStatus;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.PaymentStatus;
import com.smartdining.enums.ProductStatus;
import com.smartdining.enums.TableRequestStatus;
import com.smartdining.enums.TableStatus;
import com.smartdining.service.AdminService;
import com.smartdining.utils.JpaUtil;
import com.smartdining.utils.PasswordUtil;
import com.smartdining.utils.VerificationCodeUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class AdminServiceImpl implements AdminService {
    private static final SecureRandom QR_RANDOM = new SecureRandom();
    @Override
    public AdminOverview getOverview() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
            Map<String, LocalDateTime> payTimes = paymentTimesByOrder(em);
            List<Order> paidToday = em.createQuery("SELECT o FROM Order o WHERE o.status = :status", Order.class)
                    .setParameter("status", OrderStatus.PAID)
                    .getResultList()
                    .stream()
                    .filter(order -> isWithin(paidTime(order, payTimes), startOfToday, startOfTomorrow))
                    .toList();

            BigDecimal todayRevenue = paidToday.stream()
                    .map(Order::getTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long openTables = count(em, "SELECT COUNT(o) FROM Order o WHERE o.status = :status", "status",
                    OrderStatus.UNPAID);
            long waitingPaymentTables = count(em,
                    "SELECT COUNT(t) FROM TableShop t WHERE t.status = :status", "status", TableStatus.WAITING_PAYMENT);
            long activeEmployees = count(em,
                    "SELECT COUNT(e) FROM Employee e WHERE e.status = :status", "status", EmployeeStatus.ACTIVE);
            long pendingRequests = count(em,
                    "SELECT COUNT(tr) FROM TableRequest tr WHERE tr.status = :status", "status", TableRequestStatus.PENDING);
            return new AdminOverview(todayRevenue, paidToday.size(), openTables, waitingPaymentTables, activeEmployees,
                    pendingRequests);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Category> getCategories() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Category c ORDER BY c.name ASC", Category.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Product> getProducts() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.category.name ASC, p.name ASC", Product.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<TableShop> getTables() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM TableShop t ORDER BY t.id ASC", TableShop.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Employee> getEmployees() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Employee e ORDER BY e.role ASC, e.fullName ASC", Employee.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public Product createProduct(ProductInput input) {
        if (!isValidProductInput(input)) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Category category = em.find(Category.class, input.categoryId());
            if (category == null) {
                rollback(transaction);
                return null;
            }
            Product product = new Product();
            product.setId(nextProductId(em));
            applyProductInput(product, category, input);
            em.persist(product);
            transaction.commit();
            return product;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Product updateProduct(String productId, ProductInput input) {
        if (isBlank(productId) || !isValidProductInput(input)) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Product product = em.find(Product.class, productId);
            Category category = em.find(Category.class, input.categoryId());
            if (product == null || category == null) {
                rollback(transaction);
                return null;
            }
            applyProductInput(product, category, input);
            transaction.commit();
            return product;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public DeleteResult deleteProduct(String productId) {
        if (isBlank(productId)) {
            return new DeleteResult(false, false);
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Product product = em.find(Product.class, productId);
            if (product == null) {
                rollback(transaction);
                return new DeleteResult(false, false);
            }
            long historyCount = count(em, "SELECT COUNT(od) FROM OrderDetail od WHERE od.product.id = :id", "id",
                    productId);
            boolean archived = historyCount > 0;
            if (archived) {
                product.setStatus(ProductStatus.DISCONTINUED);
            } else {
                em.remove(product);
            }
            transaction.commit();
            return new DeleteResult(true, archived);
        } catch (Exception ex) {
            rollback(transaction);
            return new DeleteResult(false, false);
        } finally {
            em.close();
        }
    }

    @Override
    public Category createCategory(String name) {
        String normalizedName = normalizeName(name, 100);
        if (normalizedName == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            if (categoryNameExists(em, normalizedName, null)) {
                rollback(transaction);
                return null;
            }
            Category category = new Category();
            category.setName(normalizedName);
            em.persist(category);
            transaction.commit();
            return category;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Category updateCategory(Integer categoryId, String name) {
        String normalizedName = normalizeName(name, 100);
        if (categoryId == null || normalizedName == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Category category = em.find(Category.class, categoryId);
            if (category == null || categoryNameExists(em, normalizedName, categoryId)) {
                rollback(transaction);
                return null;
            }
            category.setName(normalizedName);
            transaction.commit();
            return category;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deleteCategory(Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Category category = em.find(Category.class, categoryId);
            if (category == null || count(em, "SELECT COUNT(p) FROM Product p WHERE p.category.id = :id", "id",
                    categoryId) > 0) {
                rollback(transaction);
                return false;
            }
            em.remove(category);
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
    public TableShop createTable(String tableName) {
        String normalizedName = normalizeName(tableName, 50);
        if (normalizedName == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            if (tableNameExists(em, normalizedName, null)) {
                rollback(transaction);
                return null;
            }
            TableShop table = new TableShop();
            table.setTableName(normalizedName);
            table.setStatus(TableStatus.AVAILABLE);
            table.setQrCodeUrl(newQrToken());
            em.persist(table);
            transaction.commit();
            return table;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    /** Ma QR la chuoi ngau nhien 192-bit, khong doan duoc tu so ban. */
    private static String newQrToken() {
        byte[] bytes = new byte[24];
        QR_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public TableShop updateTable(Integer tableId, String tableName) {
        String normalizedName = normalizeName(tableName, 50);
        if (tableId == null || normalizedName == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableShop table = em.find(TableShop.class, tableId);
            // Chi doi ten khi ban trong hoac dang ngung hoat dong (khong co khach)
            if (table == null || (table.getStatus() != TableStatus.AVAILABLE && table.getStatus() != TableStatus.INACTIVE)
                    || tableNameExists(em, normalizedName, tableId)) {
                rollback(transaction);
                return null;
            }
            table.setTableName(normalizedName);
            transaction.commit();
            return table;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deactivateTable(Integer tableId) {
        if (tableId == null) {
            return false;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableShop table = em.find(TableShop.class, tableId);
            if (table == null || table.getStatus() == TableStatus.INACTIVE
                    || count(em, "SELECT COUNT(o) FROM Order o WHERE o.table.id = :tableId AND o.status = :status",
                            "tableId", tableId, "status", OrderStatus.UNPAID) > 0) {
                rollback(transaction);
                return false;
            }
            table.setStatus(TableStatus.INACTIVE);
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
    public TableShop activateTable(Integer tableId) {
        if (tableId == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            TableShop table = em.find(TableShop.class, tableId);
            if (table == null || table.getStatus() != TableStatus.INACTIVE) {
                rollback(transaction);
                return null;
            }
            table.setStatus(TableStatus.AVAILABLE);
            transaction.commit();
            return table;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Employee createEmployee(EmployeeInput input) {
        EmployeeRole role = parseStaffRole(input == null ? null : input.role());
        String fullName = normalizeName(input == null ? null : input.fullName(), 100);
        String username = normalizeUsername(input == null ? null : input.username());
        String password = input == null ? null : input.password();
        String email = normalizeOptional(input == null ? null : input.email(), 100);
        // Email bat buoc: nhan vien moi phai nhap ma xac thuc gui qua email o lan dang nhap dau.
        if (role == null || fullName == null || username == null || password == null || password.length() < 8
                || !isValidEmail(email)) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            long existing = count(em, "SELECT COUNT(e) FROM Employee e WHERE e.username = :username", "username",
                    username);
            if (existing > 0) {
                rollback(transaction);
                return null;
            }
            Employee employee = new Employee();
            employee.setId(nextEmployeeId(em));
            employee.setFullName(fullName);
            employee.setUsername(username);
            employee.setPassword(PasswordUtil.forStorage(password));
            employee.setPhone(normalizeOptional(input.phone(), 15));
            employee.setEmail(email);
            employee.setRole(role);
            employee.setStatus(EmployeeStatus.PENDING);
            em.persist(employee);
            transaction.commit();
            VerificationCodeUtil.issueAndSend(employee);
            return employee;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Employee updateEmployee(String employeeId, EmployeeInput input) {
        String fullName = normalizeName(input == null ? null : input.fullName(), 100);
        if (isBlank(employeeId) || fullName == null || input == null || input.status() == null) {
            return null;
        }
        String password = input.password() == null ? "" : input.password();
        if (!password.isBlank() && password.length() < 8) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Employee employee = em.find(Employee.class, employeeId);
            if (employee == null || employee.getRole() == EmployeeRole.ADMIN) {
                rollback(transaction);
                return null;
            }
            employee.setFullName(fullName);
            employee.setPhone(normalizeOptional(input.phone(), 15));
            employee.setEmail(normalizeOptional(input.email(), 100));
            employee.setStatus(input.status());
            if (!password.isBlank()) {
                employee.setPassword(PasswordUtil.forStorage(password));
            }
            transaction.commit();
            return employee;
        } catch (Exception ex) {
            rollback(transaction);
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public AdminReport getReport(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            return new AdminReport(BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of(), List.of());
        }
        EntityManager em = JpaUtil.getEntityManager();
        try {
            // Doanh thu tinh theo gio thanh toan (Payments.PayTime); hoa don cu chua co Payments dung StartTime.
            Map<String, LocalDateTime> payTimes = paymentTimesByOrder(em);
            LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
            LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
            List<Order> orders = em.createQuery(
                    "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product "
                            + "WHERE o.status = :status", Order.class)
                    .setParameter("status", OrderStatus.PAID)
                    .getResultList()
                    .stream()
                    .filter(order -> isWithin(paidTime(order, payTimes), fromTime, toTime))
                    .sorted(Comparator.comparing((Order order) -> paidTime(order, payTimes),
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            BigDecimal revenue = BigDecimal.ZERO;
            Map<LocalDate, DailyAccumulator> daily = new LinkedHashMap<>();
            Map<String, BestSellerAccumulator> bestSellers = new LinkedHashMap<>();
            for (Order order : orders) {
                BigDecimal amount = valueOrZero(order.getTotalAmount());
                revenue = revenue.add(amount);
                LocalDateTime paidAt = paidTime(order, payTimes);
                if (paidAt != null) {
                    daily.computeIfAbsent(paidAt.toLocalDate(), ignored -> new DailyAccumulator())
                            .add(amount);
                }
                if (order.getOrderDetails() == null) {
                    continue;
                }
                for (OrderDetail detail : order.getOrderDetails()) {
                    if (detail.getStatus() == OrderDetailStatus.CANCELLED || detail.getProduct() == null) {
                        continue;
                    }
                    long quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
                    if (quantity <= 0) {
                        continue;
                    }
                    bestSellers.computeIfAbsent(detail.getProduct().getName(), ignored -> new BestSellerAccumulator())
                            .add(quantity, valueOrZero(detail.getUnitPrice()).multiply(BigDecimal.valueOf(quantity)));
                }
            }
            List<DailyRevenue> dailyRevenue = daily.entrySet().stream()
                    .map(entry -> new DailyRevenue(entry.getKey(), entry.getValue().revenue, entry.getValue().invoiceCount))
                    .toList();
            List<BestSeller> topProducts = bestSellers.entrySet().stream()
                    .map(entry -> new BestSeller(entry.getKey(), entry.getValue().quantity, entry.getValue().revenue))
                    .sorted(Comparator.comparingLong(BestSeller::quantity).reversed()
                            .thenComparing(BestSeller::revenue, Comparator.reverseOrder()))
                    .limit(10)
                    .toList();
            BigDecimal average = orders.isEmpty() ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(orders.size()), 0, java.math.RoundingMode.HALF_UP);
            return new AdminReport(revenue, orders.size(), average, dailyRevenue, topProducts);
        } finally {
            em.close();
        }
    }

    private long count(EntityManager em, String jpql, String parameterName, Object parameter) {
        return em.createQuery(jpql, Long.class).setParameter(parameterName, parameter).getSingleResult();
    }

    private long count(EntityManager em, String jpql, String firstParameterName, Object firstParameter,
            String secondParameterName, Object secondParameter) {
        return em.createQuery(jpql, Long.class)
                .setParameter(firstParameterName, firstParameter)
                .setParameter(secondParameterName, secondParameter)
                .getSingleResult();
    }

    private void applyProductInput(Product product, Category category, ProductInput input) {
        product.setName(input.name().trim());
        product.setCategory(category);
        product.setPrice(input.price());
        product.setImageUrl(normalizeOptional(input.imageUrl(), 2000));
        product.setDescription(normalizeOptional(input.description(), 500));
        product.setStatus(input.status());
    }

    private boolean isValidProductInput(ProductInput input) {
        return input != null && normalizeName(input.name(), 150) != null && input.categoryId() != null
                && input.price() != null && input.price().signum() >= 0 && input.status() != null;
    }

    private boolean categoryNameExists(EntityManager em, String name, Integer ignoredId) {
        String jpql = "SELECT COUNT(c) FROM Category c WHERE LOWER(c.name) = LOWER(:name)";
        if (ignoredId != null) {
            jpql += " AND c.id <> :id";
        }
        var query = em.createQuery(jpql, Long.class).setParameter("name", name);
        if (ignoredId != null) {
            query.setParameter("id", ignoredId);
        }
        return query.getSingleResult() > 0;
    }

    private boolean tableNameExists(EntityManager em, String name, Integer ignoredId) {
        String jpql = "SELECT COUNT(t) FROM TableShop t WHERE LOWER(t.tableName) = LOWER(:name)";
        if (ignoredId != null) {
            jpql += " AND t.id <> :id";
        }
        var query = em.createQuery(jpql, Long.class).setParameter("name", name);
        if (ignoredId != null) {
            query.setParameter("id", ignoredId);
        }
        return query.getSingleResult() > 0;
    }

    private String nextProductId(EntityManager em) {
        int highest = 0;
        for (String id : em.createQuery("SELECT p.id FROM Product p", String.class).getResultList()) {
            if (id != null && id.matches("P\\d+")) {
                highest = Math.max(highest, Integer.parseInt(id.substring(1)));
            }
        }
        return String.format("P%03d", highest + 1);
    }

    private String nextEmployeeId(EntityManager em) {
        int highest = 0;
        for (String id : em.createQuery("SELECT e.id FROM Employee e", String.class).getResultList()) {
            if (id != null && id.matches("NV\\d+")) {
                highest = Math.max(highest, Integer.parseInt(id.substring(2)));
            }
        }
        return String.format("NV%06d", highest + 1);
    }

    private EmployeeRole parseStaffRole(String value) {
        try {
            EmployeeRole role = EmployeeRole.valueOf(value == null ? "" : value);
            return role == EmployeeRole.STAFF || role == EmployeeRole.KITCHEN ? role : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeName(String value, int maximumLength) {
        String normalized = normalizeOptional(value, maximumLength);
        return normalized == null || normalized.length() < 2 ? null : normalized;
    }

    private String normalizeUsername(String value) {
        String normalized = normalizeOptional(value, 50);
        return normalized == null || !normalized.matches("[A-Za-z0-9._-]{3,50}") ? null : normalized;
    }

    @Override
    public List<Map<String, Object>> getOverviewDetails(String type) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return switch (type == null ? "" : type) {
                case "paid" -> paidOrdersToday(em);
                case "open" -> openOrders(em, false);
                case "waiting" -> openOrders(em, true);
                case "employees" -> activeEmployees(em);
                case "requests" -> pendingRequests(em);
                default -> null;
            };
        } finally {
            em.close();
        }
    }

    @Override
    public String getPaymentImage(String paymentId) {
        if (isBlank(paymentId)) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Payment payment = em.find(Payment.class, paymentId);
            return payment == null ? null : payment.getImage();
        } finally {
            em.close();
        }
    }

    /** Hoa don da thanh toan hom nay (theo gio thanh toan), kem nhan vien va phuong thuc. */
    private List<Map<String, Object>> paidOrdersToday(EntityManager em) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        // Chi lay cot can thiet (khong nap anh bill) - [orderId, paymentId, method, payTime, coAnh]
        Map<String, Object[]> payments = new HashMap<>();
        for (Object[] payment : em.createQuery(
                "SELECT p.order.id, p.paymentId, p.paymentMethod, p.payTime, "
                        + "CASE WHEN p.image IS NULL THEN 0 ELSE 1 END FROM Payment p "
                        + "WHERE p.status = :status ORDER BY p.payTime ASC", Object[].class)
                .setParameter("status", PaymentStatus.COMPLETED)
                .getResultList()) {
            payments.put((String) payment[0], payment);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Order order : em.createQuery(
                "SELECT o FROM Order o LEFT JOIN FETCH o.table LEFT JOIN FETCH o.employee WHERE o.status = :status",
                Order.class).setParameter("status", OrderStatus.PAID).getResultList()) {
            Object[] payment = payments.get(order.getId());
            LocalDateTime payTime = payment == null ? null : (LocalDateTime) payment[3];
            LocalDateTime paidAt = payTime != null ? payTime : order.getStartTime();
            if (!isWithin(paidAt, start, end)) {
                continue;
            }
            boolean hasImage = payment != null && ((Number) payment[4]).intValue() == 1;
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", order.getId());
            row.put("tableName", order.getDisplayName());
            row.put("orderType", order.getOrderType().name());
            row.put("time", paidAt == null ? null : paidAt.toString());
            row.put("amount", valueOrZero(order.getTotalAmount()));
            row.put("employee", order.getEmployee() == null ? null : order.getEmployee().getFullName());
            row.put("method", payment == null ? null : payment[2]);
            row.put("paymentId", hasImage ? payment[1] : null);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("time"))).reversed());
        return rows;
    }

    /** Ban dang co hoa don mo; waitingOnly = chi ban dang cho thanh toan. */
    private List<Map<String, Object>> openOrders(EntityManager em, boolean waitingOnly) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Order order : em.createQuery(
                "SELECT DISTINCT o FROM Order o JOIN FETCH o.table LEFT JOIN FETCH o.orderDetails "
                        + "WHERE o.status = :status ORDER BY o.startTime ASC", Order.class)
                .setParameter("status", OrderStatus.UNPAID).getResultList()) {
            if (waitingOnly && order.getTable().getStatus() != TableStatus.WAITING_PAYMENT) {
                continue;
            }
            int sent = 0;
            int served = 0;
            int inCart = 0;
            for (OrderDetail detail : order.getOrderDetails()) {
                int quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
                switch (detail.getStatus()) {
                    case CART -> inCart += quantity;
                    case CANCELLED -> { }
                    case SERVED -> { sent += quantity; served += quantity; }
                    default -> sent += quantity;
                }
            }
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", order.getId());
            row.put("tableName", order.getTable().getTableName());
            row.put("tableStatus", order.getTable().getStatus().name());
            row.put("time", order.getStartTime() == null ? null : order.getStartTime().toString());
            row.put("amount", valueOrZero(order.getTotalAmount()));
            row.put("sentCount", sent);
            row.put("servedCount", served);
            row.put("cartCount", inCart);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> activeEmployees(EntityManager em) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Employee employee : em.createQuery(
                "SELECT e FROM Employee e WHERE e.status = :status ORDER BY e.role, e.fullName", Employee.class)
                .setParameter("status", EmployeeStatus.ACTIVE).getResultList()) {
            Map<String, Object> row = new HashMap<>();
            row.put("fullName", employee.getFullName());
            row.put("username", employee.getUsername());
            row.put("role", employee.getRole().name());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> pendingRequests(EntityManager em) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TableRequest request : em.createQuery(
                "SELECT tr FROM TableRequest tr JOIN FETCH tr.table WHERE tr.status = :status ORDER BY tr.requestedTime ASC",
                TableRequest.class).setParameter("status", TableRequestStatus.PENDING).getResultList()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tableName", request.getTable().getTableName());
            row.put("type", request.getRequestType().name());
            row.put("serviceType", request.getServiceType());
            row.put("note", request.getNote());
            row.put("amount", request.getTotalAmount());
            row.put("time", request.getRequestedTime() == null ? null : request.getRequestedTime().toString());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Map<String, Long> getProductSales() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Map<String, Long> sales = new HashMap<>();
            for (Object[] row : em.createQuery(
                    "SELECT od.product.id, SUM(od.quantity) FROM OrderDetail od "
                            + "WHERE od.order.status = :paid AND od.status <> :cancelled GROUP BY od.product.id", Object[].class)
                    .setParameter("paid", OrderStatus.PAID)
                    .setParameter("cancelled", OrderDetailStatus.CANCELLED)
                    .getResultList()) {
                sales.put((String) row[0], ((Number) row[1]).longValue());
            }
            return sales;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Map<String, Object>> getStaffActivity(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            return List.of();
        }
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Map<String, ActivityRow> rows = new LinkedHashMap<>();
            for (Employee employee : em.createQuery("SELECT e FROM Employee e ORDER BY e.role, e.fullName", Employee.class)
                    .getResultList()) {
                rows.put(employee.getId(), new ActivityRow(employee));
            }

            // Hoa don + tien thu (theo gio thanh toan), tach tien mat / chuyen khoan
            Map<String, Object[]> payments = new HashMap<>();
            for (Object[] payment : em.createQuery(
                    "SELECT p.order.id, p.paymentMethod, p.payTime FROM Payment p WHERE p.status = :status", Object[].class)
                    .setParameter("status", PaymentStatus.COMPLETED).getResultList()) {
                payments.put((String) payment[0], payment);
            }
            for (Object[] order : em.createQuery(
                    "SELECT o.id, o.employee.id, o.totalAmount, o.startTime FROM Order o "
                            + "WHERE o.status = :status AND o.employee IS NOT NULL", Object[].class)
                    .setParameter("status", OrderStatus.PAID).getResultList()) {
                ActivityRow row = rows.get((String) order[1]);
                Object[] payment = payments.get((String) order[0]);
                LocalDateTime paidAt = payment != null && payment[2] != null ? (LocalDateTime) payment[2] : (LocalDateTime) order[3];
                if (row == null || !isWithin(paidAt, start, end)) {
                    continue;
                }
                BigDecimal amount = valueOrZero((BigDecimal) order[2]);
                row.invoices++;
                row.revenue = row.revenue.add(amount);
                if (payment != null && "TRANSFER".equals(payment[1])) {
                    row.transfer = row.transfer.add(amount);
                } else {
                    row.cash = row.cash.add(amount);
                }
            }

            // Yeu cau cua khach da xu ly + thoi gian phan hoi
            for (Object[] request : em.createQuery(
                    "SELECT tr.completedBy.id, tr.requestedTime, tr.completedTime FROM TableRequest tr "
                            + "WHERE tr.status = :status AND tr.completedBy IS NOT NULL", Object[].class)
                    .setParameter("status", TableRequestStatus.COMPLETED).getResultList()) {
                ActivityRow row = rows.get((String) request[0]);
                LocalDateTime requestedAt = (LocalDateTime) request[1];
                LocalDateTime completedAt = (LocalDateTime) request[2];
                if (row == null || !isWithin(completedAt, start, end)) {
                    continue;
                }
                row.requests++;
                if (requestedAt != null && completedAt != null) {
                    row.responseSeconds += Math.max(0, Duration.between(requestedAt, completedAt).toSeconds());
                    row.timedRequests++;
                }
            }

            // Mon da mang ra (phuc vu)
            for (Object[] detail : em.createQuery(
                    "SELECT od.servedBy.id, od.quantity, od.servedTime FROM OrderDetail od WHERE od.servedBy IS NOT NULL",
                    Object[].class).getResultList()) {
                ActivityRow row = rows.get((String) detail[0]);
                if (row != null && isWithin((LocalDateTime) detail[2], start, end)) {
                    row.itemsServed += detail[1] == null ? 0 : (Integer) detail[1];
                }
            }

            // Bep: mon da nau xong, thoi gian nau trung binh, so lan bao het mon
            for (Object[] detail : em.createQuery(
                    "SELECT od.preparedBy.id, od.quantity, od.status, od.startedTime, od.readyTime, od.createdTime "
                            + "FROM OrderDetail od WHERE od.preparedBy IS NOT NULL", Object[].class).getResultList()) {
                ActivityRow row = rows.get((String) detail[0]);
                if (row == null) {
                    continue;
                }
                OrderDetailStatus status = (OrderDetailStatus) detail[2];
                LocalDateTime startedAt = (LocalDateTime) detail[3];
                LocalDateTime readyAt = (LocalDateTime) detail[4];
                if (status == OrderDetailStatus.CANCELLED) {
                    if (isWithin((LocalDateTime) detail[5], start, end)) {
                        row.cancelled++;
                    }
                    continue;
                }
                if (readyAt == null || !isWithin(readyAt, start, end)) {
                    continue;
                }
                row.itemsCooked += detail[1] == null ? 0 : (Integer) detail[1];
                if (startedAt != null) {
                    row.cookSeconds += Math.max(0, Duration.between(startedAt, readyAt).toSeconds());
                    row.timedCooks++;
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (ActivityRow row : rows.values()) {
                if (row.isEmpty() && row.employee.getStatus() != EmployeeStatus.ACTIVE) {
                    continue; // An nhan vien da nghi va khong co hoat dong trong ky
                }
                result.add(row.toMap());
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Gom so lieu hoat dong cua mot nhan vien trong ky bao cao. */
    private static final class ActivityRow {
        private final Employee employee;
        private int invoices;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cash = BigDecimal.ZERO;
        private BigDecimal transfer = BigDecimal.ZERO;
        private int requests;
        private long responseSeconds;
        private int timedRequests;
        private int itemsServed;
        private int itemsCooked;
        private long cookSeconds;
        private int timedCooks;
        private int cancelled;

        private ActivityRow(Employee employee) {
            this.employee = employee;
        }

        private boolean isEmpty() {
            return invoices == 0 && requests == 0 && itemsServed == 0 && itemsCooked == 0 && cancelled == 0;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("employeeId", employee.getId());
            map.put("fullName", employee.getFullName());
            map.put("username", employee.getUsername());
            map.put("role", employee.getRole().name());
            map.put("status", employee.getStatus() == null ? null : employee.getStatus().name());
            map.put("invoices", invoices);
            map.put("revenue", revenue);
            map.put("cash", cash);
            map.put("transfer", transfer);
            map.put("requests", requests);
            map.put("avgResponseMinutes", timedRequests == 0 ? null : Math.round(responseSeconds / 6.0 / timedRequests) / 10.0);
            map.put("itemsServed", itemsServed);
            map.put("itemsCooked", itemsCooked);
            map.put("avgCookMinutes", timedCooks == 0 ? null : Math.round(cookSeconds / 6.0 / timedCooks) / 10.0);
            map.put("cancelled", cancelled);
            return map;
        }
    }

    /** Gio thanh toan cua tung hoa don (theo Payments da COMPLETED). */
    private Map<String, LocalDateTime> paymentTimesByOrder(EntityManager em) {
        Map<String, LocalDateTime> result = new HashMap<>();
        List<Object[]> rows = em.createQuery(
                "SELECT p.order.id, MAX(p.payTime) FROM Payment p WHERE p.status = :status GROUP BY p.order.id",
                Object[].class)
                .setParameter("status", PaymentStatus.COMPLETED)
                .getResultList();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                result.put((String) row[0], (LocalDateTime) row[1]);
            }
        }
        return result;
    }

    private LocalDateTime paidTime(Order order, Map<String, LocalDateTime> payTimes) {
        LocalDateTime payTime = payTimes.get(order.getId());
        return payTime != null ? payTime : order.getStartTime();
    }

    /** from (bao gom) .. to (khong bao gom); null = khong gioi han. */
    private boolean isWithin(LocalDateTime time, LocalDateTime from, LocalDateTime to) {
        if (time == null) {
            return from == null && to == null;
        }
        return (from == null || !time.isBefore(from)) && (to == null || time.isBefore(to));
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private String normalizeOptional(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.length() > maximumLength ? null : normalized;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

    private static final class DailyAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long invoiceCount;

        private void add(BigDecimal amount) {
            revenue = revenue.add(amount);
            invoiceCount++;
        }
    }

    private static final class BestSellerAccumulator {
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private void add(long addedQuantity, BigDecimal addedRevenue) {
            quantity += addedQuantity;
            revenue = revenue.add(addedRevenue);
        }
    }
}
