## Danh sách lỗi đã phát hiện (Defect log)

Các lỗi dưới đây được phát hiện trong quá trình rà soát và kiểm thử ngày 12/10/2026, đã sửa và kiểm thử lại (retest) đạt.

| Mã lỗi | Mức độ | Module | Mô tả | Bước tái hiện | Kết quả mong đợi | Kết quả thực tế (trước khi sửa) | Nguyên nhân | Trạng thái |
|---|---|---|---|---|---|---|---|---|
| BUG-01 | Cao | Quản trị / Bàn | Không ngưng sử dụng được bàn | Admin → Bàn → Ngưng sử dụng một bàn trống | Bàn chuyển sang "Ngưng sử dụng" | Báo "Chỉ được ngưng sử dụng bàn khi không có hóa đơn đang mở" dù bàn trống | Ràng buộc CHECK của cột `Tables.Status` trong DB không có giá trị `INACTIVE` | Đã sửa (TC-AD-11..13 PASS) |
| BUG-02 | Cao | Thanh toán | Đọc bảng Payments bị lỗi | Truy vấn entity `Payment` | Đọc được dữ liệu | `NumberFormatException: For input string: "COMPLETED"` | Thiếu `@Enumerated(EnumType.STRING)` ở `Payment.status` | Đã sửa |
| BUG-03 | Trung bình | Điều hướng | Trang gốc hiển thị thực đơn rỗng | Mở `http://localhost:8081/SmartDining/` | Chuyển tới trang đăng nhập / hướng dẫn quét QR | Hiện trang menu trống "Hiện chưa có món ăn nào" | Không có xử lý cho đường dẫn gốc, Tomcat mở thẳng `index.jsp` | Đã sửa (TC-AUTH-01..03 PASS) |
| BUG-04 | Trung bình | Cấu hình | Lỗi cấu hình `pom.xml` | Mở `pom.xml` trong IntelliJ | Không có lỗi | 3 lỗi đỏ "URI is not registered" | Namespace Maven viết `https://` thay vì `http://` | Đã sửa |
| BUG-05 | Thấp | Đóng gói | WAR chứa thư viện trùng với Tomcat | Build WAR, xem `WEB-INF/lib` | Không chứa `servlet-api`, `el-api` | Có `jakarta.servlet-api-6.0.0.jar`, `jakarta.el-api-5.0.0.jar` | Phụ thuộc bắc cầu từ `jstl-api` | Đã sửa |
| BUG-06 | Trung bình | Quản trị | Trang admin không tự cập nhật | Mở trang admin, ở thiết bị khác khách đặt món / nhân viên thanh toán | Doanh thu, trạng thái bàn tự cập nhật | Phải bấm làm mới (F5) mới thấy số liệu mới | Trang admin không có kết nối WebSocket | Đã sửa (TC-RT-01..14 PASS) |
| BUG-07 | Trung bình | Báo cáo | Doanh thu tính sai ngày | Mở hóa đơn 23:30 hôm trước, thanh toán 00:30 hôm sau | Doanh thu tính vào ngày thanh toán | Tính vào ngày mở hóa đơn | Báo cáo lọc theo `Orders.StartTime`; khi thanh toán không ghi bảng `Payments` | Đã sửa: ghi `Payments`, tính theo `PayTime` (TC-RT-12 PASS) |
| BUG-09 | Thấp | Khách hàng | Lọc danh mục làm vỡ bố cục thẻ món | Ở trang gọi món, bấm một danh mục rồi bấm "Tất cả" | Thẻ món giữ bố cục cũ | Thẻ món bị đổi sang `display: block`, ảnh và nội dung xếp sai | JS gán cứng `card.style.display = 'block'` | Đã sửa: gán `''` để giữ bố cục của CSS |
| BUG-10 | Thấp | Quản trị | Thẻ "Chờ thanh toán" luôn tô đỏ | Mở Tổng quan khi không có bàn nào chờ thanh toán | Thẻ ở trạng thái bình thường | Thẻ đỏ với số 0, gây báo động giả | CSS tô màu cố định | Đã sửa: chỉ tô đỏ khi số bàn chờ > 0 |
| BUG-08 | Thấp | Bếp | Danh sách bàn ở màn hình bếp không cập nhật, tên bàn có dấu `"` làm hỏng trang | Admin thêm / đổi tên bàn khi màn hình bếp đang mở | Màn hình bếp hiện bàn mới | Phải tải lại trang; tên bàn chứa `"` gây lỗi JavaScript | Danh sách bàn được chèn thẳng vào JS lúc render JSP | Đã sửa: tải qua `/api/kitchen/tables`, nghe `TABLES_UPDATED` (TC-RT-04 PASS) |

## Chức năng mới được kiểm thử

- **Xác thực email khi tạo tài khoản nhân viên** (TC-NV-01..16): admin tạo tài khoản → trạng thái `PENDING` → lần đầu đăng nhập phải nhập mã 6 số gửi qua email (hiệu lực 15 phút, tối đa 5 lần nhập sai, gửi lại mã sau 60 giây).
- Khi chưa cấu hình Gmail hoặc gửi mail thất bại, mã được in ra log Tomcat với dòng `DEV - ma xac thuc cua <username> la: <mã>` để vẫn kiểm thử được.
- **Real-time trang admin** (TC-RT-01..14): kênh WebSocket `/ws/admin` chỉ cho ADMIN; trang admin tự tải lại Tổng quan, Bàn, Thực đơn, Nhân sự, Báo cáo khi có sự kiện; hiện thông báo khi khách yêu cầu phục vụ / thanh toán; huy hiệu "Trực tiếp / Mất kết nối" và tự kết nối lại.

## Kiểm thử thủ công đã thực hiện trên trình duyệt

| Mã | Bước | Kết quả |
|---|---|---|
| TM-01 | Đăng nhập admin, quan sát góc trên phải | Hiện huy hiệu "Trực tiếp" màu xanh — PASS |
| TM-02 | Mở tab "Bàn và QR"; ở phiên khác đổi tên một bàn | Tên bàn mới hiện ngay, không tải lại trang — PASS |
| TM-03 | Phát sự kiện yêu cầu phục vụ | Hiện thông báo "Bàn 03 yêu cầu: Thêm khăn giấy (2 gói)." — PASS |
| TM-04 | Mở màn hình bếp | Danh sách bàn tải qua API, không hiện bàn đã ngưng — PASS |
| TM-05 | Giao diện tông xanh xô thơm: mở trang đăng nhập | Nền trắng ngà, thẻ trắng, khung hướng dẫn xanh nhạt, nút xanh bo tròn — PASS |
| TM-06 | Trang gọi món trên điện thoại (375px) | Header phủ xanh nhạt, nút và danh mục nền trắng nổi rõ; thẻ món dạng hàng ngang, giá màu xanh đậm; thanh giỏ hàng xanh bo tròn ở đáy — PASS |
| TM-07 | Thêm món rồi mở giỏ hàng | Món nằm trong nhóm "Chưa gửi bếp", có nút "Hủy món"; 3 nút Gửi bếp / Gọi nhân viên / Thanh toán — PASS |
| TM-08 | Màn hình bếp | Nền rêu tối, nút "Nhận làm" xanh và "Đã xong" vàng, đồng hồ góc trên, mỗi món hiện số phút chờ; viền xanh dưới 10 phút, vàng 10–20 phút, đỏ trên 20 phút — PASS |
| TM-09 | Màn hình phục vụ | Thẻ bàn có dòng tóm tắt; viền đỏ khi chờ thanh toán, viền xanh khi có món xong, nét đứt khi trống — PASS |
| TM-10 | Trang admin | Thanh bên màu rêu đậm, thẻ doanh thu nền xanh, "Chờ thanh toán" chỉ tô đỏ khi có bàn chờ — PASS |
| TM-11 | Trang xác thực email | 6 ô nhập mã, con trỏ ở ô đầu; đếm ngược "Mã còn hiệu lực 14:41" và "Gửi lại mã sau 39 giây" (nút bị khóa); dán " 501 797 " thì tự điền 6 ô, tự gửi và vào trang phục vụ — PASS |

## Chưa kiểm thử tự động (cần kiểm thử thủ công)

- Mã xác thực hết hạn sau 15 phút (cần chờ thời gian thật).
- Gửi email thật qua Gmail (cần App Password đúng trong `config.properties`).
- Mất mạng rồi có mạng lại: trang admin tự kết nối lại và tải lại toàn bộ dữ liệu.
- Giao diện trên điện thoại.

## Thay đổi giao diện

- Ngày 12/10/2026 chuyển toàn bộ giao diện sang tông **trắng ngà và xanh xô thơm** (bảng màu trong `src/main/webapp/css/theme.css`).
- Lỗi phát hiện khi kiểm thử giao diện và đã sửa: trên trang khách, nút chuông / giỏ hàng và danh mục chưa chọn trùng màu với nền header xanh nhạt nên gần như không thấy → đổi sang nền trắng.

## Chức năng mới (đợt 3) và kiểm thử

- **Khách hàng — đổi số lượng món trong giỏ** (TC-KH-18..22): nút −/+ cho món chưa gửi bếp (1–100); món đã gửi bếp không đổi được.
- **Phục vụ — thanh toán** (TC-TT-01..08, TC-PV-06): nút "Thanh toán" mở hóa đơn chi tiết (món, SL, đơn giá, thành tiền, tổng), chọn Tiền mặt / Chuyển khoản. Chỉ thanh toán được khi khách đã yêu cầu **và** mọi món đã phục vụ. Chuyển khoản bắt buộc ảnh bill (nén còn ~30–200KB, lưu cột `Payments.Image`). Nhân viên phụ trách lưu vào `Orders.Employee_ID`.
- **Admin — Tổng quan** (TC-AD-17, TC-AD-18, TC-TT-08): bấm vào ô chỉ số / dòng "Vận hành hiện tại" để xem chi tiết; hóa đơn chuyển khoản có nút "Xem bill".
- **Admin — Thực đơn**: ô tìm món theo tên, không phân biệt dấu và hoa thường.
- **Admin — Bàn và QR** (TC-AD-19): trạng thái "Ngưng hoạt động" (bảo trì / không sử dụng) chọn được trong hộp "Sửa bàn"; nút cuối dòng có chữ.

| Mã | Bước | Kết quả |
|---|---|---|
| TM-12 | Giỏ hàng: bấm "+" ở món chưa gửi bếp | Số lượng 1 → 2, thành tiền cập nhật 30.000đ → 60.000đ; nút "−" khóa khi số lượng = 1 — PASS |
| TM-13 | Phục vụ: mở hóa đơn, chọn Chuyển khoản, chọn ảnh 900×1400 | Ảnh xem trước hiện, ảnh nén còn 36KB; nút "Hoàn tất thanh toán 465.000 đ" và tên nhân viên phụ trách hiện ở cuối — PASS |
| TM-14 | Admin: bấm ô "Hóa đơn đã chốt" | Hộp chi tiết liệt kê giờ, bàn, nhân viên, phương thức, số tiền và tổng — PASS |
| TM-15 | Admin: tìm "ga nuong", "bia", "zzz" | Ra "Gà Nướng Muối Ớt"; 2 món bia; "Không tìm thấy món nào..." — PASS |
| TM-16 | Admin: Sửa bàn | Có ô Trạng thái (Đang hoạt động / Ngưng hoạt động); nút "Ngưng hoạt động" khóa khi bàn có khách — PASS |

Lỗi phát hiện và đã sửa trong đợt này: ảnh xem trước bill bị hiện dạng ảnh hỏng khi chưa chọn ảnh (CSS `display:block` ghi đè thuộc tính `hidden`); số liệu ở "Vận hành hiện tại" bị lệch ra giữa dòng.

## Chức năng mới (đợt 4) và kiểm thử

- **Bán mang về** (TC-MV-01..13): tab "Mang về" ở trang phục vụ — chọn món, tên/SĐT/giờ lấy (không bắt buộc), **thanh toán trước** (Tiền mặt / Chuyển khoản kèm ảnh bill) rồi món xuống bếp với nhãn "Mang về #N" (mã đếm lại từ 1 mỗi ngày). Bếp làm xong → đơn chuyển "Chờ giao" → nhân viên bấm "Đã giao". Bếp báo hết món thì trang phục vụ báo số tiền cần hoàn cho khách. DB: `Orders.OrderType` (DINE_IN / TAKEAWAY), `Table_ID = NULL` với đơn mang về, `CustomerName`, `CustomerPhone`, `PickupCode`, `PickupTime` (script: `database/migrations/2026-10-12_takeaway_staff-activity.sql`).
- **Bếp trên điện thoại**: ẩn cột bàn, dùng thanh chọn nhóm cuộn ngang (bàn / đơn mang về), mặc định xem tất cả món, món chờ lâu nhất lên đầu, tab Tất cả / Chờ làm / Đang nấu.
- **Báo cáo hoạt động nhân viên** (TC-BC-01..04): hóa đơn, tiền thu, tỉ lệ tiền mặt / chuyển khoản, yêu cầu đã xử lý, thời gian phản hồi TB, món mang ra, món bếp làm, thời gian nấu TB, số lần báo hết món; xuất CSV. DB: `Order_Details.PreparedBy`, `StartedTime`, `ReadyTime`, `ServedBy`, `ServedTime`. Số liệu bếp và món mang ra chỉ có từ 12/10/2026.
- **Thực đơn admin** (TC-AD-20): ảnh 4:3 có giá trên ảnh, nhãn "Bán chạy" (3 món bán nhiều nhất), "đã bán N phần", công tắc Đang bán / Tạm hết ngay trên thẻ, chế độ Lưới / Danh sách, sắp xếp theo tên, giá, bán chạy.

| Mã | Bước | Kết quả |
|---|---|---|
| TM-17 | Bếp trên điện thoại (375px) với 1 bàn + 1 đơn mang về | Cột bàn ẩn; thanh "Tất cả 4 · Bàn 03 2 · Mang về #1 2"; tab Chờ làm / Đang nấu; thẻ món ghi "Mang về #1" — PASS |
| TM-18 | Phục vụ → tab Mang về | Lưới 21 món có tìm kiếm và lọc danh mục; giỏ có tên/SĐT/giờ lấy, nút "Thanh toán 20.000 đ & gửi bếp"; danh sách "Đơn mang về hôm nay" có mã #1, trạng thái "Bếp đang làm" — PASS |
| TM-19 | Admin → Thực đơn, sắp xếp "Bán chạy nhất" | 3 món đầu có nhãn "Bán chạy", mỗi thẻ có công tắc Đang bán; chuyển sang Danh sách hiển thị mỗi món một hàng — PASS |
| TM-20 | Admin → Báo cáo → Hoạt động nhân viên | Bảng theo nhân viên, thời gian dài hiện theo giờ ("16 giờ"), nút Xuất CSV — PASS |

**Theo dõi:** TC-NV-16 (khóa tài khoản thì bị đăng xuất) FAIL 1 lần ngay sau khi redeploy, sau đó PASS 4 lần liên tiếp; chưa tái hiện được. Bộ test đã ghi thêm phản hồi của lệnh khóa tài khoản để có thông tin nếu lỗi lặp lại.
