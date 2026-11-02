# Báo cáo kiểm thử chức năng SmartDining

- Thời điểm chạy: 18:09:00 12/10/2026
- Môi trường: http://localhost:8081/SmartDining · Tomcat 10.1.20 · JDK 25 · SQL Server Express (DB `Smart_Dinings`)
- Phương pháp: kiểm thử hộp đen qua HTTP (gọi trang và API như trình duyệt), tự động bằng `docs/testing/api-test-runner.mjs`
- Tài khoản test: tài khoản mẫu trong `database/SmartDining.sql`
- Dữ liệu test: mọi bản ghi do test tạo có tên bắt đầu bằng `QA ` và được tự động xoá trước và sau mỗi lần chạy (`cleanup-test-data.sql`); thêm `--keep-data` để giữ lại
- Kết quả: **124 test case — 124 PASS — 0 FAIL**

## Tổng hợp theo module

| Module | Số TC | PASS | FAIL |
|---|---|---|---|
| Xác thực | 11 | 11 | 0 |
| Phân quyền | 5 | 5 | 0 |
| Real-time | 14 | 14 | 0 |
| Quản trị | 20 | 20 | 0 |
| Khách hàng | 22 | 22 | 0 |
| Bếp | 4 | 4 | 0 |
| Thanh toán | 8 | 8 | 0 |
| Phục vụ | 7 | 7 | 0 |
| Mang về | 13 | 13 | 0 |
| Báo cáo nhân viên | 4 | 4 | 0 |
| Tài khoản | 16 | 16 | 0 |

## Xác thực

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-AUTH-01 | Truy cập trang gốc chuyển về trang đăng nhập | GET / | 302 -> /SmartDining/login | HTTP 302 -> /SmartDining/login | PASS |
| TC-AUTH-02 | Truy cập thẳng index.jsp chuyển về đăng nhập | GET /index.jsp | 302 -> /SmartDining/login | HTTP 302 -> /SmartDining/login | PASS |
| TC-AUTH-03 | Trang đăng nhập hiển thị hướng dẫn cho khách và nhân viên | GET /login | 200, có "quét mã QR" và "Nhân viên" | HTTP 200 | PASS |
| TC-AUTH-04 | Đăng nhập admin đúng tài khoản | admin / (mật khẩu đúng) | 302 -> /admin/dashboard | HTTP 302 -> /SmartDining/admin/dashboard | PASS |
| TC-AUTH-05 | Đăng nhập staff đúng tài khoản | staff1 / (mật khẩu đúng) | 302 -> /staff/dashboard | HTTP 302 -> /SmartDining/staff/dashboard | PASS |
| TC-AUTH-06 | Đăng nhập kitchen đúng tài khoản | kitchen1 / (mật khẩu đúng) | 302 -> /kitchen/dashboard | HTTP 302 -> /SmartDining/kitchen/dashboard | PASS |
| TC-AUTH-07 | Đăng nhập sai mật khẩu | staff1 / sai-mat-khau | 200, báo "Sai ten dang nhap hoac mat khau" | HTTP 200 | PASS |
| TC-AUTH-08 | Đăng nhập tài khoản không tồn tại | khong-ton-tai / abc12345 | 200, báo lỗi | HTTP 200 | PASS |
| TC-AUTH-09 | Đăng nhập tài khoản đã bị khóa (INACTIVE) | staff2 / 1234 | 200, báo "Tai khoan cua ban da bi khoa" | HTTP 200 | PASS |
| TC-AUTH-10 | Đăng nhập sai khu vực (staff vào cổng bếp) | staff1 tại /login/kitchen | 200, báo "Tai khoan khong thuoc khu vuc" | HTTP 200 | PASS |
| TC-AUTH-11 | Đăng xuất rồi vào lại trang làm việc | GET /logout -> GET /staff/dashboard | 302 -> /login | HTTP 302 -> /SmartDining/login | PASS |

## Phân quyền

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-PQ-01 | Chưa đăng nhập vào trang admin | GET /admin/dashboard | 302 -> /login | HTTP 302 -> /SmartDining/login | PASS |
| TC-PQ-02 | Nhân viên phục vụ gọi API admin | staff1: GET /api/admin/overview | 403 | HTTP 403 | PASS |
| TC-PQ-03 | Nhân viên bếp gọi API phục vụ | kitchen1: GET /api/staff/tables | 403 | HTTP 403 | PASS |
| TC-PQ-04 | Khách chưa đăng nhập gọi API bếp | POST /api/kitchen/update-status | 403 | HTTP 403 | PASS |
| TC-PQ-05 | Admin được xem màn hình bếp | admin: GET /api/kitchen/tasks | 200 | HTTP 200 {"status":"success","data":[{"note":"Siêu cạy","orderType":"DINE_IN","groupName":"Bàn 03","pickupTime":null,"quantity":2,"tableId":3,"create | PASS |

## Real-time

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-RT-01 | Khách chưa đăng nhập không kết nối được kênh admin | WebSocket /ws/admin, không cookie | Server đóng kết nối | closed=true | PASS |
| TC-RT-02 | Nhân viên phục vụ không kết nối được kênh admin | WebSocket /ws/admin, cookie staff1 | Server đóng kết nối | closed=true | PASS |
| TC-RT-03 | Admin kết nối được kênh admin | WebSocket /ws/admin, cookie admin | Kết nối mở và giữ nguyên | opened=true closed=false | PASS |
| TC-RT-04 | Màn hình bếp nhận sự kiện khi admin thêm/đổi tên bàn | Admin đổi tên bàn -> WS /ws/kitchen | Nhận TABLES_UPDATED, /api/kitchen/tables có tên mới | event=true name=QA Bàn 330365 (đổi tên) | PASS |
| TC-RT-05 | Trang admin khác nhận sự kiện khi bàn thay đổi | Admin đổi tên bàn -> WS /ws/admin | Nhận TABLES_UPDATED | event=true | PASS |
| TC-RT-06 | Trang admin nhận sự kiện khi thực đơn thay đổi | Admin thêm/sửa món -> WS /ws/admin | Nhận MENU_UPDATED | event=true | PASS |
| TC-RT-07 | Trang admin nhận sự kiện khi khách gửi đơn | Bàn test 28 -> WS /ws/admin | Nhận ORDER_CONFIRMED của bàn 28 | event={"totalAmount":110000,"tableId":28,"status":"ORDER_CONFIRMED","tableName":"QA Bàn 330365 (đổi tên)", | PASS |
| TC-RT-08 | Trang admin nhận sự kiện khi khách yêu cầu phục vụ | Bàn test 28 -> WS /ws/admin | Nhận SERVICE_REQUEST của bàn 28 | event={"note":"2 ly","requestId":"REQ-8973a139-7a2c-4ebf-86e2-37fe92211a8b","tableId":28,"type":"Thêm nước | PASS |
| TC-RT-09 | Trang admin nhận sự kiện khi khách yêu cầu thanh toán | Bàn test 28 -> WS /ws/admin | Nhận PAYMENT_REQUEST của bàn 28 | event={"totalAmount":110000,"requestId":"REQ-2925a610-372d-47a5-8bcd-abf129927888","tableId":28,"status":" | PASS |
| TC-RT-10 | Trang admin nhận sự kiện khi thanh toán xong | Bàn test 28 -> WS /ws/admin | Nhận ORDER_PAID của bàn 28 | event={"status":"ORDER_PAID","tableId":28} | PASS |
| TC-RT-11 | Trang admin nhận sự kiện khi trạng thái bàn đổi | Bàn test 28 -> WS /ws/admin | Nhận TABLE_STATUS_UPDATED của bàn 28 | event={"tableId":28,"tableStatus":"FULL","status":"TABLE_STATUS_UPDATED"} | PASS |
| TC-RT-12 | Doanh thu hôm nay tăng đúng bằng hóa đơn vừa thanh toán | Tổng hóa đơn = 110000 | todayRevenue tăng 110000, số hóa đơn +1 | tăng 110000, hóa đơn 2 -> 3 | PASS |
| TC-RT-13 | Tổng quan có số yêu cầu đang chờ xử lý | GET /api/admin/overview | Có trường pendingRequests | pendingRequests=1 | PASS |
| TC-RT-14 | Trang admin nhận sự kiện khi nhân sự thay đổi | Tạo tài khoản, xác thực email, khóa tài khoản | Nhận EMPLOYEE_UPDATED (ít nhất 4 lần) | số lần nhận=4 | PASS |

## Quản trị

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-AD-01 | Thêm danh mục mới | name="QA Danh mục 330365" | 200, trả về id | HTTP 200 {"data":{"name":"QA Danh mục 330365","id":26}} | PASS |
| TC-AD-02 | Thêm danh mục trùng tên | name="QA Danh mục 330365" (lần 2) | 400 | HTTP 400 {"message":"Tên danh mục không hợp lệ hoặc đã tồn tại."} | PASS |
| TC-AD-03 | Thêm danh mục tên rỗng | name="   " | 400 | HTTP 400 {"message":"Tên danh mục không hợp lệ hoặc đã tồn tại."} | PASS |
| TC-AD-04 | Thêm món ăn hợp lệ | {"name":"QA Món 330365","categoryId":3,"price":50000,"imageUrl":"","description":"Món dùng để test","status":"AVAILABLE"} | 200, trả về id món | HTTP 200 {"data":{"id":"P042","name":"QA Món 330365","categoryId":3,"categoryName":"Đồ Uống","price":50000,"imageUrl":null,"description":"Món dùng để | PASS |
| TC-AD-05 | Thêm món ăn thiếu tên | name="" | 400 | HTTP 400 {"message":"Không thể lưu món ăn."} | PASS |
| TC-AD-06 | Thêm món ăn giá âm | price=-1000 | 400 | HTTP 400 {"message":"Không thể lưu món ăn."} | PASS |
| TC-AD-07 | Sửa giá món ăn | id=P042, price=55000 | 200, price=55000 | HTTP 200 {"data":{"id":"P042","name":"QA Món 330365","categoryId":3,"categoryName":"Đồ Uống","price":55000,"imageUrl":null,"description":"Món dùng để | PASS |
| TC-AD-08 | Xóa danh mục đang có món | categoryId=3 (Đồ Uống) | 400 "Chỉ được xóa danh mục chưa có món ăn" | HTTP 400 {"message":"Chỉ được xóa danh mục chưa có món ăn."} | PASS |
| TC-AD-16 | Xóa danh mục chưa có món | categoryId=26 ("QA Danh mục 330365") | 200 deleted | HTTP 200 {"data":{"deleted":true}} | PASS |
| TC-AD-09 | Thêm bàn mới | tableName="QA Bàn 330365" | 200, trạng thái AVAILABLE, có mã QR | HTTP 200 {"data":{"status":"AVAILABLE","id":28,"qrCodeUrl":"/home/index?tableId=28","tableName":"QA Bàn 330365"}} | PASS |
| TC-AD-10 | Đổi tên bàn trống | id=28 | 200 | HTTP 200 {"data":{"status":"AVAILABLE","id":28,"qrCodeUrl":"/home/index?tableId=28","tableName":"QA Bàn 330365 (đổi tên)"}} | PASS |
| TC-AD-17 | Tổng quan: chi tiết "Bàn đang phục vụ" | GET /api/admin/overview/details?type=open | 200, có bàn test với 2 món đã gửi bếp | HTTP 200 {"data":[{"amount":215000,"servedCount":3,"orderId" sent=2 | PASS |
| TC-AD-18 | Tổng quan: loại chi tiết không hợp lệ | type=khong-co | 400 | HTTP 400 {"message":"Loại chi tiết không hợp lệ."} | PASS |
| TC-AD-20 | Thực đơn có số phần đã bán | GET /api/admin/products | món test đã bán 4 phần (2 tại bàn + 2 mang về) | soldQuantity=4 | PASS |
| TC-AD-11 | Ngưng sử dụng bàn trống | DELETE /api/admin/tables/28 | 200 deactivated | HTTP 200 {"data":{"deactivated":true}} | PASS |
| TC-AD-12 | Khách không vào được bàn đã ngưng | GET /home/index?tableId=28 | 404 | HTTP 404 | PASS |
| TC-AD-19 | Đổi tên bàn đang ngưng hoạt động | id=28 | 200 | HTTP 200 {"data":{"status":"INACTIVE","id":28,"qrCodeUrl":"/home/index?tableId=28","tableName":"QA Bàn 330365 (bảo trì)"}} | PASS |
| TC-AD-13 | Kích hoạt lại bàn | POST /api/admin/tables/28/activate | 200, status=AVAILABLE | HTTP 200 {"data":{"status":"AVAILABLE","id":28,"qrCodeUrl":"/home/index?tableId=28","tableName":"QA Bàn 330365 (bảo trì)"}} | PASS |
| TC-AD-14 | Xóa món đã có trong hóa đơn | DELETE /api/admin/products/P042 | 200, archived=true (chuyển Ngưng kinh doanh) | HTTP 200 {"data":{"archived":true}} | PASS |
| TC-AD-15 | Xem báo cáo doanh thu | GET /api/admin/reports | 200, có revenue | HTTP 200 {"data":{"revenue":6220000,"invoiceCount":21,"averageInvoice":296190,"dailyRevenue":[{"invo | PASS |

## Khách hàng

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-KH-01 | Quét mã QR của bàn | GET /menu?qr=/home/index?tableId=28 | 302 -> /home/index?qr=... | HTTP 302 -> /SmartDining/home/index?qr=%2Fhome%2Findex%3FtableId%3D28 | PASS |
| TC-KH-02 | Trang thực đơn hiển thị món mới thêm | GET /home/index?qr=... | 200, có "QA Món 330365" | HTTP 200 | PASS |
| TC-KH-03 | Mã QR không hợp lệ | qr=ma-qr-khong-ton-tai | 404 | HTTP 404 | PASS |
| TC-KH-04 | Xem giỏ hàng khi chưa quét QR | GET /api/order/get-cart (không có phiên) | 401 "Phien quet QR da het han" | HTTP 401 {"status":"error","message":"Phien quet QR da het han"} | PASS |
| TC-KH-05 | Thêm món vào giỏ | productId=P042, quantity=2, note="ít cay" | 200 success | HTTP 200 {"status":"success","message":"Da them vao gio"} | PASS |
| TC-KH-06 | Thêm món với số lượng 0 | quantity=0 | 400 | HTTP 400 {"status":"error","message":"Mon an hoac so luong khong hop le"} | PASS |
| TC-KH-07 | Thêm món với số lượng âm | quantity=-3 | 400 | HTTP 400 {"status":"error","message":"Mon an hoac so luong khong hop le"} | PASS |
| TC-KH-08 | Thêm món không tồn tại | productId=P_KHONG_CO | 400 | HTTP 400 {"status":"error","message":"Mon an hoac so luong khong hop le"} | PASS |
| TC-KH-09 | Xem giỏ hàng sau khi thêm 2 món | GET /api/order/get-cart | 200, có 2 món vừa thêm | HTTP 200 {"items":[{"note":"ít cay","productId":"P042","price":55000,"qty":2,"na (items=2) | PASS |
| TC-KH-10 | Hủy món chưa gửi bếp | orderDetailId=CT-b4a39eb4-ec72-42e2-99e7-2edd60422494 (P036) | 200 success | HTTP 200 {"status":"success","message":"Da huy mon"} | PASS |
| TC-KH-18 | Tăng số lượng món trong giỏ | orderDetailId=CT-67191bc3-f479-45bf-a222-f93b37f12c9e, quantity 2 -> 3 | 200, giỏ hàng có qty=3 | HTTP 200 {"status":"success","message":"Da cap nhat so luong"} qty=3 | PASS |
| TC-KH-19 | Giảm số lượng món trong giỏ | quantity 3 -> 2 | 200, giỏ hàng có qty=2 | HTTP 200 {"status":"success","message":"Da cap nhat so luong"} qty=2 | PASS |
| TC-KH-20 | Giảm số lượng về 0 | quantity=0 | 400 (dùng "Hủy món" để bỏ món) | HTTP 400 {"status":"error","message":"Chi doi duoc so luong mon chua gui bep (1-100)"} | PASS |
| TC-KH-21 | Số lượng vượt giới hạn | quantity=101 | 400 | HTTP 400 {"status":"error","message":"Chi doi duoc so luong mon chua gui bep (1-100)"} | PASS |
| TC-KH-11 | Xác nhận gửi đơn xuống bếp | POST /api/order/confirm | 200 "Don da duoc gui xuong bep" | HTTP 200 {"status":"success","message":"Don da duoc gui xuong bep"} | PASS |
| TC-KH-22 | Đổi số lượng món đã gửi bếp | orderDetailId=CT-67191bc3-f479-45bf-a222-f93b37f12c9e, quantity=5 | 400 "Chi doi duoc so luong mon chua gui bep" | HTTP 400 {"status":"error","message":"Chi doi duoc so luong mon chua gui bep (1-100)"} | PASS |
| TC-KH-12 | Xác nhận khi giỏ trống | POST /api/order/confirm (lần 2) | 400 "Gio hang dang trong" | HTTP 400 {"status":"error","message":"Gio hang dang trong"} | PASS |
| TC-KH-13 | Hủy món đã gửi bếp | orderDetailId=CT-67191bc3-f479-45bf-a222-f93b37f12c9e | 400 "Chi co the huy mon chua gui bep" | HTTP 400 {"status":"error","message":"Chi co the huy mon chua gui bep"} | PASS |
| TC-KH-14 | Gửi yêu cầu phục vụ | type="Thêm nước lọc", note="2 ly" | 200 success | HTTP 200 {"status":"success","message":"Da gui yeu cau toi nhan vien"} | PASS |
| TC-KH-15 | Gửi yêu cầu phục vụ rỗng | type="" | 400 | HTTP 400 {"status":"error","message":"Noi dung yeu cau khong hop le"} | PASS |
| TC-KH-16 | Gửi yêu cầu thanh toán | POST /api/order/request-payment | 200 success | HTTP 200 {"status":"success","message":"Da gui yeu cau thanh toan toi nhan vien"} | PASS |
| TC-KH-17 | Giỏ hàng sau khi đã thanh toán | GET /api/order/get-cart | 200, không còn món | HTTP 200 {"items":[],"totalAmount":0,"projectedTotal":0,"currentOrderTotal":0,"status":"success"} | PASS |

## Bếp

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-BEP-01 | Món vừa gửi xuất hiện trên màn hình bếp | GET /api/kitchen/tasks | 200, có món CT-67191bc3-f479-45bf-a222-f93b37f12c9e trạng thái PENDING | HTTP 200 {"status":"success","data":[{"note":"Siêu cạy","ord found=true status=PENDING | PASS |
| TC-BEP-02 | Chuyển món sang Đang nấu | status=PREPARING | 200 success | HTTP 200 {"status":"success"} | PASS |
| TC-BEP-03 | Chuyển món sang Đã xong | status=READY | 200 success | HTTP 200 {"status":"success"} | PASS |
| TC-BEP-04 | Cập nhật trạng thái không hợp lệ | status=KHONG_HOP_LE | 400 | HTTP 400 {"status":"error","message":"Du lieu khong hop le"} | PASS |

## Thanh toán

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-TT-01 | Thanh toán khi khách chưa yêu cầu | tableId=28, method=CASH | 400 NOT_REQUESTED | HTTP 400 {"code":"NOT_REQUESTED","message":"Khach chua yeu cau thanh toan","status":"error"} | PASS |
| TC-TT-02 | Thanh toán khi còn món chưa phục vụ | Khách đã yêu cầu, món đang ở trạng thái READY | 400 ITEMS_NOT_SERVED | HTTP 400 {"code":"ITEMS_NOT_SERVED","message":"Con mon chua phuc vu, chua the thanh toan","status":"error"} | PASS |
| TC-TT-03 | Không thể chốt thanh toán bằng "Đã xử lý" yêu cầu | complete-request với yêu cầu PAYMENT | 400 (phải dùng màn hình Thanh toán) | HTTP 400 {"status":"error"} | PASS |
| TC-TT-04 | Xem hóa đơn chi tiết trước khi thanh toán | GET /api/staff/bill?tableId=28 | 200, canPay=true, 1 dòng (SL 2 × đơn giá), tổng = 110000, có nhân viên phụ trách | HTTP 200 {"status":"success","data":{"ca canPay=true dong=1 sl=2 tong=110000 nv=Nhân viên 01 | PASS |
| TC-TT-05 | Chuyển khoản nhưng không có ảnh bill | method=TRANSFER, image=null | 400 IMAGE_REQUIRED | HTTP 400 {"code":"IMAGE_REQUIRED","message":"Chuyen khoan can anh chup bill","status":"error"} | PASS |
| TC-TT-06 | Chuyển khoản với ảnh không hợp lệ | image="khong-phai-anh" | 400 INVALID_IMAGE | HTTP 400 {"code":"INVALID_IMAGE","message":"Anh bill khong hop le hoac qua lon (toi da 3MB)","status":"error"} | PASS |
| TC-TT-07 | Phương thức thanh toán không hợp lệ | method=THE_TIN_DUNG | 400 INVALID_METHOD | HTTP 400 {"code":"INVALID_METHOD","message":"Phuong thuc thanh toan khong hop le","status":"error"} | PASS |
| TC-TT-08 | Hóa đơn lưu phương thức, nhân viên và ảnh bill | Admin: overview/details?type=paid + xem ảnh bill | method=TRANSFER, có tên nhân viên, ảnh bill đúng ảnh đã gửi | method=TRANSFER nv=Nhân viên 01 anh=true | PASS |

## Phục vụ

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-PV-01 | Xem chi tiết bàn có món đã xong | tableId=28 | 200, món ở trạng thái READY | HTTP 200 {"status":"success","data":{"items":[{"unitPrice":5 status=READY | PASS |
| TC-PV-02 | Đánh dấu đã mang món ra | orderDetailId=CT-67191bc3-f479-45bf-a222-f93b37f12c9e | 200 success | HTTP 200 {"status":"success"} | PASS |
| TC-PV-03 | Đánh dấu phục vụ lại món đã phục vụ | orderDetailId=CT-67191bc3-f479-45bf-a222-f93b37f12c9e (lần 2) | 400 | HTTP 400 {"status":"error"} | PASS |
| TC-PV-04 | Nhân viên thấy yêu cầu phục vụ và thanh toán của bàn | GET /api/staff/requests | 200, có 1 SERVICE + 1 PAYMENT | HTTP 200 {"status":"success","data":[{"s service=true payment=true | PASS |
| TC-PV-05 | Hoàn tất yêu cầu phục vụ | requestId (SERVICE) | 200 success | HTTP 200 {"status":"success"} | PASS |
| TC-PV-06 | Thanh toán chuyển khoản kèm ảnh bill | method=TRANSFER, image=(ảnh PNG) | 200 success | HTTP 200 {"code":"OK","message":"Da thanh toan","status":"success"} | PASS |
| TC-PV-07 | Sau thanh toán bàn trở về Trống | tableId=28 | status=AVAILABLE | status=AVAILABLE | PASS |

## Mang về

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-MV-01 | Nhân viên xem thực đơn để bán mang về | GET /api/staff/menu | 200, có món test kèm giá và danh mục | HTTP 200 {"status":"success","data":[{"price":2000 found=true | PASS |
| TC-MV-02 | Tạo đơn mang về không có món | items=[] | 400 EMPTY | HTTP 400 {"code":"EMPTY","message":"Chua chon mon nao","status":"error"} | PASS |
| TC-MV-03 | Số điện thoại không hợp lệ | customerPhone="abc" | 400 INVALID_CUSTOMER | HTTP 400 {"code":"INVALID_CUSTOMER","message":"Ten hoac so dien thoai khong hop le","status":"error"} | PASS |
| TC-MV-04 | Giờ lấy không hợp lệ | pickupTime="25:99" | 400 INVALID_PICKUP_TIME | HTTP 400 {"code":"INVALID_PICKUP_TIME","message":"Gio lay do khong hop le (dinh dang HH:mm)","status":"error"} | PASS |
| TC-MV-05 | Chuyển khoản không có ảnh bill | method=TRANSFER, image=null | 400 IMAGE_REQUIRED | HTTP 400 {"code":"IMAGE_REQUIRED","message":"Chuyen khoan can anh chup bill","status":"error"} | PASS |
| TC-MV-06 | Số lượng món bằng 0 | quantity=0 | 400 INVALID_ITEM | HTTP 400 {"code":"INVALID_ITEM","message":"Mon hoac so luong khong hop le (mon phai dang ban, so luong 1-100)","status":"error"} | PASS |
| TC-MV-07 | Tạo đơn mang về, thanh toán tiền mặt | P042 × 2, khách "QA Khách 330365" | 200, có mã lấy đồ, tổng = 110000 | HTTP 200 {"status":"success","data":{"pickupCode":1,"total":110000,"orderId":"HD-e1e584ca-b7da-4cb0-bc20-f6addf9f4088"}} | PASS |
| TC-MV-08 | Món mang về xuống bếp ngay sau khi thanh toán | GET /api/kitchen/tasks | có món của "Mang về #1", không có bàn | group=Mang về #1 type=TAKEAWAY tableId=null | PASS |
| TC-MV-09 | Giao đồ khi bếp chưa làm xong | POST /api/staff/takeaway/deliver | 400 NOT_READY | HTTP 400 {"code":"NOT_READY","message":"Bep chua lam xong tat ca mon","status":"error"} | PASS |
| TC-MV-10 | Bếp làm xong thì đơn chuyển sang "Chờ giao" | Bếp: PREPARING → READY | state=ready | state=ready | PASS |
| TC-MV-11 | Giao đồ cho khách | POST /api/staff/takeaway/deliver | 200, state=delivered | HTTP 200 {"code":"OK","message":"Da giao do cho khach","status":"success"} state=delivered | PASS |
| TC-MV-12 | Giao lại đơn đã giao | deliver lần 2 | 400 ALREADY_DELIVERED | HTTP 400 {"code":"ALREADY_DELIVERED","message":"Don nay da giao roi","status":"error"} | PASS |
| TC-MV-13 | Doanh thu hôm nay có đơn mang về | Admin: overview/details?type=paid | có "Mang về #1", phương thức CASH | tableName=Mang về #1 method=CASH | PASS |

## Báo cáo nhân viên

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-BC-01 | Nhân viên phục vụ: hóa đơn, tiền thu, món mang ra | staff-activity?from=2026-10-12&to=2026-10-12 | staff1 có ≥ 2 hóa đơn, tiền thu > 0, ≥ 3 món đã mang ra, có yêu cầu đã xử lý | hd=4 tien=975000 mon=4 yc=6 | PASS |
| TC-BC-02 | Nhân viên bếp: số món đã nấu và thời gian nấu | staff-activity?from=2026-10-12&to=2026-10-12 | kitchen1 có ≥ 4 món đã nấu (2 tại bàn + 2 mang về), có thời gian nấu TB | mon=4 nauTB=0 | PASS |
| TC-BC-03 | Khoảng ngày không hợp lệ | from > to | 400 | HTTP 400 {"message":"Khoảng thời gian không hợp lệ."} | PASS |
| TC-BC-04 | Nhân viên phục vụ không xem được báo cáo | staff1: GET /api/admin/staff-activity | 403 | HTTP 403 | PASS |

## Tài khoản

| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
|---|---|---|---|---|---|
| TC-NV-01 | Tạo tài khoản không có email | username=qa330365, email="" | 400 | HTTP 400 {"message":"Không thể tạo tài khoản. Kiểm tra tên đăng nhập, mật khẩu (ít nhất 8 ký tự) và email."} | PASS |
| TC-NV-02 | Tạo tài khoản email sai định dạng | email="email-sai" | 400 | HTTP 400 {"message":"Không thể tạo tài khoản. Kiểm tra tên đăng nhập, mật khẩu (ít nhất 8 ký tự) và email."} | PASS |
| TC-NV-03 | Tạo tài khoản mật khẩu dưới 8 ký tự | password="123" | 400 | HTTP 400 {"message":"Không thể tạo tài khoản. Kiểm tra tên đăng nhập, mật khẩu (ít nhất 8 ký tự) và email."} | PASS |
| TC-NV-04 | Tạo tài khoản hợp lệ | username=qa330365, email=qa330365@example.com | 200, status=PENDING | HTTP 200 {"data":{"id":"NV000009","username":"qa330365","fullName":"QA Nhân viên 330365","phone":"0900000000","email":"qa330365@example.com","role":" | PASS |
| TC-NV-05 | Tạo tài khoản trùng tên đăng nhập | username=qa330365 (lần 2) | 400 | HTTP 400 {"message":"Không thể tạo tài khoản. Kiểm tra tên đăng nhập, mật khẩu (ít nhất 8 ký tự) và email."} | PASS |
| TC-NV-06 | Lần đầu đăng nhập yêu cầu nhập mã xác thực | qa330365 / Matkhau@123 | 200, trang "Xác thực tài khoản" | HTTP 200 | PASS |
| TC-NV-07 | Chưa xác thực thì không vào được trang làm việc | GET /staff/dashboard | 302 -> /login | HTTP 302 -> /SmartDining/login | PASS |
| TC-NV-08 | Nhập sai mã xác thực | code=000000 | 200, báo "Ma xac thuc khong dung. Con 4 lan thu" | HTTP 200 | PASS |
| TC-NV-09 | Gửi lại mã ngay lập tức (chưa đủ 60 giây) | POST /login/verify/resend | 200, báo "Vui long doi ... giay" | HTTP 200 | PASS |
| TC-NV-10 | Nhập đúng mã xác thực | code=(mã trong email / log DEV) | 302 -> /staff/dashboard | HTTP 302 -> /SmartDining/staff/dashboard | PASS |
| TC-NV-11 | Sau khi xác thực tài khoản thành Đang hoạt động | username=qa330365 | status=ACTIVE | status=ACTIVE | PASS |
| TC-NV-12 | Nhân viên mới vào được trang làm việc | GET /staff/dashboard | 200 | HTTP 200 | PASS |
| TC-NV-13 | Dùng lại mã sau khi đã xác thực | POST /login/verify | 302 -> /login (không còn phiên chờ xác thực) | HTTP 302 -> /SmartDining/login | PASS |
| TC-NV-14 | Nhập sai mã 5 lần | code=999999 x5 | 200, báo "nhap sai qua nhieu lan" | HTTP 200 | PASS |
| TC-NV-15 | Nhập đúng mã sau khi đã sai 5 lần | code=(mã đúng) | 200, vẫn bị chặn, yêu cầu gửi lại mã | HTTP 200 | PASS |
| TC-NV-16 | Khóa tài khoản đang đăng nhập thì bị đăng xuất | admin khóa qa330365 | 302 -> /login | khóa: HTTP 200 {"data":{"id":"NV000009","username":"qa330365","fullName":"QA Nhân viên \| vào lại: HTTP 302 -> /SmartDining/login | PASS |

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
