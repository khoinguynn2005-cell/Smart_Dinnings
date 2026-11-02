<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SmartDining - Nhân viên phục vụ</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/staff.css?v=20261012-9">
</head>
<body>
    <header class="sidebar">
        <a class="brand" href="${pageContext.request.contextPath}/staff/dashboard"><i class="fa-solid fa-bell-concierge"></i><span>Nhân viên phục vụ</span></a>
        <nav class="side-nav" aria-label="Điều hướng nhân viên">
            <button class="side-action active" type="button" data-staff-view="tables" aria-pressed="true" title="Theo dõi bàn" aria-label="Theo dõi bàn"><i class="fa-solid fa-table-cells-large"></i><span>Theo dõi bàn</span></button>
            <button class="side-action" type="button" data-staff-view="takeaway" aria-pressed="false" title="Bán mang về" aria-label="Bán mang về"><i class="fa-solid fa-bag-shopping"></i><span>Mang về</span></button>
            <button class="side-action" type="button" title="Làm mới" aria-label="Làm mới" onclick="refreshCurrentView()"><i class="fa-solid fa-rotate"></i><span>Làm mới</span></button>
        </nav>
        <div class="staff-account">
            <span class="staff-name"><i class="fa-solid fa-user"></i><span>${requestScope.employee.fullName}</span></span>
            <a class="staff-logout" href="${pageContext.request.contextPath}/logout?role=STAFF">Đăng xuất</a>
        </div>
    </header>
    <div id="staffToast" class="staff-toast" role="status" aria-live="assertive">
        <i class="fa-solid fa-bell" aria-hidden="true"></i>
        <div><strong id="staffToastTitle">Bếp đã hoàn tất món</strong><span id="staffToastMessage"></span></div>
    </div>
    <main class="workspace">
        <section class="table-panel" id="tablePanel">
            <section>
                <div class="section-heading">
                    <h2>Bàn đang theo dõi</h2>
                </div>
                <div id="tableList" class="table-list"></div>
            </section>
        </section>
        <section class="takeaway-panel" id="takeawayPanel" hidden>
            <div class="takeaway-layout">
                <section class="takeaway-menu" aria-label="Chọn món mang về">
                    <label class="takeaway-search"><i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i><input id="takeawaySearch" type="search" placeholder="Tìm món theo tên" aria-label="Tìm món theo tên" autocomplete="off"></label>
                    <div class="takeaway-categories" id="takeawayCategories"></div>
                    <div class="takeaway-products" id="takeawayProducts"><div class="empty">Đang tải thực đơn...</div></div>
                </section>
                <aside class="takeaway-cart" aria-label="Đơn mang về">
                    <div class="takeaway-cart-head"><h2>Đơn mang về mới</h2><button class="link-action" type="button" id="takeawayClear">Làm lại</button></div>
                    <div class="takeaway-fields">
                        <input id="takeawayName" maxlength="100" placeholder="Tên khách (không bắt buộc)" aria-label="Tên khách">
                        <input id="takeawayPhone" maxlength="15" inputmode="tel" placeholder="Số điện thoại (không bắt buộc)" aria-label="Số điện thoại">
                        <label class="takeaway-time">Giờ lấy <input id="takeawayPickup" type="time" aria-label="Giờ lấy đồ"><small>Để trống = lấy ngay</small></label>
                    </div>
                    <div class="takeaway-lines" id="takeawayLines"><p class="takeaway-empty">Chọn món bên trái để thêm vào đơn.</p></div>
                    <div class="takeaway-total"><span>Tổng cộng</span><strong id="takeawayTotal">0 đ</strong></div>
                    <fieldset class="pay-methods" id="takeawayMethods"><legend>Thanh toán trước</legend>
                        <label class="pay-method"><input type="radio" name="takeawayMethod" value="CASH" checked><span><i class="fa-solid fa-money-bill-wave" aria-hidden="true"></i><strong>Tiền mặt</strong><small>Không cần ảnh</small></span></label>
                        <label class="pay-method"><input type="radio" name="takeawayMethod" value="TRANSFER"><span><i class="fa-solid fa-building-columns" aria-hidden="true"></i><strong>Chuyển khoản</strong><small>Bắt buộc chụp bill</small></span></label>
                    </fieldset>
                    <div class="proof-box" id="takeawayProofBox" hidden>
                        <label class="proof-picker" for="takeawayProof"><i class="fa-solid fa-camera" aria-hidden="true"></i><span id="takeawayProofLabel">Chụp hoặc chọn ảnh bill chuyển khoản</span></label>
                        <input type="file" id="takeawayProof" accept="image/*" capture="environment" hidden>
                        <img id="takeawayProofPreview" class="proof-preview" alt="Ảnh bill chuyển khoản" hidden>
                    </div>
                    <p class="checkout-error" id="takeawayError" role="alert" hidden></p>
                    <button class="action takeaway-submit" type="button" id="takeawaySubmit" disabled><i class="fa-solid fa-fire-burner" aria-hidden="true"></i> Thanh toán & gửi bếp</button>
                </aside>
            </div>
            <section class="takeaway-orders">
                <div class="section-heading"><h2>Đơn mang về hôm nay</h2><span class="takeaway-legend"><span class="sd-pill sd-pill--warning">Bếp đang làm</span><span class="sd-pill sd-pill--success">Chờ giao</span><span class="sd-pill sd-pill--neutral">Đã giao</span></span></div>
                <div class="takeaway-order-list" id="takeawayOrders"><div class="empty">Chưa có đơn mang về nào hôm nay.</div></div>
            </section>
        </section>
        <section id="detailPanel" class="detail-panel" aria-hidden="true" onclick="closeDetailOnBackdrop(event)">
            <div class="detail-dialog" role="dialog" aria-modal="true" aria-label="Chi tiết bàn">
                <button class="detail-close" type="button" title="Đóng chi tiết bàn" aria-label="Đóng chi tiết bàn" onclick="closeTableDetail()"><i class="fa-solid fa-xmark"></i></button>
                <div id="detailContent" class="empty empty-large">Chọn một bàn để xem trạng thái và các món đã gọi.</div>
            </div>
        </section>
        <section id="checkoutPanel" class="detail-panel checkout-panel" aria-hidden="true" onclick="closeCheckoutOnBackdrop(event)">
            <div class="detail-dialog checkout-dialog" role="dialog" aria-modal="true" aria-labelledby="checkoutTitle">
                <button class="detail-close" type="button" title="Đóng hóa đơn" aria-label="Đóng hóa đơn" onclick="closeCheckout()"><i class="fa-solid fa-xmark"></i></button>
                <div id="checkoutContent"></div>
            </div>
        </section>
    </main>
    <script>
        const contextPath = "${pageContext.request.contextPath}";
        const moneyFormatter = new Intl.NumberFormat('vi-VN');
        let staffSocket;
        let tableSummaries = [];
        let pendingRequests = [];
        let selectedTableId = null;
        let openRequestTableId = null;
        const tableAlerts = new Map();

        document.addEventListener('DOMContentLoaded', () => {
            loadDashboard();
            connectStaffWebSocket();
            window.setInterval(loadDashboard, 10000);
        });

        async function loadDashboard() {
            const results = await Promise.all([loadTables(), loadRequests()]);
            tableSummaries = results[0];
            pendingRequests = results[1];
            syncReadyToast();
            renderTables();
            if (selectedTableId !== null) {
                loadTableDetails(selectedTableId);
            }
        }

        async function loadTables() {
            try {
                const response = await fetch(contextPath + '/api/staff/tables');
                if (!response.ok) return [];
                const result = await response.json();
                return result.data || [];
            } catch (_) {
                return [];
            }
        }

        async function loadRequests() {
            try {
                const response = await fetch(contextPath + '/api/staff/requests');
                if (!response.ok) return [];
                const result = await response.json();
                return result.data || [];
            } catch (_) {
                return [];
            }
        }

        function renderTables() {
            const list = document.getElementById('tableList');
            list.replaceChildren();
            if (tableSummaries.length === 0) {
                list.appendChild(createEmpty('Chưa có bàn nào để theo dõi.'));
                return;
            }
            tableSummaries.forEach((table) => {
                const requests = requestsForTable(table.tableId);
                const row = document.createElement('article');
                row.tabIndex = 0;
                row.setAttribute('role', 'button');
                const tableAlertsForCard = getTableAlerts(table.tableId, requests);
                const occupancyClass = table.tableStatus === 'AVAILABLE' ? ' table-empty' : ' table-occupied';
                row.className = 'table-row' + occupancyClass
                    + ((table.readyCount || 0) > 0 ? ' has-ready' : '')
                    + (tableAlertsForCard.length > 0 ? ' has-request' : '')
                    + (table.tableId === selectedTableId ? ' active' : '');
                row.title = 'Mở chi tiết ' + table.tableName;
                row.addEventListener('click', () => selectTable(table.tableId));
                row.addEventListener('keydown', (event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        selectTable(table.tableId);
                    }
                });
                const copy = document.createElement('span');
                copy.className = 'table-card-main';
                const icon = document.createElement('span');
                icon.className = 'table-tile-icon';
                icon.innerHTML = '<i class="fa-solid fa-table-cells-large"></i>';
                const name = document.createElement('span');
                name.className = 'table-name';
                name.textContent = table.tableName;
                const meta = document.createElement('span');
                meta.className = 'table-meta';
                meta.textContent = tableMetaText(table, tableAlertsForCard);
                const textWrap = document.createElement('span');
                textWrap.className = 'table-copy';
                textWrap.append(name, meta);
                copy.append(icon, textWrap);
                if (tableAlertsForCard.length > 0) {
                    const indicator = document.createElement('button');
                    indicator.className = 'request-indicator';
                    indicator.type = 'button';
                    indicator.title = 'Xem yêu cầu của khách';
                    indicator.setAttribute('aria-label', 'Xem yêu cầu của khách');
                    indicator.innerHTML = '<i class="fa-solid fa-bell"></i>';
                    const count = document.createElement('span');
                    count.className = 'request-count';
                    count.textContent = tableAlertsForCard.length;
                    indicator.appendChild(count);
                    indicator.addEventListener('click', (event) => {
                        event.stopPropagation();
                        toggleTableRequests(table.tableId);
                    });
                    const alerts = document.createElement('div');
                    alerts.className = 'request-popover' + (openRequestTableId === table.tableId ? ' open' : '');
                    alerts.addEventListener('click', (event) => event.stopPropagation());
                    tableAlertsForCard.forEach((alert) => {
                    const alertItem = document.createElement('span');
                    alertItem.className = 'table-alert ' + alert.type;
                    alertItem.textContent = alert.message;
                    alerts.appendChild(alertItem);
                    });
                    row.append(copy, indicator, alerts);
                } else {
                    row.appendChild(copy);
                }
                list.appendChild(row);
            });
        }

        // Dong tom tat duoi ten ban: viec can lam truoc tien hien len dau
        function tableMetaText(table, alerts) {
            if (table.tableStatus === 'AVAILABLE' && !table.orderId) return 'Trống';
            if (alerts.some((alert) => alert.type === 'payment')) return 'Chờ thanh toán';
            if ((table.readyCount || 0) > 0) return table.readyCount + ' món chờ mang ra';
            if ((table.unservedCount || 0) > 0) return table.unservedCount + ' món đang làm';
            return moneyFormatter.format(Number(table.totalAmount || 0)) + 'đ';
        }

        function getTableAlerts(tableId, requests) {
            const persisted = requests.map((request) => ({
                type: request.requestType === 'PAYMENT' ? 'payment' : 'service',
                message: request.requestType === 'PAYMENT' ? 'Khách yêu cầu thanh toán'
                    : 'Khách gọi phục vụ' + (request.serviceType ? ': ' + request.serviceType : '')
                    + (request.note ? ' - ' + request.note : '')
            }));
            const realtime = tableAlerts.get(tableId) || [];
            const unique = new Map();
            realtime.concat(persisted).forEach((alert) => {
                const alertKey = alert.orderDetailId ? 'ready-' + alert.orderDetailId : alert.type + alert.message;
                unique.set(alertKey, alert);
            });
            return Array.from(unique.values()).slice(0, 3);
        }

        function selectTable(tableId) {
            selectedTableId = tableId;
            openRequestTableId = null;
            const readyAlerts = (tableAlerts.get(tableId) || []).filter((alert) => alert.type === 'ready');
            if (readyAlerts.length > 0) tableAlerts.set(tableId, readyAlerts);
            else tableAlerts.delete(tableId);
            renderTables();
            const panel = document.getElementById('detailPanel');
            panel.classList.add('open');
            panel.setAttribute('aria-hidden', 'false');
            loadTableDetails(tableId);
        }

        function closeTableDetail() {
            selectedTableId = null;
            const panel = document.getElementById('detailPanel');
            panel.classList.remove('open');
            panel.setAttribute('aria-hidden', 'true');
            renderTables();
        }

        function closeDetailOnBackdrop(event) {
            if (event.target === document.getElementById('detailPanel')) closeTableDetail();
        }

        function toggleTableRequests(tableId) {
            openRequestTableId = openRequestTableId === tableId ? null : tableId;
            renderTables();
        }

        async function loadTableDetails(tableId) {
            const selected = tableSummaries.find((table) => table.tableId === tableId);
            if (!selected) {
                closeTableDetail();
                return;
            }
            try {
                const response = await fetch(contextPath + '/api/staff/table-details?tableId=' + encodeURIComponent(tableId));
                if (!response.ok) throw new Error('Không lấy được chi tiết bàn');
                const result = await response.json();
                if (selectedTableId === tableId) renderTableDetails(result.data);
            } catch (_) {
                if (selectedTableId === tableId) renderTableDetails({ table: selected, items: [] });
            }
        }

        function renderTableDetails(data) {
            const table = data.table;
            const root = document.createElement('div');
            const heading = document.createElement('div');
            heading.className = 'detail-heading';
            const headingCopy = document.createElement('div');
            const title = document.createElement('h2');
            title.className = 'table-title';
            title.textContent = table.tableName;
            const subtitle = document.createElement('p');
            subtitle.className = 'table-subtitle';
            const tableIsEmpty = table.tableStatus === 'AVAILABLE';
            subtitle.textContent = tableIsEmpty ? 'Chưa có khách'
                : table.tableStatus === 'WAITING_PAYMENT' ? 'Đang chờ thanh toán' : 'Đang phục vụ';
            headingCopy.append(title, subtitle);
            heading.appendChild(headingCopy);
            if (!tableIsEmpty) {
                const amount = document.createElement('div');
                amount.className = 'amount-box';
                const amountLabel = document.createElement('span');
                amountLabel.className = 'amount-label';
                amountLabel.textContent = 'Tổng món đã xác nhận';
                const amountValue = document.createElement('strong');
                amountValue.className = 'amount-value';
                amountValue.textContent = formatMoney(table.totalAmount);
                amount.append(amountLabel, amountValue);
                heading.appendChild(amount);
            }
            root.appendChild(heading);

            if (!tableIsEmpty) {
                const statusStrip = document.createElement('div');
                statusStrip.className = 'status-strip';
                statusStrip.appendChild(createStatusChip((table.unservedCount || 0) + ' món chưa phục vụ'));
                statusStrip.appendChild(createStatusChip((table.readyCount || 0) + ' món chờ mang ra'));
                if (table.tableStatus === 'WAITING_PAYMENT') statusStrip.appendChild(createStatusChip('Yêu cầu thanh toán', 'waiting-payment'));
                root.appendChild(statusStrip);
            }

            const requests = requestsForTable(table.tableId);
            if (requests.length > 0) root.appendChild(createRequestSection(requests));
            root.appendChild(createOrderSection(data.items || []));
            replaceDetail(root);
        }

        function createStatusChip(text, additionalClass) {
            const chip = document.createElement('span');
            chip.className = 'status-chip' + (additionalClass ? ' ' + additionalClass : '');
            chip.textContent = text;
            return chip;
        }

        function createRequestSection(requests) {
            const section = document.createElement('section');
            section.className = 'detail-section';
            const heading = document.createElement('h3');
            heading.textContent = 'Yêu cầu của khách';
            const list = document.createElement('div');
            list.className = 'request-list';
            requests.forEach((request) => {
                const line = document.createElement('article');
                line.className = 'request-line';
                const copy = document.createElement('div');
                const title = document.createElement('div');
                title.className = 'request-title';
                title.textContent = request.requestType === 'PAYMENT' ? 'Yêu cầu thanh toán' : (request.serviceType || 'Gọi nhân viên');
                const meta = document.createElement('div');
                meta.className = 'request-meta';
                if (request.requestType === 'PAYMENT') {
                    meta.textContent = 'Tổng tạm tính: ' + formatMoney(request.totalAmount);
                } else {
                    meta.textContent = request.note || 'Không có ghi chú thêm.';
                }
                copy.append(title, meta);
                const button = document.createElement('button');
                button.className = 'action';
                button.type = 'button';
                const isPaymentRequest = request.requestType === 'PAYMENT';
                button.textContent = isPaymentRequest ? 'Thanh toán' : 'Đã xử lý';
                // Thanh toan mo hoa don chi tiet; yeu cau phuc vu thi danh dau da xu ly
                button.addEventListener('click', () => isPaymentRequest
                    ? openCheckout(request.tableId)
                    : completeRequest(request.id, false));
                line.append(copy, button);
                list.appendChild(line);
            });
            section.append(heading, list);
            return section;
        }

        function createOrderSection(items) {
            const section = document.createElement('section');
            section.className = 'detail-section';
            const heading = document.createElement('h3');
            heading.textContent = 'Món đã gọi';
            const list = document.createElement('div');
            list.className = 'order-list';
            const rank = { READY: 0, PREPARING: 1, PENDING: 2, CART: 3, SERVED: 4, CANCELLED: 5 };
            items.sort((left, right) => (rank[left.status] ?? 99) - (rank[right.status] ?? 99));
            if (items.length === 0) {
                list.appendChild(createEmpty('Chưa có món nào để hiển thị.'));
            } else {
                items.forEach((item) => list.appendChild(createOrderLine(item)));
            }
            section.append(heading, list);
            return section;
        }

        function createOrderLine(item) {
            const line = document.createElement('article');
            const statusClass = item.status === 'SERVED' ? 'served' : item.status === 'CART' ? 'cart'
                : item.status === 'CANCELLED' ? 'cancelled' : 'unserved';
            line.className = 'order-line ' + statusClass;
            const copy = document.createElement('div');
            const title = document.createElement('h4');
            title.className = 'item-title';
            title.textContent = item.productName + ' x' + item.quantity;
            const status = document.createElement('span');
            status.className = 'item-status';
            status.textContent = getStatusLabel(item.status);
            copy.append(title, status);
            if (item.note) {
                const note = document.createElement('div');
                note.className = 'item-note';
                note.textContent = 'Ghi chú: ' + item.note;
                copy.appendChild(note);
            }
            const actions = document.createElement('div');
            actions.className = 'item-actions';
            const price = document.createElement('span');
            price.className = 'item-price';
            price.textContent = formatMoney((item.unitPrice || 0) * (item.quantity || 0));
            actions.appendChild(price);
            if (item.status === 'READY') {
                const serve = document.createElement('button');
                serve.className = 'action';
                serve.type = 'button';
                serve.textContent = 'Đã phục vụ';
                serve.addEventListener('click', () => markServed(item.id));
                actions.appendChild(serve);
            }
            line.append(copy, actions);
            return line;
        }

        function getStatusLabel(status) {
            const labels = { CART: 'Chưa gửi bếp', PENDING: 'Chờ bếp nhận', PREPARING: 'Bếp đang chế biến', READY: 'Chờ phục vụ', SERVED: 'Đã phục vụ', CANCELLED: 'Đã hủy' };
            return labels[status] || status;
        }

        async function markServed(orderDetailId) {
            try {
                const response = await fetch(contextPath + '/api/staff/serve', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ orderDetailId: orderDetailId })
                });
                if (response.ok) {
                    removeReadyTableAlert(orderDetailId);
                    loadDashboard();
                }
            } catch (_) { }
        }

        async function completeRequest(requestId, closesTable) {
            try {
                const response = await fetch(contextPath + '/api/staff/complete-request', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ requestId: requestId })
                });
                if (response.ok) {
                    if (closesTable) closeTableDetail();
                    loadDashboard();
                }
            } catch (_) { }
        }

        function requestsForTable(tableId) { return pendingRequests.filter((request) => request.tableId === tableId); }
        function formatMoney(value) { return moneyFormatter.format(Number(value || 0)) + ' đ'; }
        function createEmpty(message) { const empty = document.createElement('div'); empty.className = 'empty'; empty.textContent = message; return empty; }
        function createEmptyDetail(message) { const empty = createEmpty(message); empty.id = 'detailContent'; empty.classList.add('empty-large'); return empty; }
        function replaceDetail(content) { content.id = 'detailContent'; document.getElementById('detailContent').replaceWith(content); }

        function connectStaffWebSocket() {
            const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
            staffSocket = new WebSocket(protocol + window.location.host + contextPath + '/ws/staff');
            staffSocket.onmessage = (event) => {
                try { handleStaffEvent(JSON.parse(event.data)); } catch (_) { }
            };
            staffSocket.onclose = () => window.setTimeout(connectStaffWebSocket, 3000);
        }

        function handleStaffEvent(payload) {
            const alertTypes = { ORDER_CONFIRMED: 'order', SERVICE_REQUEST: 'service', PAYMENT_REQUEST: 'payment' };
            const alertMessages = {
                ORDER_CONFIRMED: 'Khách gọi món',
                SERVICE_REQUEST: 'Khách gọi phục vụ' + (payload.type ? ': ' + payload.type : '') + (payload.note ? ' - ' + payload.note : ''),
                PAYMENT_REQUEST: 'Khách yêu cầu thanh toán'
            };
            if (alertTypes[payload.status] && payload.tableId) {
                addTableAlert(payload.tableId, alertTypes[payload.status], alertMessages[payload.status]);
            }
            if (payload.status === 'ITEM_READY' && payload.tableId) {
                const readyMessage = (payload.productName || 'Món ăn')
                    + (payload.quantity ? ' x' + payload.quantity : '')
                    + ' đã xong - mang ra ' + (payload.tableName || ('Bàn ' + payload.tableId));
                addTableAlert(payload.tableId, 'ready', readyMessage, payload.orderDetailId);
                showStaffToast(readyMessage);
            }
            // Don mang ve: bao mon xong / het mon (can hoan tien) va lam moi danh sach
            if (payload.orderType === 'TAKEAWAY' || payload.status === 'TAKEAWAY_UPDATED') {
                if (payload.status === 'ITEM_READY') {
                    showStaffToast((payload.productName || 'Món') + (payload.quantity ? ' x' + payload.quantity : '') + ' đã xong', payload.tableName || 'Đơn mang về');
                }
                if (payload.status === 'ITEM_CANCELLED' && payload.refundAmount) {
                    showStaffToast('Bếp báo hết món, cần hoàn ' + formatMoney(payload.refundAmount) + ' cho khách.', 'Đơn mang về');
                }
                loadTakeawayOrders();
                return;
            }
            if (payload.status === 'ITEM_SERVED' && payload.orderDetailId) {
                removeReadyTableAlert(payload.orderDetailId);
            }
            if (['ORDER_CONFIRMED', 'ITEM_READY', 'ITEM_SERVED', 'ITEM_CANCELLED', 'KITCHEN_TASK_UPDATED', 'PAYMENT_REQUEST', 'SERVICE_REQUEST', 'TABLE_REQUEST_COMPLETED', 'TABLE_STATUS_UPDATED', 'ORDER_PAID', 'TABLES_UPDATED'].includes(payload.status)) {
                loadDashboard();
            }
        }


        // ---------- Thanh toan: hoa don chi tiet + phuong thuc + anh bill ----------
        const checkoutState = { tableId: null, bill: null, method: 'CASH', image: null, submitting: false };

        async function openCheckout(tableId) {
            checkoutState.tableId = tableId;
            checkoutState.bill = null;
            checkoutState.method = 'CASH';
            checkoutState.image = null;
            const panel = document.getElementById('checkoutPanel');
            panel.classList.add('open');
            panel.setAttribute('aria-hidden', 'false');
            const content = document.getElementById('checkoutContent');
            content.innerHTML = '<div class="empty empty-large">Đang tải hóa đơn...</div>';
            try {
                const response = await fetch(contextPath + '/api/staff/bill?tableId=' + encodeURIComponent(tableId));
                const result = await response.json();
                if (!response.ok) throw new Error(result.message || 'Không tải được hóa đơn');
                checkoutState.bill = result.data;
                renderCheckout();
            } catch (error) {
                content.innerHTML = '';
                content.appendChild(createEmptyDetail(error.message));
            }
        }

        function closeCheckout() {
            const panel = document.getElementById('checkoutPanel');
            panel.classList.remove('open');
            panel.setAttribute('aria-hidden', 'true');
        }

        function closeCheckoutOnBackdrop(event) {
            if (event.target === document.getElementById('checkoutPanel')) closeCheckout();
        }

        function renderCheckout() {
            const bill = checkoutState.bill;
            const rows = bill.items.map((item) => '<tr>'
                + '<td>' + escapeText(item.productName) + (item.status !== 'SERVED' ? ' <span class="bill-flag">' + getStatusLabel(item.status) + '</span>' : '') + '</td>'
                + '<td class="num">' + item.quantity + '</td>'
                + '<td class="num">' + formatMoney(item.unitPrice) + '</td>'
                + '<td class="num">' + formatMoney(item.lineTotal) + '</td></tr>').join('');
            const blockers = [];
            if (!bill.paymentRequested) blockers.push('Khách chưa bấm "Yêu cầu thanh toán".');
            if (bill.unservedCount > 0) blockers.push('Còn ' + bill.unservedCount + ' món chưa phục vụ.');
            const startedAt = bill.startTime ? new Date(bill.startTime).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) : '';

            document.getElementById('checkoutContent').innerHTML = ''
                + '<div class="checkout-head">'
                + '  <p class="checkout-kicker">Hóa đơn thanh toán</p>'
                + '  <h2 class="table-title" id="checkoutTitle">' + escapeText(bill.tableName) + '</h2>'
                + '  <p class="table-subtitle">Mã HĐ ' + escapeText(bill.orderId) + (startedAt ? ' · mở lúc ' + startedAt : '') + '</p>'
                + '</div>'
                + '<table class="bill-table"><thead><tr><th>Món</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr></thead>'
                + '<tbody>' + (rows || '<tr><td colspan="4" class="bill-empty">Chưa có món nào được gửi bếp.</td></tr>') + '</tbody>'
                + '<tfoot><tr><td colspan="3">Tổng cộng</td><td class="num">' + formatMoney(bill.total) + '</td></tr></tfoot></table>'
                + (blockers.length ? '<div class="checkout-warning" role="alert"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i><div><strong>Chưa thể thanh toán</strong>' + blockers.map((text) => '<span>' + text + '</span>').join('') + '</div></div>' : '')
                + '<fieldset class="pay-methods"' + (blockers.length ? ' disabled' : '') + '><legend>Phương thức thanh toán</legend>'
                + '  <label class="pay-method"><input type="radio" name="payMethod" value="CASH" checked><span><i class="fa-solid fa-money-bill-wave" aria-hidden="true"></i><strong>Tiền mặt</strong><small>Không cần ảnh</small></span></label>'
                + '  <label class="pay-method"><input type="radio" name="payMethod" value="TRANSFER"><span><i class="fa-solid fa-building-columns" aria-hidden="true"></i><strong>Chuyển khoản</strong><small>Bắt buộc chụp bill</small></span></label>'
                + '</fieldset>'
                + '<div class="proof-box" id="proofBox" hidden>'
                + '  <label class="proof-picker" for="proofInput"><i class="fa-solid fa-camera" aria-hidden="true"></i><span id="proofLabel">Chụp hoặc chọn ảnh bill chuyển khoản</span></label>'
                + '  <input type="file" id="proofInput" accept="image/*" capture="environment" hidden>'
                + '  <img id="proofPreview" class="proof-preview" alt="Ảnh bill chuyển khoản" hidden>'
                + '</div>'
                + '<p class="checkout-cashier"><i class="fa-solid fa-user-check" aria-hidden="true"></i> Nhân viên phụ trách: <strong>' + escapeText(bill.cashier || '') + '</strong></p>'
                + '<p class="checkout-error" id="checkoutError" role="alert" hidden></p>'
                + '<div class="checkout-actions"><button type="button" class="secondary-action" onclick="closeCheckout()">Hủy</button>'
                + '<button type="button" class="action" id="checkoutSubmit" onclick="submitCheckout()" ' + (blockers.length ? 'disabled' : '') + '><i class="fa-solid fa-check" aria-hidden="true"></i> Hoàn tất thanh toán ' + formatMoney(bill.total) + '</button></div>';

            document.querySelectorAll('input[name="payMethod"]').forEach((radio) => radio.addEventListener('change', () => {
                checkoutState.method = radio.value;
                document.getElementById('proofBox').hidden = radio.value !== 'TRANSFER';
                setCheckoutError('');
            }));
            document.getElementById('proofInput').addEventListener('change', async (event) => {
                const file = event.target.files[0];
                if (!file) return;
                try {
                    checkoutState.image = await compressImage(file);
                    const preview = document.getElementById('proofPreview');
                    preview.src = checkoutState.image;
                    preview.hidden = false;
                    document.getElementById('proofLabel').textContent = 'Chụp lại ảnh khác';
                    setCheckoutError('');
                } catch (_) {
                    checkoutState.image = null;
                    setCheckoutError('Không đọc được ảnh, vui lòng chụp lại.');
                }
            });
        }

        // Nen anh ve toi da 1280px, JPEG chat luong 0.8 de luu vao DB nhe hon
        async function compressImage(file) {
            const bitmap = await createImageBitmap(file);
            const scale = Math.min(1, 1280 / Math.max(bitmap.width, bitmap.height));
            const canvas = document.createElement('canvas');
            canvas.width = Math.round(bitmap.width * scale);
            canvas.height = Math.round(bitmap.height * scale);
            canvas.getContext('2d').drawImage(bitmap, 0, 0, canvas.width, canvas.height);
            return canvas.toDataURL('image/jpeg', 0.8);
        }

        function setCheckoutError(message) {
            const error = document.getElementById('checkoutError');
            if (!error) return;
            error.textContent = message;
            error.hidden = !message;
        }

        async function submitCheckout() {
            if (checkoutState.submitting) return;
            if (checkoutState.method === 'TRANSFER' && !checkoutState.image) {
                setCheckoutError('Chuyển khoản cần chụp ảnh bill trước khi hoàn tất.');
                return;
            }
            const button = document.getElementById('checkoutSubmit');
            checkoutState.submitting = true;
            button.disabled = true;
            try {
                const response = await fetch(contextPath + '/api/staff/checkout', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ tableId: checkoutState.tableId, method: checkoutState.method, image: checkoutState.method === 'TRANSFER' ? checkoutState.image : null })
                });
                const result = await response.json();
                if (!response.ok) throw new Error(result.message || 'Không thể thanh toán');
                closeCheckout();
                closeTableDetail();
                showStaffToast(checkoutState.bill.tableName + ' — ' + formatMoney(checkoutState.bill.total) + (checkoutState.method === 'TRANSFER' ? ' (chuyển khoản)' : ' (tiền mặt)'), 'Đã thanh toán');
                loadDashboard();
            } catch (error) {
                setCheckoutError(error.message);
                button.disabled = false;
            } finally {
                checkoutState.submitting = false;
            }
        }

        function escapeText(value) {
            const span = document.createElement('span');
            span.textContent = value == null ? '' : String(value);
            return span.innerHTML;
        }


        // ---------- Ban mang ve: chon mon -> thanh toan truoc -> gui bep -> giao do ----------
        const takeawayState = { menu: [], category: '', search: '', cart: [], method: 'CASH', image: null, submitting: false, loaded: false };

        function showStaffView(view) {
            const takeaway = view === 'takeaway';
            document.getElementById('tablePanel').hidden = takeaway;
            document.getElementById('takeawayPanel').hidden = !takeaway;
            document.querySelectorAll('[data-staff-view]').forEach((button) => {
                button.classList.toggle('active', button.dataset.staffView === view);
                button.setAttribute('aria-pressed', String(button.dataset.staffView === view));
            });
            try { window.localStorage.setItem('smartDining.staffView', view); } catch (_) { }
            if (takeaway) {
                if (!takeawayState.loaded) loadTakeawayMenu();
                loadTakeawayOrders();
            }
        }

        function refreshCurrentView() {
            if (document.getElementById('takeawayPanel').hidden) {
                loadDashboard();
            } else {
                loadTakeawayMenu();
                loadTakeawayOrders();
            }
        }

        async function loadTakeawayMenu() {
            try {
                const response = await fetch(contextPath + '/api/staff/menu');
                const result = await response.json();
                takeawayState.menu = result.data || [];
                takeawayState.loaded = true;
                renderTakeawayMenu();
            } catch (_) {
                document.getElementById('takeawayProducts').innerHTML = '<div class="empty">Không tải được thực đơn.</div>';
            }
        }

        function normalizeText(value) {
            return String(value || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
        }

        function renderTakeawayMenu() {
            const categories = [];
            takeawayState.menu.forEach((product) => {
                if (product.categoryId != null && !categories.some((item) => item.id === product.categoryId)) {
                    categories.push({ id: product.categoryId, name: product.categoryName });
                }
            });
            document.getElementById('takeawayCategories').innerHTML = [{ id: '', name: 'Tất cả' }].concat(categories)
                .map((category) => '<button type="button" class="takeaway-category' + (String(takeawayState.category) === String(category.id) ? ' is-active' : '')
                    + '" data-category="' + escapeText(category.id) + '">' + escapeText(category.name) + '</button>').join('');
            const keyword = normalizeText(takeawayState.search);
            const products = takeawayState.menu.filter((product) =>
                (!takeawayState.category || String(product.categoryId) === String(takeawayState.category))
                && (!keyword || normalizeText(product.name).includes(keyword)));
            document.getElementById('takeawayProducts').innerHTML = products.length ? products.map((product) => {
                const line = takeawayState.cart.find((item) => item.productId === product.id);
                const image = product.imageUrl ? '<img src="' + escapeText(product.imageUrl) + '" alt="" loading="lazy" onerror="this.remove()">' : '';
                return '<button type="button" class="takeaway-product' + (line ? ' is-picked' : '') + '" data-product="' + escapeText(product.id) + '">'
                    + '<span class="takeaway-thumb">' + image + '<i class="fa-solid fa-bowl-food" aria-hidden="true"></i></span>'
                    + '<span class="takeaway-product-name">' + escapeText(product.name) + '</span>'
                    + '<span class="takeaway-product-price">' + formatMoney(product.price) + (line ? ' · ×' + line.quantity : '') + '</span></button>';
            }).join('') : '<div class="empty">Không có món nào phù hợp.</div>';
        }

        function addTakeawayItem(productId) {
            const product = takeawayState.menu.find((item) => item.id === productId);
            if (!product) return;
            const line = takeawayState.cart.find((item) => item.productId === productId);
            if (line) {
                line.quantity = Math.min(100, line.quantity + 1);
            } else {
                takeawayState.cart.push({ productId, name: product.name, price: Number(product.price || 0), quantity: 1, note: '' });
            }
            renderTakeawayCart();
            renderTakeawayMenu();
        }

        function changeTakeawayQty(productId, delta) {
            const line = takeawayState.cart.find((item) => item.productId === productId);
            if (!line) return;
            line.quantity += delta;
            if (line.quantity <= 0) {
                takeawayState.cart = takeawayState.cart.filter((item) => item.productId !== productId);
            }
            renderTakeawayCart();
            renderTakeawayMenu();
        }

        function takeawayTotal() {
            return takeawayState.cart.reduce((sum, line) => sum + line.price * line.quantity, 0);
        }

        function renderTakeawayCart() {
            const lines = document.getElementById('takeawayLines');
            lines.innerHTML = takeawayState.cart.length ? takeawayState.cart.map((line) => '<div class="takeaway-line">'
                + '<div class="takeaway-line-main"><strong>' + escapeText(line.name) + '</strong><span>' + formatMoney(line.price * line.quantity) + '</span></div>'
                + '<div class="takeaway-line-tools"><span class="qty-stepper">'
                + '<button type="button" data-qty="-1" data-line="' + escapeText(line.productId) + '" aria-label="Giảm số lượng"><i class="fa-solid fa-minus"></i></button>'
                + '<b>' + line.quantity + '</b>'
                + '<button type="button" data-qty="1" data-line="' + escapeText(line.productId) + '" aria-label="Tăng số lượng"><i class="fa-solid fa-plus"></i></button></span>'
                + '<input class="takeaway-note" data-note="' + escapeText(line.productId) + '" maxlength="255" placeholder="Ghi chú cho bếp" value="' + escapeText(line.note) + '"></div></div>').join('')
                : '<p class="takeaway-empty">Chọn món bên trái để thêm vào đơn.</p>';
            document.getElementById('takeawayTotal').textContent = formatMoney(takeawayTotal());
            document.getElementById('takeawaySubmit').disabled = !takeawayState.cart.length;
            document.getElementById('takeawaySubmit').innerHTML = '<i class="fa-solid fa-fire-burner" aria-hidden="true"></i> Thanh toán ' + formatMoney(takeawayTotal()) + ' & gửi bếp';
        }

        function setTakeawayError(message) {
            const error = document.getElementById('takeawayError');
            error.textContent = message;
            error.hidden = !message;
        }

        function resetTakeawayForm() {
            takeawayState.cart = [];
            takeawayState.image = null;
            ['takeawayName', 'takeawayPhone', 'takeawayPickup'].forEach((id) => { document.getElementById(id).value = ''; });
            document.querySelector('input[name="takeawayMethod"][value="CASH"]').checked = true;
            takeawayState.method = 'CASH';
            document.getElementById('takeawayProofBox').hidden = true;
            document.getElementById('takeawayProofPreview').hidden = true;
            document.getElementById('takeawayProofLabel').textContent = 'Chụp hoặc chọn ảnh bill chuyển khoản';
            setTakeawayError('');
            renderTakeawayCart();
            renderTakeawayMenu();
        }

        async function submitTakeaway() {
            if (takeawayState.submitting || !takeawayState.cart.length) return;
            if (takeawayState.method === 'TRANSFER' && !takeawayState.image) {
                setTakeawayError('Chuyển khoản cần chụp ảnh bill trước khi gửi bếp.');
                return;
            }
            const button = document.getElementById('takeawaySubmit');
            takeawayState.submitting = true;
            button.disabled = true;
            try {
                const response = await fetch(contextPath + '/api/staff/takeaway', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        items: takeawayState.cart.map((line) => ({ productId: line.productId, quantity: line.quantity, note: line.note })),
                        customerName: document.getElementById('takeawayName').value,
                        customerPhone: document.getElementById('takeawayPhone').value,
                        pickupTime: document.getElementById('takeawayPickup').value,
                        method: takeawayState.method,
                        image: takeawayState.method === 'TRANSFER' ? takeawayState.image : null
                    })
                });
                const result = await response.json();
                if (!response.ok) throw new Error(result.message || 'Không thể tạo đơn mang về');
                showStaffToast('Mã lấy đồ #' + result.data.pickupCode + ' — ' + formatMoney(result.data.total) + ', đã gửi bếp.', 'Đã tạo đơn mang về');
                resetTakeawayForm();
                loadTakeawayOrders();
            } catch (error) {
                setTakeawayError(error.message);
                button.disabled = false;
            } finally {
                takeawayState.submitting = false;
            }
        }

        async function loadTakeawayOrders() {
            if (document.getElementById('takeawayPanel').hidden) return;
            try {
                const response = await fetch(contextPath + '/api/staff/takeaways');
                const result = await response.json();
                renderTakeawayOrders(result.data || []);
            } catch (_) { }
        }

        function renderTakeawayOrders(orders) {
            const container = document.getElementById('takeawayOrders');
            if (!orders.length) {
                container.innerHTML = '<div class="empty">Chưa có đơn mang về nào hôm nay.</div>';
                return;
            }
            const order = { ready: 0, preparing: 1, delivered: 2 };
            const stateLabel = { preparing: ['Bếp đang làm', 'warning'], ready: ['Chờ giao', 'success'], delivered: ['Đã giao', 'neutral'] };
            const time = (value) => value ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
            container.innerHTML = orders.slice().sort((first, second) => order[first.state] - order[second.state]).map((item) => {
                const [label, tone] = stateLabel[item.state];
                const customer = [item.customerName, item.customerPhone].filter(Boolean).map(escapeText).join(' · ');
                const refund = Number(item.refundAmount || 0) > 0
                    ? '<p class="takeaway-refund"><i class="fa-solid fa-rotate-left" aria-hidden="true"></i> Bếp báo hết món — hoàn ' + formatMoney(item.refundAmount) + ' cho khách</p>' : '';
                return '<article class="takeaway-order is-' + item.state + '">'
                    + '<div class="takeaway-order-head"><strong class="pickup-code">#' + item.pickupCode + '</strong>'
                    + '<span class="sd-pill sd-pill--' + tone + '">' + label + '</span></div>'
                    + '<p class="takeaway-order-meta">' + (customer || 'Khách lẻ') + ' · đặt ' + time(item.createdTime)
                    + (item.pickupTime ? ' · lấy ' + time(item.pickupTime) : ' · lấy ngay') + '</p>'
                    + '<ul>' + item.items.map((line) => '<li class="' + (line.status === 'CANCELLED' ? 'is-cancelled' : '') + '">'
                        + line.quantity + ' × ' + escapeText(line.productName) + ' <span>' + getStatusLabel(line.status) + '</span></li>').join('') + '</ul>'
                    + refund
                    + '<div class="takeaway-order-foot"><span>' + formatMoney(item.totalAmount) + '</span>'
                    + (item.state === 'ready' ? '<button class="action" type="button" data-deliver="' + escapeText(item.orderId) + '"><i class="fa-solid fa-hand-holding" aria-hidden="true"></i> Đã giao</button>' : '')
                    + '</div></article>';
            }).join('');
        }

        async function deliverTakeaway(orderId, button) {
            button.disabled = true;
            try {
                const response = await fetch(contextPath + '/api/staff/takeaway/deliver', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ orderId })
                });
                const result = await response.json();
                if (!response.ok) throw new Error(result.message);
                loadTakeawayOrders();
            } catch (error) {
                showStaffToast(error.message || 'Không thể giao đơn', 'Lỗi');
                button.disabled = false;
            }
        }

        document.addEventListener('DOMContentLoaded', () => {
            document.querySelectorAll('[data-staff-view]').forEach((button) => button.addEventListener('click', () => showStaffView(button.dataset.staffView)));
            document.getElementById('takeawaySearch').addEventListener('input', (event) => { takeawayState.search = event.target.value; renderTakeawayMenu(); });
            document.getElementById('takeawayCategories').addEventListener('click', (event) => {
                const button = event.target.closest('[data-category]');
                if (!button) return;
                takeawayState.category = button.dataset.category;
                renderTakeawayMenu();
            });
            document.getElementById('takeawayProducts').addEventListener('click', (event) => {
                const button = event.target.closest('[data-product]');
                if (button) addTakeawayItem(button.dataset.product);
            });
            document.getElementById('takeawayLines').addEventListener('click', (event) => {
                const button = event.target.closest('[data-qty]');
                if (button) changeTakeawayQty(button.dataset.line, Number(button.dataset.qty));
            });
            document.getElementById('takeawayLines').addEventListener('input', (event) => {
                const line = takeawayState.cart.find((item) => item.productId === event.target.dataset.note);
                if (line) line.note = event.target.value;
            });
            document.getElementById('takeawayClear').addEventListener('click', resetTakeawayForm);
            document.getElementById('takeawaySubmit').addEventListener('click', submitTakeaway);
            document.querySelectorAll('input[name="takeawayMethod"]').forEach((radio) => radio.addEventListener('change', () => {
                takeawayState.method = radio.value;
                document.getElementById('takeawayProofBox').hidden = radio.value !== 'TRANSFER';
                setTakeawayError('');
            }));
            document.getElementById('takeawayProof').addEventListener('change', async (event) => {
                const file = event.target.files[0];
                if (!file) return;
                try {
                    takeawayState.image = await compressImage(file);
                    const preview = document.getElementById('takeawayProofPreview');
                    preview.src = takeawayState.image;
                    preview.hidden = false;
                    document.getElementById('takeawayProofLabel').textContent = 'Chụp lại ảnh khác';
                    setTakeawayError('');
                } catch (_) {
                    takeawayState.image = null;
                    setTakeawayError('Không đọc được ảnh, vui lòng chụp lại.');
                }
            });
            document.getElementById('takeawayOrders').addEventListener('click', (event) => {
                const button = event.target.closest('[data-deliver]');
                if (button) deliverTakeaway(button.dataset.deliver, button);
            });
            renderTakeawayCart();
            let savedView = 'tables';
            try { savedView = window.localStorage.getItem('smartDining.staffView') || 'tables'; } catch (_) { }
            if (savedView === 'takeaway') showStaffView('takeaway');
            window.setInterval(loadTakeawayOrders, 15000);
        });

        let staffToastTimer = null;
        function showStaffToast(message, title) {
            const toast = document.getElementById('staffToast');
            document.getElementById('staffToastTitle').textContent = title || 'Bếp đã hoàn tất món';
            document.getElementById('staffToastMessage').textContent = message;
            toast.classList.add('show');
            window.clearTimeout(staffToastTimer);
            // Thong bao thanh toan tu an; thong bao mon xong van do syncReadyToast quan ly
            if (title) staffToastTimer = window.setTimeout(() => toast.classList.remove('show'), 5000);
        }

        function hideStaffToast() {
            document.getElementById('staffToast').classList.remove('show');
        }

        function syncReadyToast() {
            const readyCount = tableSummaries.reduce((total, table) => total + Number(table.readyCount || 0), 0);
            if (readyCount === 0) {
                hideStaffToast();
                return;
            }

            const activeReadyAlert = Array.from(tableAlerts.values())
                .flat()
                .find((alert) => alert.type === 'ready');
            const message = activeReadyAlert?.message
                || 'Có ' + readyCount + ' món đã xong, vui lòng mang ra cho khách.';
            showStaffToast(message);
        }

        function addTableAlert(tableId, type, message, orderDetailId) {
            const alerts = tableAlerts.get(tableId) || [];
            const exists = orderDetailId
                ? alerts.some((alert) => String(alert.orderDetailId) === String(orderDetailId))
                : alerts.some((alert) => alert.type === type && alert.message === message);
            if (!exists) {
                alerts.unshift({ type: type, message: message, orderDetailId: orderDetailId });
                tableAlerts.set(tableId, alerts.slice(0, 3));
            }
            renderTables();
        }

        function removeReadyTableAlert(orderDetailId) {
            tableAlerts.forEach((alerts, tableId) => {
                const remaining = alerts.filter((alert) => String(alert.orderDetailId) !== String(orderDetailId));
                if (remaining.length > 0) tableAlerts.set(tableId, remaining);
                else tableAlerts.delete(tableId);
            });
        }
    </script>
</body>
</html>
