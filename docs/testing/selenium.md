# Kiểm thử giao diện bằng Selenium + TestNG

Bộ test ở `src/test/java/com/smartdining/selenium`, danh sách lớp chạy khai báo trong `testng.xml`.

## Phạm vi (15 test)

**Đăng nhập và phân quyền** (`LoginSeleniumTest`)

| Test | Kiểm tra |
|---|---|
| rootRedirectsToLoginWithQrNotice | `/` chuyển về `/login`, có hướng dẫn khách quét QR |
| wrongPasswordShowsError | Sai mật khẩu → báo lỗi, ở lại `/login` |
| adminLoginOpensAdminDashboard | admin → `/admin/dashboard`, tab Tổng quan |
| staffLoginOpensStaffDashboard | staff1 → `/staff/dashboard`, tải được danh sách bàn |
| kitchenLoginOpensKitchenDashboard | kitchen1 → `/kitchen/dashboard` |
| staffCannotLoginAtKitchenGate | staff1 đăng nhập ở `/login/kitchen` bị từ chối |
| staffCannotOpenAdminDashboard | staff1 mở `/admin/dashboard` bị chuyển về `/login` |
| guestIsRedirectedFromWorkspaces | Chưa đăng nhập không vào được trang phục vụ / bếp / admin |
| logoutEndsSession | Đăng xuất rồi mở lại trang phục vụ bị chuyển về `/login` |

**Khách gọi món qua QR** (`CustomerMenuSeleniumTest`). Không bấm "Thêm vào giỏ" nên không tạo đơn trong DB.

| Test | Kiểm tra |
|---|---|
| menuShowsTableNumberAndProducts | `/menu?tableId=1` hiện đúng số bàn và có món |
| invalidTableIsRejected | `tableId` = `abc`, `-1`, `999999` không mở thực đơn |
| categoryFilterShowsMatchingProducts | Lọc danh mục đúng món; "Tất cả" hiện lại đủ; không tái lỗi BUG-09 |
| productModalUpdatesQuantityAndPrice | Hộp chọn món: số lượng không dưới 1, thành tiền = đơn giá × số lượng |
| cartSidebarOpensAndCloses | Mở / đóng giỏ hàng của bàn |
| menuFitsMobileWidth | Màn hình 375px không tràn ngang, thanh giỏ hàng vẫn hiện |

## Cách chạy

1. Bật SQL Server (DB đã chạy `database/SmartDining.sql`) và Tomcat với ứng dụng ở `http://localhost:8081/SmartDining`.
2. Chạy:

```bash
mvn test
```

Tuỳ chọn (tham số `-D` hoặc biến môi trường):

| Tham số | Biến môi trường | Mặc định |
|---|---|---|
| `-DbaseUrl` | `SMARTDINING_BASE_URL` | `http://localhost:8081/SmartDining` |
| `-Dbrowser` | `SMARTDINING_TEST_BROWSER` | `chrome` (hoặc `edge`) |
| `-Dheadless` | `SMARTDINING_TEST_HEADLESS` | `true` (`false` để xem trình duyệt chạy) |
| `-DtableId` | `SMARTDINING_TEST_TABLE_ID` | `1` |
| `-DadminUser`, `-DadminPassword`, `-DstaffUser`, ... | `SMARTDINING_ADMIN_USER`, ... | tài khoản mẫu trong `SmartDining.sql` |

Ví dụ xem trình duyệt Edge chạy: `mvn test -Dbrowser=edge -Dheadless=false`

## Kết quả

- Báo cáo: `target/surefire-reports` (mở `index.html`).
- Ảnh chụp khi test fail: `target/selenium-screenshots`.
- Lần chạy ngày 26/10/2026 (Tomcat 10.1.20, JDK 25, Chrome headless): **15/15 PASS**, khoảng 20 giây.

## Lưu ý

- Mật khẩu mẫu (`Staff@123`...) nằm trong danh sách mật khẩu bị lộ, nên Chrome hiện hộp cảnh báo sau khi đăng nhập. Hộp này chiếm bàn phím làm các test sau gõ không vào form. `SeleniumTestBase` đã tắt trình quản lý mật khẩu của trình duyệt để tránh lỗi này.
- Sau khi đổi package (vd. `servlet` → `api`) cần `mvn clean` trước khi deploy, nếu không `target/classes` còn class cũ khiến hai servlet trùng URL và ứng dụng không khởi động.
