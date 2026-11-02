<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SmartDining - Quản trị</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin.css?v=20261012-10">
</head>
<body>
    <div class="admin-shell">
        <aside class="admin-sidebar" aria-label="Điều hướng quản trị">
            <a class="admin-brand" href="${pageContext.request.contextPath}/admin/dashboard">
                <span class="brand-mark"><i class="fa-solid fa-utensils"></i></span>
                <span class="brand-copy"><strong>SmartDining</strong><small>Quản trị nhà hàng</small></span>
            </a>
            <button class="sidebar-toggle" id="sidebarToggle" type="button" aria-pressed="false" title="Thu gọn thanh điều hướng" aria-label="Thu gọn thanh điều hướng">
                <i class="fa-solid fa-angles-left"></i>
            </button>
            <p class="nav-caption">Tác vụ</p>

            <nav class="admin-nav" aria-label="Chức năng quản trị">
                <button class="nav-item is-active" type="button" data-view="overview" title="Tổng quan">
                    <i class="fa-solid fa-chart-pie"></i><span>Tổng quan</span>
                </button>
                <button class="nav-item" type="button" data-view="menu" title="Thực đơn">
                    <i class="fa-solid fa-bowl-food"></i><span>Thực đơn</span>
                </button>
                <button class="nav-item" type="button" data-view="tables" title="Bàn và QR">
                    <i class="fa-solid fa-table-cells-large"></i><span>Bàn và QR</span>
                </button>
                <button class="nav-item" type="button" data-view="employees" title="Nhân sự">
                    <i class="fa-solid fa-users"></i><span>Nhân sự</span>
                </button>
                <button class="nav-item" type="button" data-view="reports" title="Báo cáo">
                    <i class="fa-solid fa-chart-column"></i><span>Báo cáo</span>
                </button>
            </nav>

            <div class="sidebar-account">
                <span class="avatar"><i class="fa-solid fa-user"></i></span>
                <span class="account-copy"><strong><c:out value="${employee.fullName}" /></strong><small>Chủ quán</small></span>
                <a class="icon-button account-logout" href="${pageContext.request.contextPath}/logout?role=ADMIN" title="Đăng xuất" aria-label="Đăng xuất">
                    <i class="fa-solid fa-arrow-right-from-bracket"></i>
                </a>
            </div>
        </aside>

        <main class="admin-main">
            <header class="admin-header">
                <div>
                    <p class="section-kicker" id="viewEyebrow">Quản trị nhà hàng</p>
                    <h1 id="viewTitle">Tổng quan</h1>
                </div>
                <div class="header-actions">
                    <span class="live-status is-offline" id="liveStatus" title="Đang kết nối..." aria-live="polite"><i class="fa-solid fa-circle"></i><span>Đang kết nối</span></span>
                    <button class="icon-button" id="refreshButton" type="button" title="Làm mới dữ liệu" aria-label="Làm mới dữ liệu">
                        <i class="fa-solid fa-rotate"></i>
                    </button>
                    <span class="header-user"><i class="fa-solid fa-user-shield"></i><c:out value="${employee.fullName}" /></span>
                </div>
            </header>

            <section class="admin-view is-active" id="overviewView" data-view-panel="overview">
                <div class="metric-grid" aria-live="polite">
                    <article class="metric-card metric-revenue is-clickable" data-detail="paid" tabindex="0" role="button" title="Xem chi tiết"><span class="metric-icon"><i class="fa-solid fa-wallet"></i></span><div><p>Doanh thu hôm nay</p><strong id="todayRevenue">0 đ</strong></div></article>
                    <article class="metric-card metric-bill is-clickable" data-detail="paid" tabindex="0" role="button" title="Xem chi tiết"><span class="metric-icon"><i class="fa-solid fa-receipt"></i></span><div><p>Hóa đơn đã chốt</p><strong id="todayOrders">0</strong></div></article>
                    <article class="metric-card metric-table is-clickable" data-detail="open" tabindex="0" role="button" title="Xem chi tiết"><span class="metric-icon"><i class="fa-solid fa-chair"></i></span><div><p>Bàn đang phục vụ</p><strong id="openTables">0</strong></div></article>
                    <article class="metric-card metric-alert is-clickable" data-detail="waiting" tabindex="0" role="button" title="Xem chi tiết"><span class="metric-icon"><i class="fa-solid fa-bell"></i></span><div><p>Chờ thanh toán</p><strong id="waitingTables">0</strong></div></article>
                </div>
                <div class="overview-grid">
                    <section class="surface-panel">
                        <div class="panel-heading"><div><h2>Vận hành hiện tại</h2><p>Tự cập nhật theo thời gian thực từ các đơn và bàn đang mở.</p></div></div>
                        <dl class="operation-list">
                            <div class="is-clickable" data-detail="employees" tabindex="0" role="button" title="Xem chi tiết"><dt>Nhân sự đang hoạt động</dt><dd id="activeEmployees">0</dd><i class="fa-solid fa-chevron-right detail-chevron" aria-hidden="true"></i></div>
                            <div class="is-clickable" data-detail="open" tabindex="0" role="button" title="Xem chi tiết"><dt>Bàn đang phục vụ</dt><dd id="overviewOpenTables">0</dd><i class="fa-solid fa-chevron-right detail-chevron" aria-hidden="true"></i></div>
                            <div class="is-clickable" data-detail="waiting" tabindex="0" role="button" title="Xem chi tiết"><dt>Bàn yêu cầu thanh toán</dt><dd id="overviewWaitingTables">0</dd><i class="fa-solid fa-chevron-right detail-chevron" aria-hidden="true"></i></div>
                            <div class="is-clickable" data-detail="requests" tabindex="0" role="button" title="Xem chi tiết"><dt>Yêu cầu đang chờ xử lý</dt><dd id="pendingRequests">0</dd><i class="fa-solid fa-chevron-right detail-chevron" aria-hidden="true"></i></div>
                        </dl>
                    </section>
                    <section class="surface-panel quiet-panel">
                        <div class="panel-heading"><div><h2>Luồng phục vụ</h2><p>Admin quản lý dữ liệu nền; Staff và Bếp tiếp tục xử lý order thời gian thực.</p></div></div>
                        <ol class="flow-list"><li>Khách gọi món tại bàn</li><li>Bếp chế biến và báo hoàn thành</li><li>Nhân viên phục vụ, nhận thanh toán</li></ol>
                    </section>
                </div>
            </section>

            <section class="admin-view" id="menuView" data-view-panel="menu">
                <div class="view-toolbar">
                    <div id="productCategoryFilters" class="category-filter-buttons" role="group" aria-label="Lọc món theo danh mục">
                        <button class="category-filter-button is-active" type="button" data-category-filter="" aria-pressed="true">Tất cả danh mục</button>
                    </div>
                    <div class="toolbar-spacer"></div>
                    <label class="search-field"><i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i><input id="productSearch" type="search" placeholder="Tìm món theo tên" aria-label="Tìm món theo tên" autocomplete="off"></label>
                    <select class="select-control sort-control" id="productSort" aria-label="Sắp xếp món"><option value="name">Tên A–Z</option><option value="sold">Bán chạy nhất</option><option value="priceAsc">Giá thấp → cao</option><option value="priceDesc">Giá cao → thấp</option></select>
                    <span class="view-toggle" role="group" aria-label="Kiểu hiển thị"><button type="button" data-product-view="grid" title="Dạng lưới" aria-label="Dạng lưới"><i class="fa-solid fa-table-cells-large"></i></button><button type="button" data-product-view="list" title="Dạng danh sách" aria-label="Dạng danh sách"><i class="fa-solid fa-list"></i></button></span>
                    <button class="secondary-button" type="button" id="manageCategoriesButton"><i class="fa-solid fa-tags"></i><span>Danh mục</span></button>
                    <button class="primary-button" type="button" id="addProductButton"><i class="fa-solid fa-plus"></i><span>Thêm món</span></button>
                </div>
                <div class="product-admin-grid" id="productList" aria-live="polite"></div>
            </section>

            <section class="admin-view" id="tablesView" data-view-panel="tables">
                <div class="view-toolbar">
                    <div><h2 class="toolbar-title">Bàn và đường dẫn QR</h2><p class="toolbar-copy">QR mở bằng dịch vụ bên ngoài và trỏ trực tiếp đến đúng bàn.</p></div>
                    <div class="toolbar-spacer"></div>
                    <button class="primary-button" type="button" id="addTableButton"><i class="fa-solid fa-plus"></i><span>Thêm bàn</span></button>
                </div>
                <div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>Bàn</th><th>Trạng thái</th><th>Đường dẫn khách</th><th aria-label="Thao tác"></th></tr></thead><tbody id="tableList"></tbody></table></div>
            </section>

            <section class="admin-view" id="employeesView" data-view-panel="employees">
                <div class="view-toolbar">
                    <div><h2 class="toolbar-title">Tài khoản nhân sự</h2><p class="toolbar-copy">Chỉ tạo tài khoản nhân viên phục vụ hoặc nhân viên bếp.</p></div>
                    <div class="toolbar-spacer"></div>
                    <button class="primary-button" type="button" id="addEmployeeButton"><i class="fa-solid fa-user-plus"></i><span>Cấp tài khoản</span></button>
                </div>
                <div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>Nhân sự</th><th>Vai trò</th><th>Liên hệ</th><th>Trạng thái</th><th aria-label="Thao tác"></th></tr></thead><tbody id="employeeList"></tbody></table></div>
            </section>

            <section class="admin-view" id="reportsView" data-view-panel="reports">
                <form class="report-filter" id="reportFilter">
                    <label>Từ ngày<input id="reportFrom" type="date"></label>
                    <label>Đến ngày<input id="reportTo" type="date"></label>
                    <button class="primary-button" type="submit"><i class="fa-solid fa-filter"></i><span>Lọc báo cáo</span></button>
                    <button class="text-button" type="button" id="clearReportFilter">Xóa lọc</button>
                </form>
                <div class="metric-grid report-metric-grid">
                    <article class="metric-card metric-revenue"><span class="metric-icon"><i class="fa-solid fa-coins"></i></span><div><p>Doanh thu đã thanh toán</p><strong id="reportRevenue">0 đ</strong></div></article>
                    <article class="metric-card metric-bill"><span class="metric-icon"><i class="fa-solid fa-file-invoice"></i></span><div><p>Số hóa đơn</p><strong id="reportInvoices">0</strong></div></article>
                    <article class="metric-card metric-table"><span class="metric-icon"><i class="fa-solid fa-calculator"></i></span><div><p>Trung bình hóa đơn</p><strong id="reportAverage">0 đ</strong></div></article>
                </div>
                <div class="report-layout">
                    <section class="surface-panel report-chart-panel"><div class="panel-heading"><div><h2>Doanh thu theo ngày</h2><p>Chỉ tính hóa đơn đã thanh toán.</p></div></div><div class="revenue-chart" id="revenueChart"></div></section>
                    <section class="surface-panel"><div class="panel-heading"><div><h2>Món được gọi nhiều</h2><p>Xếp theo tổng số lượng trong các hóa đơn đã chốt.</p></div></div><div class="best-seller-list" id="bestSellerList"></div></section>
                </div>
                <section class="surface-panel staff-activity-panel">
                    <div class="panel-heading"><div><h2>Hoạt động nhân viên</h2><p>Theo khoảng ngày đang lọc. Thời gian phản hồi = từ lúc khách gửi yêu cầu đến lúc nhân viên xử lý; số liệu của bếp tính từ 12/10/2026.</p></div><button class="secondary-button" type="button" id="exportStaffActivity"><i class="fa-solid fa-file-csv"></i><span>Xuất CSV</span></button></div>
                    <div class="admin-table-wrap staff-activity-wrap"><table class="admin-table staff-activity-table"><thead><tr><th>Nhân viên</th><th class="num">Hóa đơn</th><th class="num">Tiền thu</th><th class="num">Tiền mặt / CK</th><th class="num">Yêu cầu</th><th class="num">Phản hồi TB</th><th class="num">Món mang ra</th><th class="num">Món bếp làm</th><th class="num">Nấu TB</th><th class="num">Báo hết</th></tr></thead><tbody id="staffActivityList"><tr><td colspan="10" class="detail-empty">Đang tải...</td></tr></tbody></table></div>
                </section>
            </section>
        </main>
    </div>

    <div class="modal" id="productModal" aria-hidden="true"><div class="modal-dialog modal-wide" role="dialog" aria-modal="true" aria-labelledby="productModalTitle"><div class="modal-header"><h2 id="productModalTitle">Thêm món ăn</h2><button class="icon-button" type="button" data-close-modal="productModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><form id="productForm" class="form-grid"><input type="hidden" id="productId"><label>Tên món<input id="productName" maxlength="150" required></label><label>Danh mục<select id="productCategory" required></select></label><label>Giá bán<input id="productPrice" type="number" min="0" step="1000" required></label><label>Trạng thái<select id="productStatus"><option value="AVAILABLE">Đang bán</option><option value="OUT_OF_STOCK">Tạm hết hàng</option><option value="DISCONTINUED">Ngưng kinh doanh</option></select></label><label class="field-full">Link ảnh<input id="productImageUrl" type="url" maxlength="2000" placeholder="https://..."></label><label class="field-full">Mô tả<textarea id="productDescription" maxlength="500" rows="3"></textarea></label><div class="modal-actions field-full"><button class="secondary-button" type="button" data-close-modal="productModal">Hủy</button><button class="primary-button" type="submit"><i class="fa-solid fa-floppy-disk"></i><span>Lưu món</span></button></div></form></div></div>

    <div class="modal" id="categoryModal" aria-hidden="true"><div class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="categoryModalTitle"><div class="modal-header"><h2 id="categoryModalTitle">Danh mục</h2><button class="icon-button" type="button" data-close-modal="categoryModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><form class="inline-create" id="categoryForm"><input id="categoryId" type="hidden"><input id="categoryName" maxlength="100" placeholder="Tên danh mục" required><button class="primary-button" type="submit"><i class="fa-solid fa-plus"></i><span>Lưu</span></button></form><div class="category-list" id="categoryList"></div></div></div>

    <div class="modal" id="tableModal" aria-hidden="true"><div class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="tableModalTitle"><div class="modal-header"><h2 id="tableModalTitle">Thêm bàn</h2><button class="icon-button" type="button" data-close-modal="tableModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><form id="tableForm" class="single-form"><input id="tableId" type="hidden"><label>Tên bàn<input id="tableName" maxlength="50" placeholder="Ví dụ: Bàn 07" required></label><label id="tableStatusField" hidden>Trạng thái<select id="tableStatusSelect"><option value="ACTIVE">Đang hoạt động (nhận khách)</option><option value="INACTIVE">Ngưng hoạt động (bảo trì hoặc không sử dụng nữa)</option></select></label><p class="field-hint">Chỉ sửa được khi bàn không có khách. Bàn ngưng hoạt động không hiện ở trang phục vụ, bếp và mã QR không gọi món được.</p><div class="modal-actions"><button class="secondary-button" type="button" data-close-modal="tableModal">Hủy</button><button class="primary-button" type="submit"><i class="fa-solid fa-floppy-disk"></i><span>Lưu bàn</span></button></div></form></div></div>

    <div class="modal" id="employeeModal" aria-hidden="true"><div class="modal-dialog modal-wide" role="dialog" aria-modal="true" aria-labelledby="employeeModalTitle"><div class="modal-header"><h2 id="employeeModalTitle">Cấp tài khoản</h2><button class="icon-button" type="button" data-close-modal="employeeModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><form id="employeeForm" class="form-grid"><input id="employeeId" type="hidden"><label>Họ và tên<input id="employeeFullName" maxlength="100" required></label><label class="create-only">Vai trò<select id="employeeRole"><option value="STAFF">Nhân viên phục vụ</option><option value="KITCHEN">Nhân viên bếp</option></select></label><label class="create-only">Tên đăng nhập<input id="employeeUsername" maxlength="50" pattern="[A-Za-z0-9._-]{3,50}" required></label><label>Mật khẩu <span class="optional-copy" id="passwordHint">(ít nhất 8 ký tự)</span><input id="employeePassword" type="password" minlength="8"></label><label>Số điện thoại<input id="employeePhone" maxlength="15"></label><label>Email <span class="optional-copy">(nhận mã xác thực)</span><input id="employeeEmail" type="email" maxlength="100"></label><label class="edit-only">Trạng thái<select id="employeeStatus"><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Đã khóa</option><option value="PENDING">Chờ xác thực email</option></select></label><div class="modal-actions field-full"><button class="secondary-button" type="button" data-close-modal="employeeModal">Hủy</button><button class="primary-button" type="submit"><i class="fa-solid fa-floppy-disk"></i><span>Lưu tài khoản</span></button></div></form></div></div>

    <div class="modal" id="detailModal" aria-hidden="true"><div class="modal-dialog modal-wide" role="dialog" aria-modal="true" aria-labelledby="detailTitle"><div class="modal-header"><h2 id="detailTitle">Chi tiết</h2><button class="icon-button" type="button" data-close-modal="detailModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><div class="detail-body" id="detailBody"></div></div></div>
    <div class="modal" id="confirmModal" aria-hidden="true"><div class="modal-dialog modal-small" role="dialog" aria-modal="true" aria-labelledby="confirmTitle"><div class="modal-header"><h2 id="confirmTitle">Xác nhận thao tác</h2><button class="icon-button" type="button" data-close-modal="confirmModal" title="Đóng" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button></div><p class="confirm-message" id="confirmMessage"></p><div class="modal-actions"><button class="secondary-button" type="button" data-close-modal="confirmModal">Hủy</button><button class="danger-button" type="button" id="confirmButton"><i class="fa-solid fa-trash"></i><span>Xác nhận</span></button></div></div></div>
    <div class="toast-region" id="toastRegion" aria-live="polite" aria-atomic="true"></div>

    <script>const contextPath = "${pageContext.request.contextPath}";</script>
    <script src="${pageContext.request.contextPath}/js/admin.js?v=20261012-10"></script>
</body>
</html>
