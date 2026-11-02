package com.smartdining.bugs;

import java.math.BigDecimal;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kiem chung cac loi nghiem trong tim thay khi doc code (muc 1-7 trong docs/testing/serious-bugs.md).
 * Moi test mo ta hanh vi DUNG; khi loi con ton tai thi test FAIL. Chua sua code ung dung.
 *
 * Can: Tomcat chay SmartDining tai baseUrl va SQL Server (doc cau hinh tu META-INF/config.properties).
 * Du lieu test la cac ban ten "QA BUG ..." va duoc xoa truoc/sau khi chay.
 */
public class SeriousBugTest {
    private static final int PARALLEL_REQUESTS = 10;

    private String baseUrl;
    private String run;
    private int tableCounter;
    private ApiClient admin;
    private ApiClient staff;
    private ApiClient kitchen;
    private String productId;
    private BigDecimal productPrice;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        baseUrl = setting("baseUrl", "SMARTDINING_BASE_URL", "http://localhost:8081/SmartDining").replaceAll("/+$", "");
        run = Long.toString(System.currentTimeMillis() % 1_000_000);
        TestDatabase.cleanup();

        admin = loggedIn("admin", setting("adminUser", "SMARTDINING_ADMIN_USER", "admin"),
                setting("adminPassword", "SMARTDINING_ADMIN_PASSWORD", "Admin@123"));
        staff = loggedIn("staff", setting("staffUser", "SMARTDINING_STAFF_USER", "staff1"),
                setting("staffPassword", "SMARTDINING_STAFF_PASSWORD", "Staff@123"));
        kitchen = loggedIn("kitchen", setting("kitchenUser", "SMARTDINING_KITCHEN_USER", "kitchen1"),
                setting("kitchenPassword", "SMARTDINING_KITCHEN_PASSWORD", "Kitchen@123"));

        productId = (String) TestDatabase.scalar(
                "SELECT TOP 1 ID FROM Products WHERE Status = 'AVAILABLE' AND Price > 0 ORDER BY ID");
        Assert.assertNotNull(productId, "Database can it nhat mot mon AVAILABLE co gia > 0.");
        productPrice = TestDatabase.decimal("SELECT Price FROM Products WHERE ID = ?", productId);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        TestDatabase.cleanup();
    }

    // ---------- Loi 1: hai khach cung ban goi mon cung luc ----------

    @Test(description = "Loi 1: nhieu dien thoai cung ban them mon cung luc chi duoc tao MOT hoa don")
    public void concurrentAddToCartCreatesSingleOrder() throws Exception {
        // Lap lai vai lan vi loi canh tranh khong phai lan nao cung xay ra
        for (int round = 1; round <= 3; round++) {
            int tableId = createTable();
            ApiClient customer = customerAt(tableId);

            List<ApiClient.Response> responses = concurrently(PARALLEL_REQUESTS, i -> customer.postJson(
                    orderApi("add-cart", tableId), ApiClient.body("productId", productId, "quantity", 1, "note", "")));

            long succeeded = responses.stream().filter(ApiClient.Response::ok).count();
            long openOrders = TestDatabase.count(
                    "SELECT COUNT(*) FROM Orders WHERE Table_ID = ? AND Status = 'UNPAID'", tableId);
            Assert.assertEquals(openOrders, 1L, "Vong " + round + ": " + succeeded + "/" + PARALLEL_REQUESTS
                    + " request them mon thanh cong nhung ban " + tableId + " co " + openOrders
                    + " hoa don dang mo (mong doi dung 1).");
        }
    }

    // ---------- Loi 2: bam "Gui bep" cung luc ----------

    @Test(description = "Loi 2: bam Gui bep cung luc tu nhieu dien thoai chi cong tien mot lan")
    public void concurrentConfirmCountsTotalOnce() throws Exception {
        int tableId = createTable();
        ApiClient customer = customerAt(tableId);
        assertOk(customer.postJson(orderApi("add-cart", tableId),
                ApiClient.body("productId", productId, "quantity", 2, "note", "")), "Them mon");
        BigDecimal expected = productPrice.multiply(BigDecimal.valueOf(2));

        List<ApiClient.Response> responses = concurrently(PARALLEL_REQUESTS,
                i -> customer.postJson(orderApi("confirm", tableId), "{}"));

        long succeeded = responses.stream().filter(ApiClient.Response::ok).count();
        BigDecimal total = TestDatabase.decimal(
                "SELECT TotalAmount FROM Orders WHERE Table_ID = ? AND Status = 'UNPAID'", tableId);
        Assert.assertEquals(succeeded, 1L, "Chi mot lan Gui bep duoc chap nhan, thuc te " + succeeded + " lan.");
        Assert.assertEquals(total.compareTo(expected), 0,
                "Tong tien hoa don phai la " + expected + " nhung la " + total + ".");
    }

    // ---------- Loi 3: hai nhan vien cung thanh toan mot ban ----------

    @Test(description = "Loi 3: nhieu yeu cau thanh toan cung luc chi tao MOT ban ghi Payment")
    public void concurrentCheckoutCreatesSinglePayment() throws Exception {
        int tableId = createTable();
        String orderId = prepareTableReadyToPay(tableId);
        // Hai thiet bi: nhan vien phuc vu va admin cung bam "Hoan tat thanh toan"
        ApiClient[] cashiers = { staff, admin };

        List<ApiClient.Response> responses = concurrently(PARALLEL_REQUESTS, i -> cashiers[i % 2].postJson(
                "/api/staff/checkout", ApiClient.body("tableId", tableId, "method", "CASH")));

        long succeeded = responses.stream().filter(ApiClient.Response::ok).count();
        long payments = TestDatabase.count("SELECT COUNT(*) FROM Payments WHERE Order_ID = ?", orderId);
        Assert.assertEquals(payments, 1L, succeeded + " lan thanh toan thanh cong, bang Payments co "
                + payments + " dong cho hoa don " + orderId + " (mong doi 1).");
    }

    // ---------- Loi 4: WebSocket ngung nhan su kien ----------

    @Test(description = "Loi 4: man hinh phuc vu nhan du moi su kien khi server gui lien tiep, va van nhan tiep sau do")
    public void staffWebSocketReceivesEveryEventUnderBurst() throws Exception {
        int tableId = createTable();
        ApiClient customer = customerAt(tableId);
        // Ban phai co khach (FULL) moi gui duoc yeu cau phuc vu
        assertOk(customer.postJson(orderApi("add-cart", tableId),
                ApiClient.body("productId", productId, "quantity", 1, "note", "")), "Them mon");

        ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
        WebSocket socket = staff.openWebSocket("/ws/staff", collector(messages));
        try {
            int burst = 20;
            List<ApiClient.Response> responses = concurrently(burst, i -> customer.postJson(
                    orderApi("request-service", tableId),
                    ApiClient.body("type", "QA test " + i, "note", "")));
            long accepted = responses.stream().filter(ApiClient.Response::ok).count();

            // Mot su kien rieng sau dot gui: tao them ban -> TABLES_UPDATED
            createTable();

            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (System.nanoTime() < deadline && !(countEvents(messages, "SERVICE_REQUEST") >= accepted
                    && countEvents(messages, "TABLES_UPDATED") >= 1)) {
                Thread.sleep(200);
            }
            long received = countEvents(messages, "SERVICE_REQUEST");
            Assert.assertEquals(received, accepted, "Server nhan " + accepted
                    + " yeu cau phuc vu nhung man hinh phuc vu chi nhan " + received + " su kien.");
            Assert.assertTrue(countEvents(messages, "TABLES_UPDATED") >= 1,
                    "Sau dot su kien lien tiep, man hinh phuc vu khong con nhan su kien moi (TABLES_UPDATED).");
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    // ---------- Loi 5: ma QR khong bao ve ban ----------

    @Test(description = "Loi 5a: chua quet QR thi khong goi mon cho ban bang cach doi tableId")
    public void orderApiRejectsTableIdWithoutQrScan() {
        int tableId = createTable();
        ApiClient stranger = new ApiClient(baseUrl); // khong quet QR, khong co session

        ApiClient.Response response = stranger.postJson(orderApi("add-cart", tableId),
                ApiClient.body("productId", productId, "quantity", 1, "note", ""));

        Assert.assertFalse(response.ok(), "Nguoi chua quet QR van them mon vao ban " + tableId
                + " chi bang ?tableId= (HTTP " + response.status() + ").");
    }

    @Test(description = "Loi 5b: khach dang ngoi ban A khong xem duoc gio hang cua ban B")
    public void customerCannotReadAnotherTablesCart() {
        int tableA = createTable();
        int tableB = createTable();
        assertOk(customerAt(tableB).postJson(orderApi("add-cart", tableB),
                ApiClient.body("productId", productId, "quantity", 1, "note", "")), "Ban B goi mon");
        ApiClient customerA = customerAt(tableA);

        ApiClient.Response response = customerA.get(orderApi("get-cart", tableB));

        Assert.assertFalse(response.ok() && response.json().path("items").size() > 0,
                "Khach o ban " + tableA + " doc duoc gio hang cua ban " + tableB + ": " + response.body());
    }

    @Test(description = "Loi 5c: ma QR cua ban moi phai la chuoi ngau nhien, khong doan duoc tu so ban")
    public void newTableQrCodeIsNotGuessable() {
        JsonNode table = createTableJson();
        String qr = table.path("qrCodeUrl").asText();
        int tableId = table.path("id").asInt();

        Assert.assertFalse(qr.contains("tableId=" + tableId) || qr.endsWith("=" + tableId),
                "Ma QR cua ban " + tableId + " chi la so ban, ai cung doan duoc: " + qr);
    }

    // ---------- Loi 6: mat khau luu dang ro ----------

    @Test(description = "Loi 6: mat khau nhan vien khong duoc luu nguyen van trong database")
    public void passwordsAreNotStoredInPlainText() {
        String username = setting("staffUser", "SMARTDINING_STAFF_USER", "staff1");
        String rawPassword = setting("staffPassword", "SMARTDINING_STAFF_PASSWORD", "Staff@123");

        String stored = (String) TestDatabase.scalar("SELECT Password FROM Employees WHERE Username = ?", username);

        Assert.assertNotNull(stored, "Khong tim thay tai khoan " + username);
        Assert.assertNotEquals(stored, rawPassword,
                "Cot Employees.Password cua " + username + " chua mat khau dang ro.");
    }

    // ---------- Loi 7: dang nhap khong gioi han so lan sai ----------

    @Test(description = "Loi 7: dang nhap sai lien tuc phai bi chan hoac khoa tam")
    public void repeatedFailedLoginsAreThrottled() {
        // Ten dang nhap khong ton tai de khong khoa nham tai khoan that khi loi da duoc sua
        ApiClient attacker = new ApiClient(baseUrl);
        String username = "qa-bruteforce-" + run;
        ApiClient.Response last = null;
        for (int attempt = 1; attempt <= 15; attempt++) {
            last = attacker.login(null, username, "sai-mat-khau-" + attempt);
        }

        String body = last.body().toLowerCase(Locale.ROOT);
        boolean throttled = last.status() == 429
                || body.contains("qua nhieu") || body.contains("quá nhiều")
                || body.contains("tam khoa") || body.contains("tạm khóa")
                || body.contains("thu lai sau") || body.contains("thử lại sau");
        Assert.assertTrue(throttled, "Sau 15 lan dang nhap sai lien tiep he thong van xu ly binh thuong (HTTP "
                + last.status() + "), khong gioi han hay khoa tam.");
    }

    // ---------- Ho tro ----------

    private ApiClient loggedIn(String area, String username, String password) {
        ApiClient client = new ApiClient(baseUrl);
        ApiClient.Response response = client.login(area, username, password);
        Assert.assertEquals(response.status(), 302, "Dang nhap " + username + " that bai: " + response.body());
        return client;
    }

    private int createTable() {
        return createTableJson().path("id").asInt();
    }

    private JsonNode createTableJson() {
        String name = TestDatabase.PREFIX + run + "-" + (++tableCounter);
        ApiClient.Response response = admin.postJson("/api/admin/tables", ApiClient.body("tableName", name));
        assertOk(response, "Tao ban " + name);
        return response.json().path("data");
    }

    /** Khach quet QR cua ban (giong trang /menu chuyen toi), session nho so ban. */
    private ApiClient customerAt(int tableId) {
        ApiClient customer = new ApiClient(baseUrl);
        assertOk(customer.get("/home/index?tableId=" + tableId), "Mo thuc don ban " + tableId);
        return customer;
    }

    private String orderApi(String action, int tableId) {
        return "/api/order/" + action + "?tableId=" + tableId;
    }

    /** Dua ban toi luc khach yeu cau thanh toan va moi mon da phuc vu. Tra ve ma hoa don. */
    private String prepareTableReadyToPay(int tableId) {
        ApiClient customer = customerAt(tableId);
        assertOk(customer.postJson(orderApi("add-cart", tableId),
                ApiClient.body("productId", productId, "quantity", 1, "note", "")), "Them mon");
        assertOk(customer.postJson(orderApi("confirm", tableId), "{}"), "Gui bep");
        for (JsonNode item : customer.get(orderApi("get-cart", tableId)).json().path("items")) {
            String id = item.path("id").asText();
            assertOk(kitchen.postJson("/api/kitchen/update-status",
                    ApiClient.body("orderDetailId", id, "status", "PREPARING")), "Bep nhan lam");
            assertOk(kitchen.postJson("/api/kitchen/update-status",
                    ApiClient.body("orderDetailId", id, "status", "READY")), "Bep lam xong");
            assertOk(staff.postJson("/api/staff/serve", ApiClient.body("orderDetailId", id)), "Mang mon ra");
        }
        assertOk(customer.postJson(orderApi("request-payment", tableId), "{}"), "Yeu cau thanh toan");
        return (String) TestDatabase.scalar(
                "SELECT ID FROM Orders WHERE Table_ID = ? AND Status = 'UNPAID'", tableId);
    }

    /** Chay n request cung mot thoi diem (cho tat ca san sang roi moi bat dau). */
    private List<ApiClient.Response> concurrently(int n, IntFunction<ApiClient.Response> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ApiClient.Response>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return call.apply(index);
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<ApiClient.Response> responses = new ArrayList<>();
            for (Future<ApiClient.Response> future : futures) {
                responses.add(future.get(60, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    private WebSocket.Listener collector(ConcurrentLinkedQueue<String> messages) {
        return new WebSocket.Listener() {
            private final StringBuilder partial = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                partial.append(data);
                if (last) {
                    messages.add(partial.toString());
                    partial.setLength(0);
                }
                webSocket.request(1);
                return null;
            }
        };
    }

    private long countEvents(ConcurrentLinkedQueue<String> messages, String status) {
        return messages.stream().filter(message -> message.contains("\"status\":\"" + status + "\"")).count();
    }

    private void assertOk(ApiClient.Response response, String step) {
        Assert.assertTrue(response.ok(), step + " that bai (HTTP " + response.status() + "): " + response.body());
    }

    private String setting(String systemProperty, String environmentVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
