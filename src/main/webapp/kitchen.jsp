<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Màn hình bếp - SmartDining</title>
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/kitchen.css?v=20261012-6">
</head>
<body>

    <div class="header">
        <h1><i class="fa-solid fa-fire-burner"></i> Màn hình bếp</h1>
        <div class="user-info">
            <span class="kds-clock" id="kdsClock" aria-label="Giờ hiện tại"></span>
            <span><i class="fa-solid fa-user"></i> ${requestScope.employee.fullName}</span>
            <a href="${pageContext.request.contextPath}/logout?role=KITCHEN" class="logout-btn">Đăng xuất</a>
        </div>
    </div>

    <div class="layout-container">
        <!-- Sidebar -->
        <div class="sidebar">
            <div class="kitchen-sidebar-heading">
                <div class="kitchen-sidebar-title"><i class="fa-solid fa-clipboard-list"></i><span>Đơn cần xử lý</span></div>
                <button class="sidebar-collapse" id="kitchenSidebarToggle" type="button" aria-pressed="false" title="Thu gọn danh sách bàn" aria-label="Thu gọn danh sách bàn"><i class="fa-solid fa-angles-left"></i></button>
            </div>
            <div class="sidebar-scroll">
                <div class="tier-title tier-title-active"><span>Đơn cần xử lý</span><span class="tier-count" id="activeTableCount">0</span></div>
                <div id="activeTablesList"></div>
                
                <div class="sidebar-divider" aria-hidden="true"><span></span><i class="fa-solid fa-arrow-down"></i><span></span></div>
                <div class="tier-title"><span>Chưa có món cần xử lý</span><span class="tier-count tier-count-muted" id="idleTableCount">0</span></div>
                <div id="emptyTablesList"></div>
            </div>
        </div>

        <!-- Main Content -->
        <div class="main-content">
            <div class="main-header">
                <h2 id="selectedTableTitle">Tất cả món</h2>
                <div id="selectedTableStatus" class="table-status"></div>
            </div>
            <div class="kds-toolbar">
                <div class="group-chips" id="groupChips" aria-label="Chọn bàn hoặc đơn mang về"></div>
                <div class="status-tabs" id="statusTabs" role="tablist" aria-label="Lọc theo trạng thái">
                    <button type="button" role="tab" data-status="ALL" class="is-active">Tất cả <span>0</span></button>
                    <button type="button" role="tab" data-status="PENDING">Chờ làm <span>0</span></button>
                    <button type="button" role="tab" data-status="PREPARING">Đang nấu <span>0</span></button>
                </div>
            </div>
            
            <div class="tasks-container" id="tasksContainer">
                <div class="empty-state">
                    <i class="fa-solid fa-utensils"></i>
                    <p>Đang tải danh sách món...</p>
                </div>
            </div>
        </div>
    </div>

    <script>
        const contextPath = "${pageContext.request.contextPath}";
        let allTasks = [];
        let taskRefreshSequence = 0;
        let taskRefreshTimer = null;
        
        // Danh sach ban lay tu /api/kitchen/tables (tai lai khi admin them / doi ten / ngung ban)
        let serverTables = [];

        async function loadTables() {
            try {
                const res = await fetch(contextPath + '/api/kitchen/tables');
                if (!res.ok) return;
                const json = await res.json();
                serverTables = Array.isArray(json.data) ? json.data : [];
                renderTaskViews();
            } catch (error) {
                console.error('Cannot load kitchen tables', error);
            }
        }

        const socketStatus = {
            NEW_ORDER: 'NEW_ORDER',
            KITCHEN_TASK_UPDATED: 'KITCHEN_TASK_UPDATED',
            TABLES_UPDATED: 'TABLES_UPDATED'
        };

        const beepSound = new Audio("data:audio/wav;base64,UklGRl9vT19XQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YU"+Array(100).join("a")+"..."); 
        
        function playBeep() {
            try {
                const ctx = new (window.AudioContext || window.webkitAudioContext)();
                const osc = ctx.createOscillator();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(880, ctx.currentTime);
                osc.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 0.2);
            } catch (e) {
                console.log("Audio not supported");
            }
        }

        async function fetchTasks() {
            const requestSequence = ++taskRefreshSequence;
            try {
                const res = await fetch(contextPath + '/api/kitchen/tasks');
                if (!res.ok) {
                    if (res.status === 401 || res.status === 403) {
                        window.location.href = contextPath + '/login';
                        return;
                    }
                    throw new Error('Cannot load kitchen tasks: HTTP ' + res.status);
                }
                const json = await res.json();

                // A newer WebSocket event can arrive while this request is pending.
                if (requestSequence !== taskRefreshSequence) {
                    return;
                }

                if (json.status === 'success') {
                    allTasks = json.data;
                    renderTaskViews();
                } else if (json.status === 'error' && json.message === 'Unauthorized') {
                    window.location.href = contextPath + '/login';
                }
            } catch (e) {
                console.error(e);
            }
        }

        // ---------- Hien thi: nhom mon theo ban / don mang ve ----------
        // selectedGroup: 'all' = moi mon cua moi ban, hoac groupKey ('table-3', 'order-HD-...')
        let selectedGroup = 'all';
        let statusFilter = 'ALL';

        function groupKeyOf(task) {
            return task.groupKey || ('table-' + task.tableId);
        }

        function groupNameOf(task) {
            if (task.groupName) return task.groupName;
            const table = serverTables.find((item) => String(item.id) === String(task.tableId));
            return table ? table.name : 'Bàn ' + task.tableId;
        }

        function waitedMinutes(task) {
            return task.createdTime ? Math.max(0, Math.floor((Date.now() - new Date(task.createdTime)) / 60000)) : 0;
        }

        function renderTaskViews() {
            const groups = buildGroups();
            if (selectedGroup !== 'all' && !groups.some((group) => group.key === selectedGroup && group.pending + group.preparing > 0)) {
                selectedGroup = 'all';
            }
            renderSidebar(groups);
            renderGroupChips(groups);
            renderStatusTabs();
            renderTasks();
        }

        function scheduleTaskRefresh(invalidatePendingRequest = true) {
            if (invalidatePendingRequest) {
                taskRefreshSequence++;
            }
            window.clearTimeout(taskRefreshTimer);
            taskRefreshTimer = window.setTimeout(fetchTasks, 120);
        }

        function mergeNewTasks(tasks) {
            taskRefreshSequence++;

            const tasksById = new Map(allTasks.map(task => [String(task.id), task]));
            (Array.isArray(tasks) ? tasks : []).forEach(task => {
                if (task && task.id != null) {
                    tasksById.set(String(task.id), task);
                }
            });

            allTasks = Array.from(tasksById.values())
                .filter(task => task.status === 'PENDING' || task.status === 'PREPARING');
            renderTaskViews();
            scheduleTaskRefresh(false);
        }

        /** Nhom = moi ban (ke ca ban chua co mon) + moi don mang ve dang co mon. */
        function buildGroups() {
            const groups = new Map(serverTables.map((table, index) => ['table-' + table.id, {
                key: 'table-' + table.id, name: table.name, takeaway: false, order: index,
                pending: 0, preparing: 0, oldest: Number.POSITIVE_INFINITY
            }]));
            allTasks.forEach((task) => {
                const key = groupKeyOf(task);
                if (!groups.has(key)) {
                    groups.set(key, { key, name: groupNameOf(task), takeaway: task.orderType === 'TAKEAWAY',
                        pickupTime: task.pickupTime, order: 1000, pending: 0, preparing: 0, oldest: Number.POSITIVE_INFINITY });
                }
                const group = groups.get(key);
                if (task.status === 'PENDING') group.pending++;
                if (task.status === 'PREPARING') group.preparing++;
                const created = Date.parse(task.createdTime || '');
                if (Number.isFinite(created)) group.oldest = Math.min(group.oldest, created);
            });
            return Array.from(groups.values());
        }

        function renderSidebar(groups) {
            const activeList = document.getElementById('activeTablesList');
            const idleList = document.getElementById('emptyTablesList');
            const active = groups.filter((group) => group.pending + group.preparing > 0)
                .sort((first, second) => first.oldest - second.oldest || first.order - second.order);
            const idle = groups.filter((group) => group.pending + group.preparing === 0 && !group.takeaway)
                .sort((first, second) => first.order - second.order);
            document.getElementById('activeTableCount').textContent = String(active.length);
            document.getElementById('idleTableCount').textContent = String(idle.length);
            activeList.innerHTML = '';
            idleList.innerHTML = '';
            if (active.length) {
                activeList.appendChild(createGroupButton({ key: 'all', name: 'Tất cả món', pending: allTasks.filter((t) => t.status === 'PENDING').length,
                    preparing: allTasks.filter((t) => t.status === 'PREPARING').length, all: true }, true));
            }
            active.forEach((group) => activeList.appendChild(createGroupButton(group, true)));
            idle.forEach((group) => idleList.appendChild(createGroupButton(group, false)));
            if (!active.length) {
                activeList.innerHTML = '<p class="sidebar-empty-state">Chưa có món nào cần bếp xử lý.</p>';
            }
        }

        function createGroupButton(group, isActive) {
            const item = document.createElement('button');
            item.type = 'button';
            item.title = group.name;
            item.className = 'table-item' + (isActive ? '' : ' disabled')
                + (group.pending > 0 && !group.all ? ' new-order-alert' : '')
                + (selectedGroup === group.key ? ' active-select' : '');
            if (isActive) {
                item.addEventListener('click', () => selectGroup(group.key));
            } else {
                item.disabled = true;
            }
            const icon = group.all ? 'fa-layer-group' : group.takeaway ? 'fa-bag-shopping' : 'fa-receipt';
            let badge = '';
            if (group.pending > 0) badge = '<div class="table-badge">' + group.pending + ' món mới</div>';
            else if (group.preparing > 0) badge = '<div class="table-badge is-cooking">' + group.preparing + ' đang nấu</div>';
            item.innerHTML = '<div class="table-info"><div class="table-icon"><i class="fa-solid ' + icon + '"></i></div>'
                + '<div class="table-name">' + escapeHtml(group.name) + '</div></div>' + badge;
            return item;
        }

        /** Thanh chon nhom cuon ngang (hien tren dien thoai thay cho cot ben trai). */
        function renderGroupChips(groups) {
            const active = groups.filter((group) => group.pending + group.preparing > 0)
                .sort((first, second) => first.oldest - second.oldest || first.order - second.order);
            const chips = [{ key: 'all', name: 'Tất cả', count: allTasks.length }]
                .concat(active.map((group) => ({ key: group.key, name: group.name, count: group.pending + group.preparing,
                    late: group.oldest < Date.now() - 20 * 60000, takeaway: group.takeaway })));
            document.getElementById('groupChips').innerHTML = chips.map((chip) => '<button type="button" class="group-chip'
                + (selectedGroup === chip.key ? ' is-active' : '') + (chip.late ? ' is-late' : '') + '" data-group="' + escapeHtml(chip.key) + '">'
                + (chip.takeaway ? '<i class="fa-solid fa-bag-shopping" aria-hidden="true"></i> ' : '')
                + escapeHtml(chip.name) + ' <span>' + chip.count + '</span></button>').join('');
        }

        function renderStatusTabs() {
            const scope = tasksInGroup();
            const counts = { ALL: scope.length, PENDING: scope.filter((t) => t.status === 'PENDING').length,
                PREPARING: scope.filter((t) => t.status === 'PREPARING').length };
            document.querySelectorAll('#statusTabs [data-status]').forEach((tab) => {
                tab.classList.toggle('is-active', tab.dataset.status === statusFilter);
                tab.setAttribute('aria-selected', String(tab.dataset.status === statusFilter));
                tab.querySelector('span').textContent = counts[tab.dataset.status];
            });
        }

        function tasksInGroup() {
            return selectedGroup === 'all' ? allTasks.slice() : allTasks.filter((task) => groupKeyOf(task) === selectedGroup);
        }

        function selectGroup(key) {
            selectedGroup = key;
            renderTaskViews();
        }

        function renderTasks() {
            const container = document.getElementById('tasksContainer');
            const showAll = selectedGroup === 'all';
            const tasks = tasksInGroup()
                .filter((task) => statusFilter === 'ALL' || task.status === statusFilter)
                // Mon cho lau nhat len dau
                .sort((first, second) => new Date(first.createdTime) - new Date(second.createdTime));
            const groupName = showAll ? 'Tất cả món' : (tasks[0] ? groupNameOf(tasks[0]) : (buildGroups().find((g) => g.key === selectedGroup) || {}).name);
            document.getElementById('selectedTableTitle').textContent = groupName || 'Tất cả món';
            const waiting = tasks.filter((task) => task.status === 'PENDING').length;
            document.getElementById('selectedTableStatus').textContent = tasks.length
                ? tasks.length + ' món · ' + waiting + ' chờ làm' : '';

            if (!tasks.length) {
                container.innerHTML = '<div class="empty-state"><i class="fa-solid fa-check-double is-done"></i><p>'
                    + (allTasks.length ? 'Không có món nào ở mục này.' : 'Bếp đã xong tất cả các món!') + '</p></div>';
                return;
            }
            container.innerHTML = tasks.map((task) => createCardHtml(task, showAll)).join('');
        }

        function createCardHtml(t, showGroup) {
            const timeStr = new Date(t.createdTime || Date.now()).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
            const safeName = escapeHtml(t.productName);
            const safeNote = escapeHtml(t.note);
            const noteHtml = safeNote ? '<div class="task-note"><i class="fa-solid fa-triangle-exclamation"></i> ' + safeNote + '</div>' : '';
            const statusClass = t.status === 'PENDING' ? 'status-pending' : 'status-preparing';
            // So phut da cho: duoi 10 phut xanh, 10-20 phut vang, tren 20 phut do
            const minutes = waitedMinutes(t);
            const ageClass = minutes >= 20 ? ' age-late' : minutes >= 10 ? ' age-warm' : '';
            const taskId = escapeHtml(t.id);
            const pickup = t.pickupTime ? ' · lấy lúc ' + new Date(t.pickupTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute: '2-digit'}) : '';
            const groupHtml = showGroup || t.orderType === 'TAKEAWAY'
                ? '<div class="task-group' + (t.orderType === 'TAKEAWAY' ? ' is-takeaway' : '') + '">'
                    + (t.orderType === 'TAKEAWAY' ? '<i class="fa-solid fa-bag-shopping" aria-hidden="true"></i> ' : '')
                    + escapeHtml(groupNameOf(t)) + pickup + '</div>'
                : '';
            const actionHtml = t.status === 'PENDING'
                ? '<button class="btn btn-start" type="button" data-task-action="start" data-task-id="' + taskId + '"><i class="fa-solid fa-fire"></i> Nhận làm</button>'
                : '<button class="btn btn-finish" type="button" data-task-action="finish" data-task-id="' + taskId + '"><i class="fa-solid fa-check"></i> Đã xong</button>';

            return '<div class="task-card ' + statusClass + ageClass + '" id="task-' + taskId + '">'
                + '<div class="task-header"><span class="time" title="Gọi lúc ' + timeStr + '"><span class="task-age">' + minutes + ' phút</span> · ' + timeStr + '</span>'
                + '<span class="task-state">' + (t.status === 'PENDING' ? 'Món mới' : 'Đang nấu') + '</span></div>'
                + groupHtml
                + '<div class="task-name">' + safeName + ' <span class="task-qty">x' + t.quantity + '</span></div>'
                + noteHtml + '<div class="task-actions">' + actionHtml
                + '<button class="btn btn-cancel" type="button" data-task-action="cancel" data-task-id="' + taskId + '"><i class="fa-solid fa-xmark"></i> Hết</button>'
                + '</div></div>';
        }

        async function updateStatus(id, newStatus) {
            const response = await fetch(contextPath + '/api/kitchen/update-status', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderDetailId: id, status: newStatus })
            });
            if (!response.ok) {
                throw new Error('Không thể cập nhật món: HTTP ' + response.status);
            }
            scheduleTaskRefresh();
        }

        async function cancelItem(id) {
            if(!confirm("Xác nhận báo Hết nguyên liệu (sẽ hủy món và trừ tiền khách)?")) return;
            const response = await fetch(contextPath + '/api/kitchen/cancel-item', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderDetailId: id })
            });
            if (!response.ok) {
                throw new Error('Không thể hủy món: HTTP ' + response.status);
            }
            scheduleTaskRefresh();
        }

        function escapeHtml(value) {
            return String(value || '')
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#039;');
        }

        let socket;
        function connectWebSocket() {
            const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
            const wsUrl = protocol + window.location.host + contextPath + '/ws/kitchen';
            
            socket = new WebSocket(wsUrl);

            socket.onopen = function(e) {
                console.log("Đã kết nối Real-time KDS (WebSocket)");
            };

            socket.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    
                    if (data.status === socketStatus.NEW_ORDER) {
                        playBeep();
                        mergeNewTasks(data.data);
                    } else if (data.status === socketStatus.KITCHEN_TASK_UPDATED) {
                        scheduleTaskRefresh();
                    } else if (data.status === socketStatus.TABLES_UPDATED) {
                        loadTables();
                    }
                } catch (e) {
                    console.error('Cannot process kitchen WebSocket event', e);
                }
            };

            socket.onclose = function(e) {
                setTimeout(connectWebSocket, 3000);
            };
        }

        document.addEventListener("DOMContentLoaded", () => {
            const sidebarToggle = document.getElementById('kitchenSidebarToggle');
            const isSidebarCollapsed = window.localStorage.getItem('smartDining.kitchenSidebarCollapsed') === 'true';
            setKitchenSidebarCollapsed(isSidebarCollapsed);
            sidebarToggle.addEventListener('click', () => {
                const collapsed = !document.body.classList.contains('sidebar-collapsed');
                setKitchenSidebarCollapsed(collapsed);
                window.localStorage.setItem('smartDining.kitchenSidebarCollapsed', String(collapsed));
            });
            document.getElementById('groupChips').addEventListener('click', (event) => {
                const chip = event.target.closest('[data-group]');
                if (chip) selectGroup(chip.dataset.group);
            });
            document.getElementById('statusTabs').addEventListener('click', (event) => {
                const tab = event.target.closest('[data-status]');
                if (!tab) return;
                statusFilter = tab.dataset.status;
                renderTaskViews();
            });
            document.getElementById('tasksContainer').addEventListener('click', async (event) => {
                const button = event.target.closest('[data-task-action]');
                if (!button || button.disabled) return;
                const action = button.dataset.taskAction;
                const taskId = button.dataset.taskId;
                button.disabled = true;
                try {
                    if (action === 'start') await updateStatus(taskId, 'PREPARING');
                    if (action === 'finish') await updateStatus(taskId, 'READY');
                    if (action === 'cancel') await cancelItem(taskId);
                } catch (error) {
                    console.error(error);
                    window.alert('Không thể cập nhật món. Vui lòng thử lại.');
                } finally {
                    button.disabled = false;
                }
            });
            loadTables().then(fetchTasks);
            const clock = document.getElementById('kdsClock');
            const tickClock = () => { clock.textContent = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }); };
            tickClock();
            window.setInterval(tickClock, 15000);
            connectWebSocket();
            window.setInterval(fetchTasks, 10000);
        });

        function setKitchenSidebarCollapsed(collapsed) {
            document.body.classList.toggle('sidebar-collapsed', collapsed);
            const toggle = document.getElementById('kitchenSidebarToggle');
            toggle.setAttribute('aria-pressed', String(collapsed));
            toggle.setAttribute('title', collapsed ? 'Mở rộng danh sách bàn' : 'Thu gọn danh sách bàn');
            toggle.setAttribute('aria-label', collapsed ? 'Mở rộng danh sách bàn' : 'Thu gọn danh sách bàn');
        }
    </script>
</body>
</html>
