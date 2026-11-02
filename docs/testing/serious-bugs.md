# Kiểm chứng các lỗi nghiêm trọng (TestNG)

Bộ test `src/test/java/com/smartdining/bugs/SeriousBugTest.java` kiểm chứng 7 lỗi nghiêm trọng tìm được khi đọc code.
Mỗi test mô tả **hành vi đúng**, nên khi lỗi còn tồn tại thì test **FAIL**; sau khi sửa lỗi, test phải chuyển sang PASS.
Bộ test **chưa sửa code ứng dụng**.

## Cách chạy

Cần Tomcat chạy SmartDining ở `http://localhost:8081/SmartDining` và SQL Server (đọc `db.*` trong `META-INF/config.properties`).

```bash
mvn test -Dtestng.suite=testng-bugs.xml
```

`mvn test` không kèm tham số vẫn chỉ chạy bộ Selenium (`testng.xml`).

Dữ liệu test: các bàn tên `QA BUG ...` kèm hóa đơn, thanh toán, yêu cầu của bàn đó. Chúng được xóa trước và sau khi chạy (thêm vào đó, `cleanup-test-data.sql` cũng xóa được vì tên bắt đầu bằng `QA `).

## Kết quả ngày 26/10/2026: 9 test, 9 FAIL (đúng như dự đoán, chạy 2 lần cho cùng kết quả)

| Lỗi | Test | Kết quả thực tế |
|---|---|---|
| 1. Thêm món cùng lúc tạo nhiều hóa đơn | `concurrentAddToCartCreatesSingleOrder` | 10 request song song trên bàn trống → **7–9 hóa đơn UNPAID** (mong đợi 1) |
| 2. Gửi bếp cùng lúc cộng tiền nhiều lần | `concurrentConfirmCountsTotalOnce` | **10/10** lần "Gửi bếp" được chấp nhận (mong đợi 1) |
| 3. Thanh toán cùng lúc tạo nhiều Payment | `concurrentCheckoutCreatesSinglePayment` | staff + admin bấm thanh toán song song → **10 dòng Payments** cho một hóa đơn |
| 4. WebSocket ngừng nhận sự kiện | `staffWebSocketReceivesEveryEventUnderBurst` | 20 yêu cầu phục vụ liên tiếp → màn hình phục vụ chỉ nhận **1–2 sự kiện**, sau đó không nhận gì nữa |
| 5a. Gọi món không cần quét QR | `orderApiRejectsTableIdWithoutQrScan` | Không có session vẫn thêm món được bằng `?tableId=` (HTTP 200) |
| 5b. Xem giỏ hàng bàn khác | `customerCannotReadAnotherTablesCart` | Khách bàn A đọc được giỏ hàng bàn B |
| 5c. Mã QR đoán được | `newTableQrCodeIsNotGuessable` | Bàn mới có QR `/home/index?tableId=<số bàn>` |
| 6. Mật khẩu lưu dạng rõ | `passwordsAreNotStoredInPlainText` | `Employees.Password` của staff1 = `Staff@123` |
| 7. Không giới hạn đăng nhập sai | `repeatedFailedLoginsAreThrottled` | 15 lần sai liên tiếp vẫn xử lý bình thường (HTTP 200) |

## Lưu ý

- Test 1–4 là lỗi tranh chấp khi nhiều request chạy cùng lúc: lần nào chạy cũng fail nhưng con số cụ thể (7, 9 hóa đơn; 1, 2 sự kiện) thay đổi.
- Test 7 dùng tên đăng nhập không tồn tại (`qa-bruteforce-...`) để khi lỗi đã được sửa thì không khóa nhầm tài khoản thật.
- Test 6 so sánh với mật khẩu mẫu của `staff1`; đổi bằng `-DstaffUser=... -DstaffPassword=...` nếu dùng tài khoản khác.
- Các test khác đi qua bước "khách quét QR" (`/home/index?tableId=`) trước khi gọi API, nên vẫn dùng được sau khi sửa lỗi 5 nếu luồng quét QR vẫn tạo session cho bàn.
