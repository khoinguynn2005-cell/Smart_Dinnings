# SmartDining

Hệ thống gọi món tại bàn bằng mã QR cho nhà hàng. Khách quét QR trên bàn để xem thực đơn, gọi món, gửi bếp và yêu cầu thanh toán. Nhân viên phục vụ, nhân viên bếp và quản trị viên theo dõi đơn hàng theo thời gian thực qua WebSocket. Ngoài ra hệ thống còn có luồng mang về (takeaway).

Dự án là ứng dụng **Java Web (WAR)** chạy trên **Apache Tomcat 11**, dùng **SQL Server** làm cơ sở dữ liệu.

---

## 1. Công nghệ

| Thành phần | Phiên bản / ghi chú |
|---|---|
| JDK | Build với `release 21`, đã kiểm tra chạy trên **JDK 27** |
| Web container | **Apache Tomcat 11.0.x** (Jakarta Servlet 6, WebSocket 2.1) |
| ORM | Hibernate ORM 6.4.4 (JPA `jakarta.persistence` 3.1) |
| CSDL | Microsoft SQL Server (Express), driver `mssql-jdbc` 13.6 |
| JSON | Jackson 2.22 |
| Email | Angus Mail 2.0 (gửi mã xác thực khi tạo tài khoản) |
| Kiểm thử | TestNG 7.10, Selenium 4.27, và một bộ kiểm thử API bằng Node.js |
| Build | Maven (`pom.xml`), packaging `war`, tên file `SmartDining.war` |

Dự án **không dùng Lombok** (đã gỡ để tương thích JDK 27). Getter, setter và constructor được viết tay trong các entity.

---

## 2. Cấu trúc thư mục

```
SmartDining/
├── pom.xml                      # Maven: dependency, plugin, build WAR
├── testng.xml                   # Suite Selenium (giao diện)
├── testng-bugs.xml              # Suite kiểm chứng các lỗi nghiêm trọng (API + DB)
├── database/
│   ├── SmartDining.sql          # Tạo bảng + dữ liệu mẫu
│   └── migrations/              # Script nâng cấp, chạy theo thứ tự ngày
├── docs/
│   ├── diagrams/                # Sơ đồ hoạt động (đăng nhập, quản trị, khách)
│   └── testing/                 # Tài liệu kiểm thử, báo cáo, script API, script dọn dữ liệu
└── src/
    ├── main/java/
    │   ├── META-INF/            # persistence.xml, config.properties (KHÔNG commit)
    │   └── com/smartdining/
    │       ├── api/             # Servlet trả JSON: AdminApi, StaffApi, KitchenApi, CustomerOrder
    │       ├── servlet/         # Servlet trả trang JSP / chuyển hướng: Login, Home, Menu, Staff, Kitchen, Admin, Logout
    │       ├── service/         # Interface nghiệp vụ
    │       │   └── impl/        # Cài đặt nghiệp vụ, quản lý transaction JPA
    │       ├── dao/             # Interface truy cập dữ liệu
    │       │   └── impl/
    │       ├── entity/          # Entity JPA (ánh xạ bảng)
    │       ├── enums/           # Trạng thái: EmployeeRole, TableStatus, OrderDetailStatus, ...
    │       ├── filter/          # AAAFilter: kiểm tra quyền theo đường dẫn
    │       ├── websocket/       # Endpoint /ws/* và helper gửi tin
    │       └── utils/           # JpaUtil, ConfigUtil, SecurityUtil, PasswordUtil, XMail, BugLog, ...
    ├── main/webapp/
    │   ├── index.jsp            # Trang khách (thực đơn, giỏ hàng)
    │   ├── staff.jsp            # Màn hình phục vụ
    │   ├── kitchen.jsp          # Màn hình bếp
    │   ├── admin.jsp            # Trang quản trị
    │   ├── login.jsp, verify.jsp
    │   ├── css/, js/main.js, js/admin.js
    │   └── META-INF/
    └── test/java/com/smartdining/
        ├── bugs/                # SeriousBugTest, ApiClient, TestDatabase
        └── selenium/            # LoginSeleniumTest, CustomerMenuSeleniumTest, SeleniumTestBase
```

---

## 3. Vai trò và luồng nghiệp vụ

Có 3 vai trò nhân viên (`EmployeeRole`) và khách:

| Vai trò | Đăng nhập | Làm gì |
|---|---|---|
| Khách | Không cần đăng nhập, quét QR | Xem thực đơn, thêm món vào giỏ, gửi bếp, gọi nhân viên, yêu cầu thanh toán |
| Nhân viên phục vụ (`STAFF`) | `/login/staff` | Xem bàn, bàn có khách, phục vụ món đã xong, thu tiền, xử lý yêu cầu, đơn mang về |
| Nhân viên bếp (`KITCHEN`) | `/login/kitchen` | Nhận món mới, đổi trạng thái (đang làm, xong), hủy món |
| Quản trị (`ADMIN`) | `/login/admin` | Quản lý bàn, danh mục, món ăn, nhân viên, xem báo cáo, xem hoạt động nhân viên |

Trạng thái nhân viên (`EmployeeStatus`): `PENDING` (chờ xác thực email), `ACTIVE`, `INACTIVE` (bị khóa).

### Luồng khách gọi món

1. Khách quét QR của bàn. QR chứa đường dẫn `/home/index?qr=<token>`. Token là chuỗi ngẫu nhiên 192 bit, lưu trong cột `Tables.QRCodeURL`.
2. `HomeServlet` tìm bàn theo token, lưu `currentTableId` vào **session** và hiển thị `index.jsp`.
3. Mọi thao tác giỏ hàng gọi `/api/order/*` (`CustomerOrderServlet`). Bàn luôn lấy từ session, không nhận từ tham số URL.
4. Thêm món vào giỏ → trạng thái món là `CART`. Khi bấm "Gửi bếp" (`/api/order/confirm`) → món chuyển sang `PENDING`, cộng tiền vào hóa đơn và gửi sự kiện tới bếp.
5. Bếp đổi trạng thái món (`PENDING` → `PREPARING` → `READY`), phục vụ xác nhận món đã ra (`SERVED`).
6. Khách bấm "Thanh toán" → `requestPayment`, bàn chuyển sang `WAITING_PAYMENT`, nhân viên nhận thông báo.
7. Nhân viên thu tiền (`/api/staff/checkout`) → tạo bản ghi `Payments`, hóa đơn chuyển `PAID`, bàn về `AVAILABLE`.

### Trạng thái chính

- **Bàn** (`TableStatus`): `AVAILABLE` → `FULL` (đã có khách) → `WAITING_PAYMENT` → `AVAILABLE`. `INACTIVE` là bàn ngưng dùng, không nhận khách.
- **Hóa đơn** (`OrderStatus`): `UNPAID` → `PAID` (hoặc `CANCELLED`).
- **Món** (`OrderDetailStatus`): `CART` → `PENDING` → `PREPARING` → `READY` → `SERVED` (hoặc `CANCELLED`).
- **Loại đơn** (`OrderType`): `DINE_IN` (tại bàn) và `TAKEAWAY` (mang về, không gắn bàn, có mã lấy đồ `PickupCode`).

---

## 4. Kiến trúc và cách hoạt động

```
Trình duyệt ──HTTP──▶ Servlet (api/ hoặc servlet/) ──▶ Service (service/impl) ──▶ DAO / EntityManager ──▶ SQL Server
     ▲                         │                                 │
     └────── WebSocket ◀── websocket/*  ◀──── broadcast sau mỗi thay đổi ───┘
```

- **Servlet** nhận request, kiểm tra tham số, gọi service, trả JSON (Jackson) hoặc forward sang JSP.
- **Service** chứa nghiệp vụ và quản lý transaction. Mỗi thao tác ghi tự mở `EntityManager` từ `JpaUtil`, `begin`/`commit`/`rollback`, rồi đóng.
- **WebSocket**: các endpoint `/ws/customer`, `/ws/staff`, `/ws/kitchen`, `/ws/admin`. Sau khi ghi DB thành công, service gọi `broadcast` để các màn hình cập nhật. Việc gửi tin được gom vào `WebSocketBroadcast`.
- **AAAFilter** (áp dụng cho `/*`) kiểm tra vai trò theo đường dẫn:
  - `/admin/*`, `/employee/*`, `/api/admin/*`: chỉ `ADMIN`.
  - `/staff/*`, `/api/staff/*`: `STAFF` hoặc `ADMIN`.
  - `/kitchen/*`, `/api/kitchen/*`: `KITCHEN` hoặc `ADMIN`.

  Không đủ quyền thì trả 403 cho API hoặc chuyển về trang đăng nhập.
- **Đăng nhập nhân viên**: `SecurityUtil` tạo token ngẫu nhiên, lưu trong bộ nhớ (`ConcurrentHashMap`, hết hạn sau 8 giờ) và gửi về cookie `SMARTDINING_AUTH_<ROLE>`. Mỗi vai trò có cookie riêng.
- **Mật khẩu**: `PasswordUtil` băm PBKDF2-HMAC-SHA256 (có muối). Mật khẩu cũ dạng chữ thường vẫn đăng nhập được và sẽ được băm lại sau lần đăng nhập đúng đầu tiên.
- **Khóa đăng nhập**: sai 5 lần liên tiếp với cùng tên đăng nhập thì khóa 15 phút (lưu trong bộ nhớ, `LoginServlet`).
- **Xác thực email**: tài khoản nhân viên mới ở trạng thái `PENDING`, phải nhập mã gửi qua email (`VerificationCodeUtil`, `XMail`) mới kích hoạt được.
- **Đồng thời**: các thao tác trên cùng một bàn (thêm món, gửi bếp, yêu cầu thanh toán, thu tiền, sửa giỏ) đều khóa dòng bàn bằng `LockModeType.PESSIMISTIC_WRITE` đầu transaction, tránh tạo trùng hóa đơn hoặc thu tiền hai lần.
- **Ghi lỗi**: lỗi bị nuốt trong `catch` nên ghi qua `BugLog.swallowed(...)` để còn dấu vết khi điều tra.

---

## 5. Cài đặt và chạy

### 5.1. Yêu cầu

- Windows (các đường dẫn trong tài liệu theo Windows), JDK 27 hoặc 21.
- SQL Server Express (instance `SQLEXPRESS`), bật đăng nhập bằng user `sa` (hoặc user bạn tạo).
- Apache Tomcat 11.0.x (tải từ tomcat.apache.org, giải nén, ví dụ `D:\tomcat\apache-tomcat-11.0.27`).
- IntelliJ IDEA (Ultimate hoặc Community) có plugin Maven. Với Community, cài thêm plugin **SmartTomcat**.
- Google Chrome (chỉ cần khi chạy test Selenium).
- Node.js (chỉ cần khi chạy `docs/testing/api-test-runner.mjs`).

### 5.2. Tạo database

1. Mở SSMS hoặc `sqlcmd`, chạy `database/SmartDining.sql` (tạo database `Smart_Dinings`, các bảng, dữ liệu mẫu).
2. Chạy lần lượt các file trong `database/migrations/` theo ngày trong tên file (ví dụ `2026-10-12_...` trước `2026-11-02_...`).

> Lưu ý: `2026-11-02_table_qr_tokens.sql` đổi mã QR của mọi bàn. Sau khi chạy, **phải in lại mã QR** cho các bàn, mã cũ sẽ không còn hoạt động.

### 5.3. Tạo file cấu hình (không commit)

Tạo file `src/main/java/META-INF/config.properties` (file này đã nằm trong `.gitignore`):

```properties
app.url=http://localhost:8081/SmartDining
db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
db.url=jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=Smart_Dinings;encrypt=true;trustServerCertificate=true
db.username=sa
db.password=<mật khẩu SQL Server>
mail.username=<tài khoản Gmail gửi mã xác thực>
mail.password=<App Password của Gmail>
```

- `db.*` được `JpaUtil` đọc để mở kết nối. `persistence.xml` không chứa thông tin đăng nhập.
- `mail.*` dùng để gửi mã xác thực. Nếu chưa cấu hình Gmail, mã xác thực vẫn được tạo, nhưng không gửi được email (xem phần 8).
- `app.url` được dùng trong nội dung email.

**Không bao giờ commit file này và không ghi mật khẩu vào code.**

### 5.4. Mở project trong IntelliJ

1. `File → Open…` → chọn `pom.xml` → **Open as Project**. Chờ Maven import xong.
2. `File → Project Structure → Project SDK`: chọn JDK 27 (hoặc JDK 21). Language level: 21.
3. Kiểm tra `Maven` tool window: build được `clean package -DskipTests`.

### 5.5. Cài Tomcat và tạo run configuration

**Cách A (IntelliJ Ultimate, Tomcat Server):**
1. `Run → Edit Configurations → + → Tomcat Server → Local`.
2. Tab **Server**: chọn thư mục Tomcat, JRE là JDK 27, URL `http://localhost:8081/SmartDining/login`, HTTP port `8081`.
3. Tab **Deployment**: `+ → SmartDining:war exploded`, Application context `/SmartDining`.
4. Artifact nằm trong `File → Project Structure → Artifacts`. Nếu chưa có, thêm `Web Application: Exploded → From Modules…`.

**Cách B (SmartTomcat, dùng được cả Community):**
1. `Settings → Plugins → Marketplace` → cài **SmartTomcat**, khởi động lại IDE.
2. `Run → Edit Configurations → + → Smart Tomcat`.
3. Điền: Tomcat server (thư mục Tomcat), Server port `8081`, Context path `/SmartDining`, Use classpath of module `SmartDining`.
4. Nếu báo lỗi `docBase ... does not exist` trong log, cần tạo thư mục `webapps\SmartDining` (xem mục 8).

**Cách C (không dùng IDE):**
```bat
mvn -B clean package -DskipTests
copy target\SmartDining.war D:\tomcat\apache-tomcat-11.0.27\webapps\
D:\tomcat\apache-tomcat-11.0.27\bin\catalina.bat run
```

Đổi cổng HTTP của Tomcat thành **8081** trong `conf\server.xml` (`<Connector port="8081" protocol="HTTP/1.1" ...>`) để khớp với `app.url` và bộ test.

### 5.6. Chạy

Bấm **Run** (▶) trên configuration Tomcat. Khi log có `Server startup in ...`, mở:

- Trang khách (mô phỏng quét QR): cần token của một bàn, ví dụ `http://localhost:8081/SmartDining/home/index?qr=<QRCodeURL của bàn>`. Xem token trong màn hình admin (copy link khách).
- Đăng nhập nhân viên: `http://localhost:8081/SmartDining/login/staff`, `/login/kitchen`, `/login/admin`.
- Tài khoản mẫu: xem phần chú thích đầu bảng `Employees` trong `database/SmartDining.sql` (tài khoản admin, staff1, kitchen1). Đây là mật khẩu demo, đổi trước khi dùng thật.

Sau mỗi lần sửa code, bấm **Build** rồi **Run** lại (hoặc Redeploy trong Tomcat).

---

## 6. Chạy kiểm thử

Chạy kiểm thử khi Tomcat đang chạy trên cổng 8081, nếu không test sẽ báo `Connection refused`.

| Bộ test | File | Cần gì | Lệnh |
|---|---|---|---|
| Kiểm chứng lỗi nghiêm trọng (API + DB) | `testng-bugs.xml`, `SeriousBugTest` | Tomcat đang chạy, SQL Server | `mvn test -Dtestng.suite=testng-bugs.xml` |
| Giao diện Selenium | `testng.xml` (mặc định) | Tomcat, Chrome | `mvn test` |
| API black-box | `docs/testing/api-test-runner.mjs` | Tomcat, Node.js | `node docs/testing/api-test-runner.mjs` |

Biến môi trường cho test: `SMARTDINING_BASE_URL`, `SMARTDINING_ADMIN_USER/PASSWORD`, `SMARTDINING_STAFF_USER/PASSWORD`, `SMARTDINING_KITCHEN_USER/PASSWORD`. Có thể truyền bằng `-D` (ví dụ `-DstaffPassword=...`).

Test `SeriousBugTest` tạo dữ liệu có tên bắt đầu bằng `QA BUG`, tự xóa trước và sau khi chạy. Có thể dọn thủ công bằng `docs/testing/cleanup-test-data.sql` (chỉ xóa các bản ghi tên bắt đầu bằng `QA `).

Test dùng DB thật và gửi nhiều request song song. **Không chạy trên database production.**

---

## 7. Quy ước khi sửa code

- **Thêm API mới cho nhân viên**: thêm nhánh trong `StaffApiServlet` (hoặc Admin/Kitchen). Nếu có nghiệp vụ mới, đặt trong `service/` (interface) và `service/impl/` (cài đặt). Truy cập dữ liệu qua `dao/` hoặc `EntityManager` từ `JpaUtil`.
- **Thao tác ghi trên bàn** phải khóa dòng bàn đầu transaction (xem `OrderServiceImpl.lockTable`). Không đọc hóa đơn rồi ghi mà không khóa.
- **Gửi sự kiện realtime**: sau khi `commit` thành công mới gọi `broadcast`. Không gọi trong transaction.
- **Entity**: không dùng Lombok. Viết getter/setter thủ công.
- **Thêm quyền truy cập đường dẫn mới**: cập nhật `AAAFilter.requiredRoles` (mục 4).
- **Đổi trạng thái DB**: thêm file trong `database/migrations/` với tên `yyyy-MM-dd_mo-ta.sql`, viết theo kiểu idempotent (`IF COL_LENGTH(...) IS NULL`). Không sửa `SmartDining.sql` cho thay đổi đã triển khai.
- **Ghi log lỗi**: dùng `BugLog.swallowed("mo ta", ex)`. Không dùng `System.out` trong code chạy thật.
- **Kiểu xuống dòng**: các file hiện dùng CRLF. Giữ nguyên kiểu đang có khi sửa.
- **Mật khẩu và thông tin nhạy cảm**: không ghi vào code, không commit `config.properties`, không đưa mật khẩu thật vào docs hay test.

---

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân và cách xử lý |
|---|---|
| `Connection refused` tới `localhost:8081` | Tomcat chưa chạy, hoặc cổng không phải 8081. Kiểm tra `server.xml` và configuration. |
| Tomcat báo `main resource set ... is not a directory or war file` | Smart Tomcat trỏ tới thư mục deploy chưa tồn tại. Đóng gói bằng `mvn package`, rồi copy `target\SmartDining` vào thư mục `webapps\SmartDining` của Smart Tomcat. |
| `ExceptionInInitializerError` khi build | Thường là Lombok trên JDK mới. Dự án đã gỡ Lombok, nên kiểm tra lại JDK và `pom.xml`. |
| Không kết nối được database | Kiểm tra SQL Server đang chạy (`MSSQL$SQLEXPRESS`), cổng 1433, user/mật khẩu trong `config.properties`, và `trustServerCertificate=true`. |
| Không nhận được email xác thực | Kiểm tra `mail.username`, `mail.password` (App Password của Gmail). Khi chưa cấu hình Gmail, xem mã xác thực trong log Tomcat (`catalina.out`), đúng như `api-test-runner.mjs` mô tả. |
| Quét QR xong báo "Ma QR khong hop le" | Mã QR cũ (`tableId=...`) đã bị đổi. Chạy migration `2026-11-02_table_qr_tokens.sql` và in lại QR. |
| Đăng nhập nhân viên bị báo khóa | Sai mật khẩu 5 lần, khóa 15 phút. Chờ hết thời gian, hoặc khởi động lại Tomcat (khóa lưu trong bộ nhớ). |
| Chạy ▶ trong IntelliJ mà ra TestNG | Đang chọn cấu hình TestNG, hoặc `<default>`. Chọn đúng configuration Tomcat trên thanh công cụ. |
| Sửa code nhưng trang không đổi | Chưa build lại hoặc chưa redeploy. Build rồi Run lại; với Smart Tomcat, copy lại thư mục đã build. |

---

## 9. Hạn chế đã biết

- Phiên đăng nhập nhân viên và khóa đăng nhập lưu trong bộ nhớ: khởi động lại Tomcat sẽ đăng xuất tất cả. Không dùng được khi chạy nhiều instance Tomcat.
- Khóa đăng nhập theo tên đăng nhập, người khác có thể cố tình khóa một tài khoản trong 15 phút.
- Mã QR khách nằm trong trang khách và trong URL WebSocket. Ai quét được QR của bàn đều có thể xem sự kiện của bàn đó.
- Dữ liệu mẫu trong `SmartDining.sql` có mật khẩu demo. Nếu dùng thật, phải đổi ngay.
- Một số lớp còn chứa các trường thử nghiệm và log cũ, sẽ dọn dần khi sửa.
- Chưa có CI. Test chủ yếu chạy thủ công trên máy có SQL Server và Tomcat.

---

## 10. Tài liệu thêm

- `docs/testing/serious-bugs.md`: mô tả 7 nhóm lỗi nghiêm trọng và test kiểm chứng.
- `docs/testing/defects.md`, `docs/testing/test-report.md`: danh sách lỗi và báo cáo kiểm thử.
- `docs/testing/selenium.md`: cách chạy Selenium.
- `docs/diagrams/`: sơ đồ hoạt động của đăng nhập, quản trị và khách hàng.
