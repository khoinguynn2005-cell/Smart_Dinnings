const state = {
    categories: [],
    products: [],
    tables: [],
    employees: [],
    report: null,
    selectedCategoryId: '',
    productSearch: '',
    productSort: 'name',
    productView: (() => { try { return window.localStorage.getItem('smartDining.productView') || 'grid'; } catch (_) { return 'grid'; } })()
};

let confirmAction = null;

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
});

const viewTitles = {
    overview: ['Quản trị nhà hàng', 'Tổng quan'],
    menu: ['Thiết lập thực đơn', 'Thực đơn'],
    tables: ['Quản lý khu vực phục vụ', 'Bàn và QR'],
    employees: ['Cấp và quản lý tài khoản', 'Nhân sự'],
    reports: ['Theo dõi kết quả kinh doanh', 'Báo cáo']
};

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    loadAll();
    connectAdminSocket();
});

function bindEvents() {
    const isSidebarCollapsed = window.localStorage.getItem('smartDining.adminSidebarCollapsed') === 'true';
    document.body.classList.toggle('sidebar-collapsed', isSidebarCollapsed);
    updateSidebarToggle(isSidebarCollapsed);

    document.querySelectorAll('[data-view]').forEach((button) => {
        button.addEventListener('click', () => showView(button.dataset.view));
    });
    document.getElementById('refreshButton').addEventListener('click', loadAll);
    document.getElementById('sidebarToggle').addEventListener('click', toggleSidebar);
    document.getElementById('productCategoryFilters').addEventListener('click', (event) => {
        const button = event.target.closest('[data-category-filter]');
        if (!button) return;
        state.selectedCategoryId = button.dataset.categoryFilter;
        renderCategoryFilters();
        renderProducts();
    });
    document.getElementById('addProductButton').addEventListener('click', () => openProductModal());
    document.getElementById('productSort').addEventListener('change', (event) => {
        state.productSort = event.target.value;
        renderProducts();
    });
    document.querySelectorAll('[data-product-view]').forEach((button) => button.addEventListener('click', () => {
        state.productView = button.dataset.productView;
        try { window.localStorage.setItem('smartDining.productView', state.productView); } catch (_) { }
        renderProducts();
    }));
    document.getElementById('productSearch').addEventListener('input', (event) => {
        state.productSearch = event.target.value;
        renderProducts();
    });
    document.getElementById('manageCategoriesButton').addEventListener('click', () => openCategoryModal());
    document.getElementById('addTableButton').addEventListener('click', () => openTableModal());
    document.getElementById('addEmployeeButton').addEventListener('click', () => openEmployeeModal());
    document.getElementById('productForm').addEventListener('submit', saveProduct);
    document.getElementById('categoryForm').addEventListener('submit', saveCategory);
    document.getElementById('tableForm').addEventListener('submit', saveTable);
    document.getElementById('employeeForm').addEventListener('submit', saveEmployee);
    document.getElementById('reportFilter').addEventListener('submit', loadReport);
    document.getElementById('clearReportFilter').addEventListener('click', () => {
        document.getElementById('reportFrom').value = '';
        document.getElementById('reportTo').value = '';
        loadReport();
    });
    document.getElementById('confirmButton').addEventListener('click', async () => {
        if (!confirmAction) return;
        const action = confirmAction;
        confirmAction = null;
        closeModal('confirmModal');
        await action();
    });

    document.querySelectorAll('[data-close-modal]').forEach((button) => {
        button.addEventListener('click', () => closeModal(button.dataset.closeModal));
    });
    document.querySelectorAll('.modal').forEach((modal) => {
        modal.addEventListener('click', (event) => {
            if (event.target === modal) closeModal(modal.id);
        });
    });
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        document.querySelectorAll('.modal.is-open').forEach((modal) => closeModal(modal.id));
    });

    document.getElementById('productList').addEventListener('click', handleProductAction);
    document.getElementById('categoryList').addEventListener('click', handleCategoryAction);
    document.getElementById('tableList').addEventListener('click', handleTableAction);
    document.getElementById('employeeList').addEventListener('click', handleEmployeeAction);
}

function toggleSidebar() {
    const collapsed = document.body.classList.toggle('sidebar-collapsed');
    updateSidebarToggle(collapsed);
    window.localStorage.setItem('smartDining.adminSidebarCollapsed', String(collapsed));
}

function updateSidebarToggle(collapsed) {
    const toggle = document.getElementById('sidebarToggle');
    toggle.setAttribute('aria-pressed', String(collapsed));
    toggle.setAttribute('title', collapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng');
    toggle.setAttribute('aria-label', collapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng');
}

async function loadAll() {
    const refreshButton = document.getElementById('refreshButton');
    refreshButton.classList.add('is-loading');
    try {
        const [overview, categories, products, tables, employees] = await Promise.all([
            request('overview'), request('categories'), request('products'), request('tables'), request('employees')
        ]);
        state.categories = categories;
        state.products = products;
        state.tables = tables;
        state.employees = employees;
        renderOverview(overview);
        renderCategoryOptions();
        renderProducts();
        renderCategories();
        renderTables();
        renderEmployees();
        await loadReport();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        refreshButton.classList.remove('is-loading');
    }
}

async function loadMenuData() {
    const [categories, products] = await Promise.all([request('categories'), request('products')]);
    state.categories = categories;
    state.products = products;
    renderCategoryOptions();
    renderProducts();
    renderCategories();
}

async function loadTableData() {
    state.tables = await request('tables');
    renderTables();
}

async function loadEmployeeData() {
    state.employees = await request('employees');
    renderEmployees();
}

async function loadReport(event) {
    if (event) event.preventDefault();
    const from = document.getElementById('reportFrom').value;
    const to = document.getElementById('reportTo').value;
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    try {
        state.report = await request(`reports${params.size ? `?${params}` : ''}`);
        renderReport();
        state.staffActivity = await request(`staff-activity${params.size ? `?${params}` : ''}`);
        renderStaffActivity();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function request(path, options = {}) {
    const response = await fetch(`${contextPath}/api/admin/${path}`, {
        ...options,
        headers: {
            ...(options.body ? { 'Content-Type': 'application/json' } : {}),
            ...(options.headers || {})
        }
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(payload.message || 'Không thể thực hiện thao tác.');
    return payload.data;
}

function showView(view) {
    const title = viewTitles[view];
    if (!title) return;
    document.querySelectorAll('.nav-item').forEach((button) => button.classList.toggle('is-active', button.dataset.view === view));
    document.querySelectorAll('[data-view-panel]').forEach((panel) => panel.classList.toggle('is-active', panel.dataset.viewPanel === view));
    document.getElementById('viewEyebrow').textContent = title[0];
    document.getElementById('viewTitle').textContent = title[1];
}

function renderOverview(overview) {
    document.getElementById('todayRevenue').textContent = formatMoney(overview.todayRevenue);
    document.getElementById('todayOrders').textContent = String(overview.todayPaidOrders || 0);
    document.getElementById('openTables').textContent = String(overview.openTables || 0);
    document.getElementById('waitingTables').textContent = String(overview.waitingPaymentTables || 0);
    // Chi to do the 'Cho thanh toan' khi thuc su co ban dang cho
    document.querySelector('.metric-alert')?.classList.toggle('has-alert', (overview.waitingPaymentTables || 0) > 0);
    document.getElementById('activeEmployees').textContent = String(overview.activeEmployees || 0);
    document.getElementById('overviewOpenTables').textContent = String(overview.openTables || 0);
    document.getElementById('overviewWaitingTables').textContent = String(overview.waitingPaymentTables || 0);
    document.getElementById('pendingRequests').textContent = String(overview.pendingRequests || 0);
}

function renderCategoryOptions() {
    const productCategory = document.getElementById('productCategory');
    if (!state.categories.some((category) => String(category.id) === state.selectedCategoryId)) {
        state.selectedCategoryId = '';
    }
    productCategory.innerHTML = state.categories
        .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join('');
    renderCategoryFilters();
}

function renderCategoryFilters() {
    const container = document.getElementById('productCategoryFilters');
    const buttons = [{ id: '', name: 'Tất cả danh mục' }, ...state.categories];
    container.innerHTML = buttons.map((category) => {
        const id = String(category.id);
        const isSelected = id === state.selectedCategoryId;
        return `<button class="category-filter-button${isSelected ? ' is-active' : ''}" type="button"
            data-category-filter="${escapeAttribute(id)}" aria-pressed="${isSelected}">${escapeHtml(category.name)}</button>`;
    }).join('');
}

// "ga nuong" tim ra "Gà Nướng": bo dau tieng Viet, khong phan biet hoa thuong
function normalizeSearch(value) {
    return String(value || '').normalize('NFD').replace(/[̀-ͯ]/g, '')
        .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
}

function renderProducts() {
    const categoryId = state.selectedCategoryId;
    const keyword = normalizeSearch(state.productSearch);
    const visibleProducts = state.products.filter((product) =>
        (!categoryId || String(product.categoryId) === categoryId)
        && (!keyword || normalizeSearch(product.name).includes(keyword)));
    const sorters = {
        name: (first, second) => first.name.localeCompare(second.name, 'vi'),
        priceAsc: (first, second) => Number(first.price) - Number(second.price),
        priceDesc: (first, second) => Number(second.price) - Number(first.price),
        sold: (first, second) => Number(second.soldQuantity || 0) - Number(first.soldQuantity || 0)
    };
    visibleProducts.sort(sorters[state.productSort] || sorters.name);
    // "Ban chay": 3 mon ban nhieu nhat (it nhat 1 phan)
    const bestSellerIds = new Set(state.products.filter((product) => Number(product.soldQuantity || 0) > 0)
        .sort(sorters.sold).slice(0, 3).map((product) => product.id));
    const container = document.getElementById('productList');
    container.classList.toggle('is-list', state.productView === 'list');
    document.querySelectorAll('[data-product-view]').forEach((button) => {
        button.classList.toggle('is-active', button.dataset.productView === state.productView);
        button.setAttribute('aria-pressed', String(button.dataset.productView === state.productView));
    });
    if (!visibleProducts.length) {
        container.innerHTML = keyword
            ? `<div class="empty-state">Không tìm thấy món nào có tên chứa "${escapeHtml(state.productSearch.trim())}".</div>`
            : '<div class="empty-state">Không có món nào phù hợp.</div>';
        return;
    }
    container.innerHTML = visibleProducts.map((product) => {
        const available = product.status === 'AVAILABLE';
        const image = product.imageUrl
            ? `<img src="${escapeAttribute(product.imageUrl)}" alt="" loading="lazy" onerror="this.remove()">`
            : '';
        const badge = available
            ? (bestSellerIds.has(product.id) ? '<span class="product-flag is-hot"><i class="fa-solid fa-fire"></i> Bán chạy</span>' : '')
            : `<span class="product-flag is-off">${productStatusLabel(product.status)}</span>`;
        const sold = Number(product.soldQuantity || 0);
        return `<article class="product-admin-card${available ? '' : ' is-inactive'}">
            <div class="product-image">${image}<i class="fa-solid fa-bowl-food product-placeholder" aria-hidden="true"></i>${badge}<span class="product-price-chip">${formatMoney(product.price)}</span></div>
            <div class="product-card-content">
                <div class="product-card-top"><div><h3>${escapeHtml(product.name)}</h3><p>${escapeHtml(product.categoryName)} · ${sold ? `đã bán ${sold} phần` : 'chưa bán'}</p></div></div>
                <p class="product-description">${escapeHtml(product.description || 'Chưa có mô tả.')}</p>
                <div class="card-actions">
                    <button class="status-switch${available ? ' is-on' : ''}" type="button" role="switch" aria-checked="${available}" data-product-action="toggle" data-id="${escapeAttribute(product.id)}" title="${available ? 'Bấm để chuyển sang Tạm hết' : 'Bấm để mở bán lại'}"><span class="switch-track"><span class="switch-thumb"></span></span><span>${available ? 'Đang bán' : 'Tạm hết'}</span></button>
                    <span class="card-tools"><button class="small-icon-button" type="button" data-product-action="edit" data-id="${escapeAttribute(product.id)}" title="Sửa món" aria-label="Sửa món"><i class="fa-solid fa-pen"></i></button><button class="small-icon-button is-danger" type="button" data-product-action="delete" data-id="${escapeAttribute(product.id)}" title="Xóa hoặc ngưng kinh doanh món" aria-label="Xóa hoặc ngưng kinh doanh món"><i class="fa-solid fa-trash"></i></button></span>
                </div>
            </div>
        </article>`;
    }).join('');
}

// Cong tac tren the: Dang ban <-> Tam het (mon ngung kinh doanh bat lai thanh Dang ban)
async function toggleProductStatus(product) {
    const nextStatus = product.status === 'AVAILABLE' ? 'OUT_OF_STOCK' : 'AVAILABLE';
    try {
        await request(`products/${encodeURIComponent(product.id)}`, {
            method: 'PUT',
            body: JSON.stringify({ name: product.name, categoryId: product.categoryId, price: product.price,
                imageUrl: product.imageUrl || '', description: product.description || '', status: nextStatus })
        });
        product.status = nextStatus;
        renderProducts();
        showToast(nextStatus === 'AVAILABLE' ? `Đã mở bán lại “${product.name}”.` : `“${product.name}” đã chuyển sang Tạm hết.`);
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderCategories() {
    const container = document.getElementById('categoryList');
    if (!state.categories.length) {
        container.innerHTML = '<div class="empty-state">Chưa có danh mục.</div>';
        return;
    }
    container.innerHTML = state.categories.map((category) => `<div class="category-row"><strong>${escapeHtml(category.name)}</strong><button class="small-icon-button" type="button" data-category-action="edit" data-id="${category.id}" title="Sửa danh mục" aria-label="Sửa danh mục"><i class="fa-solid fa-pen"></i></button><button class="small-icon-button is-danger" type="button" data-category-action="delete" data-id="${category.id}" title="Xóa danh mục" aria-label="Xóa danh mục"><i class="fa-solid fa-trash"></i></button></div>`).join('');
}

function renderTables() {
    const container = document.getElementById('tableList');
    if (!state.tables.length) {
        container.innerHTML = '<tr><td colspan="4"><div class="empty-state">Chưa có bàn phục vụ.</div></td></tr>';
        return;
    }
    container.innerHTML = state.tables.map((table) => {
        const link = customerTableUrl(table.id);
        const editable = table.status === 'AVAILABLE' || table.status === 'INACTIVE';
        const inactive = table.status === 'INACTIVE';
        const lifecycleAction = inactive
            ? `<button class="table-state-button is-reopen" type="button" data-table-action="activate" data-id="${table.id}" title="Mở lại bàn để nhận khách"><i class="fa-solid fa-rotate-left" aria-hidden="true"></i><span>Mở lại</span></button>`
            : `<button class="table-state-button" type="button" data-table-action="deactivate" data-id="${table.id}" title="${table.status === 'AVAILABLE' ? 'Ngưng hoạt động: bảo trì hoặc không sử dụng nữa' : 'Bàn đang có khách, không thể ngưng hoạt động'}" ${table.status === 'AVAILABLE' ? '' : 'disabled'}><i class="fa-solid fa-ban" aria-hidden="true"></i><span>Ngưng hoạt động</span></button>`;
        return `<tr><td><div class="table-name">${escapeHtml(table.tableName)}<small>Mã bàn: ${table.id}</small></div></td><td><span class="status-badge ${statusClass(table.status)}">${tableStatusLabel(table.status)}</span></td><td><div class="customer-link"><i class="fa-solid fa-link"></i><code>${escapeHtml(link)}</code></div></td><td><div class="row-actions"><button class="small-icon-button" type="button" data-table-action="copy" data-id="${table.id}" title="Sao chép đường dẫn khách" aria-label="Sao chép đường dẫn khách"><i class="fa-regular fa-copy"></i></button><button class="small-icon-button" type="button" data-table-action="qr" data-id="${table.id}" title="Mở QR bằng dịch vụ ngoài" aria-label="Mở QR bằng dịch vụ ngoài"><i class="fa-solid fa-qrcode"></i></button><button class="small-icon-button" type="button" data-table-action="edit" data-id="${table.id}" title="${editable ? 'Sửa bàn' : 'Bàn đang có khách, không thể sửa'}" aria-label="Sửa bàn" ${editable ? '' : 'disabled'}><i class="fa-solid fa-pen"></i></button>${lifecycleAction}</div></td></tr>`;
    }).join('');
}

function renderEmployees() {
    const container = document.getElementById('employeeList');
    container.innerHTML = state.employees.map((employee) => {
        const isAdmin = employee.role === 'ADMIN';
        const contact = [employee.phone, employee.email].filter(Boolean).map(escapeHtml).join('<br>') || 'Chưa cập nhật';
        return `<tr><td><div class="table-name">${escapeHtml(employee.fullName)}<small>@${escapeHtml(employee.username)}</small></div></td><td><span class="role-label">${roleLabel(employee.role)}</span></td><td><div class="contact-stack"><small>${contact}</small></div></td><td><span class="status-badge ${statusClass(employee.status)}">${employeeStatusLabel(employee.status)}</span></td><td><div class="row-actions">${isAdmin ? '<span class="seller-meta">Tài khoản chủ quán</span>' : `<button class="small-icon-button" type="button" data-employee-action="edit" data-id="${escapeAttribute(employee.id)}" title="Sửa tài khoản" aria-label="Sửa tài khoản"><i class="fa-solid fa-pen"></i></button>`}</div></td></tr>`;
    }).join('');
}

function renderReport() {
    const report = state.report || { revenue: 0, invoiceCount: 0, averageInvoice: 0, dailyRevenue: [], bestSellers: [] };
    document.getElementById('reportRevenue').textContent = formatMoney(report.revenue);
    document.getElementById('reportInvoices').textContent = String(report.invoiceCount || 0);
    document.getElementById('reportAverage').textContent = formatMoney(report.averageInvoice);
    const dailyRevenue = report.dailyRevenue || [];
    const maximumRevenue = Math.max(...dailyRevenue.map((item) => Number(item.revenue || 0)), 1);
    document.getElementById('revenueChart').innerHTML = dailyRevenue.length
        ? dailyRevenue.map((item) => `<div class="chart-bar-group" title="${formatMoney(item.revenue)} - ${item.invoiceCount} hóa đơn"><div class="chart-bar-track"><span class="chart-bar" style="height:${Math.max(5, Math.round((Number(item.revenue || 0) / maximumRevenue) * 100))}%"></span></div><span class="chart-label">${formatDate(item.date)}</span></div>`).join('')
        : '<div class="empty-state">Chưa có hóa đơn đã thanh toán trong khoảng đã chọn.</div>';
    const sellers = report.bestSellers || [];
    document.getElementById('bestSellerList').innerHTML = sellers.length
        ? sellers.map((item, index) => `<div class="best-seller-item"><span class="seller-rank">${index + 1}</span><div><div class="seller-name">${escapeHtml(item.productName)}</div><span class="seller-meta">${formatMoney(item.revenue)}</span></div><span class="seller-quantity">${item.quantity} món</span></div>`).join('')
        : '<div class="empty-state">Chưa có dữ liệu món bán.</div>';
}

function openProductModal(product) {
    const form = document.getElementById('productForm');
    form.reset();
    document.getElementById('productModalTitle').textContent = product ? 'Cập nhật món ăn' : 'Thêm món ăn';
    document.getElementById('productId').value = product?.id || '';
    document.getElementById('productName').value = product?.name || '';
    document.getElementById('productCategory').value = product ? String(product.categoryId) : String(state.categories[0]?.id || '');
    document.getElementById('productPrice').value = product?.price || '';
    document.getElementById('productImageUrl').value = product?.imageUrl || '';
    document.getElementById('productDescription').value = product?.description || '';
    document.getElementById('productStatus').value = product?.status || 'AVAILABLE';
    openModal('productModal');
}

async function saveProduct(event) {
    event.preventDefault();
    const id = document.getElementById('productId').value;
    const payload = {
        name: document.getElementById('productName').value,
        categoryId: Number(document.getElementById('productCategory').value),
        price: Number(document.getElementById('productPrice').value),
        imageUrl: document.getElementById('productImageUrl').value,
        description: document.getElementById('productDescription').value,
        status: document.getElementById('productStatus').value
    };
    try {
        await request(id ? `products/${encodeURIComponent(id)}` : 'products', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        closeModal('productModal');
        await loadMenuData();
        showToast(id ? 'Đã cập nhật món ăn.' : 'Đã thêm món ăn.');
    } catch (error) {
        showToast(error.message, true);
    }
}

function openCategoryModal(category) {
    document.getElementById('categoryId').value = category?.id || '';
    document.getElementById('categoryName').value = category?.name || '';
    document.getElementById('categoryModalTitle').textContent = category ? 'Sửa danh mục' : 'Danh mục';
    openModal('categoryModal');
    document.getElementById('categoryName').focus();
}

async function saveCategory(event) {
    event.preventDefault();
    const id = document.getElementById('categoryId').value;
    try {
        await request(id ? `categories/${id}` : 'categories', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify({ name: document.getElementById('categoryName').value })
        });
        await loadMenuData();
        openCategoryModal();
        showToast(id ? 'Đã đổi tên danh mục.' : 'Đã thêm danh mục.');
    } catch (error) {
        showToast(error.message, true);
    }
}

function openTableModal(table) {
    document.getElementById('tableForm').reset();
    document.getElementById('tableModalTitle').textContent = table ? 'Sửa bàn' : 'Thêm bàn';
    document.getElementById('tableId').value = table?.id || '';
    document.getElementById('tableName').value = table?.tableName || '';
    // O trang thai chi co khi sua ban da co
    document.getElementById('tableStatusField').hidden = !table;
    document.getElementById('tableStatusSelect').value = table?.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE';
    openModal('tableModal');
}

async function saveTable(event) {
    event.preventDefault();
    const id = document.getElementById('tableId').value;
    const table = state.tables.find((item) => String(item.id) === id);
    const tableName = document.getElementById('tableName').value;
    const wantInactive = document.getElementById('tableStatusSelect').value === 'INACTIVE';
    try {
        if (!id) {
            await request('tables', { method: 'POST', body: JSON.stringify({ tableName }) });
        } else {
            if (tableName.trim() !== table.tableName) {
                await request(`tables/${id}`, { method: 'PUT', body: JSON.stringify({ tableName }) });
            }
            if (wantInactive && table.status !== 'INACTIVE') {
                await request(`tables/${id}`, { method: 'DELETE' });
            } else if (!wantInactive && table.status === 'INACTIVE') {
                await request(`tables/${id}/activate`, { method: 'POST', body: JSON.stringify({}) });
            }
        }
        closeModal('tableModal');
        await loadTableData();
        showToast(id ? 'Đã lưu thay đổi của bàn.' : 'Đã thêm bàn mới.');
    } catch (error) {
        showToast(error.message, true);
    }
}

function openEmployeeModal(employee) {
    const form = document.getElementById('employeeForm');
    form.reset();
    form.classList.toggle('is-editing', Boolean(employee));
    document.getElementById('employeeModalTitle').textContent = employee ? 'Cập nhật tài khoản' : 'Cấp tài khoản';
    document.getElementById('employeeId').value = employee?.id || '';
    document.getElementById('employeeFullName').value = employee?.fullName || '';
    document.getElementById('employeeUsername').value = employee?.username || '';
    document.getElementById('employeeUsername').required = !employee;
    document.getElementById('employeeRole').value = employee?.role === 'KITCHEN' ? 'KITCHEN' : 'STAFF';
    document.getElementById('employeePhone').value = employee?.phone || '';
    document.getElementById('employeeEmail').value = employee?.email || '';
    document.getElementById('employeeStatus').value = employee?.status || 'ACTIVE';
    document.getElementById('employeePassword').required = !employee;
    document.getElementById('employeeEmail').required = !employee;
    document.getElementById('passwordHint').textContent = employee ? '(để trống nếu không đổi)' : '(ít nhất 8 ký tự)';
    openModal('employeeModal');
}

async function saveEmployee(event) {
    event.preventDefault();
    const id = document.getElementById('employeeId').value;
    const payload = {
        fullName: document.getElementById('employeeFullName').value,
        username: document.getElementById('employeeUsername').value,
        password: document.getElementById('employeePassword').value,
        phone: document.getElementById('employeePhone').value,
        email: document.getElementById('employeeEmail').value,
        role: document.getElementById('employeeRole').value,
        status: document.getElementById('employeeStatus').value
    };
    try {
        await request(id ? `employees/${encodeURIComponent(id)}` : 'employees', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        closeModal('employeeModal');
        await loadEmployeeData();
        showToast(id ? 'Đã cập nhật tài khoản.' : 'Đã cấp tài khoản.');
    } catch (error) {
        showToast(error.message, true);
    }
}

function handleProductAction(event) {
    const button = event.target.closest('[data-product-action]');
    if (!button) return;
    const product = state.products.find((item) => item.id === button.dataset.id);
    if (!product) return;
    if (button.dataset.productAction === 'edit') {
        openProductModal(product);
        return;
    }
    if (button.dataset.productAction === 'toggle') {
        toggleProductStatus(product);
        return;
    }
    openConfirm('Xóa món ăn', `Món “${product.name}” sẽ bị xóa nếu chưa có order. Nếu đã có lịch sử, món sẽ chuyển thành ngưng kinh doanh để giữ nguyên hóa đơn cũ.`, async () => {
        try {
            const result = await request(`products/${encodeURIComponent(product.id)}`, { method: 'DELETE' });
            await loadMenuData();
            showToast(result.archived ? 'Món đã được ngưng kinh doanh để bảo toàn lịch sử order.' : 'Đã xóa món ăn.');
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

function handleCategoryAction(event) {
    const button = event.target.closest('[data-category-action]');
    if (!button) return;
    const category = state.categories.find((item) => String(item.id) === button.dataset.id);
    if (!category) return;
    if (button.dataset.categoryAction === 'edit') {
        openCategoryModal(category);
        return;
    }
    openConfirm('Xóa danh mục', `Xóa danh mục “${category.name}”? Danh mục có món ăn sẽ không thể xóa.`, async () => {
        try {
            await request(`categories/${category.id}`, { method: 'DELETE' });
            await loadMenuData();
            openCategoryModal();
            showToast('Đã xóa danh mục.');
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

function handleTableAction(event) {
    const button = event.target.closest('[data-table-action]');
    if (!button || button.disabled) return;
    const table = state.tables.find((item) => String(item.id) === button.dataset.id);
    if (!table) return;
    if (button.dataset.tableAction === 'edit') {
        openTableModal(table);
        return;
    }
    const link = customerTableUrl(table.id);
    if (button.dataset.tableAction === 'copy') {
        navigator.clipboard?.writeText(link).then(() => showToast('Đã sao chép đường dẫn khách.')).catch(() => showToast('Không thể sao chép đường dẫn.', true));
        return;
    }
    if (button.dataset.tableAction === 'activate') {
        openConfirm('Mở lại bàn', `Mở lại “${table.tableName}” để khách có thể quét QR và gọi món?`, async () => {
            try {
                await request(`tables/${table.id}/activate`, { method: 'POST', body: JSON.stringify({}) });
                await loadTableData();
                showToast('Bàn đã sẵn sàng phục vụ.');
            } catch (error) {
                showToast(error.message, true);
            }
        });
        return;
    }
    if (button.dataset.tableAction === 'deactivate') {
        openConfirm('Ngưng hoạt động bàn', `“${table.tableName}” sẽ tạm ngưng nhận khách (bảo trì hoặc không sử dụng nữa): ẩn khỏi trang phục vụ và bếp, mã QR không gọi món được. Lịch sử hóa đơn vẫn được giữ lại.`, async () => {
            try {
                await request(`tables/${table.id}`, { method: 'DELETE' });
                await loadTableData();
                showToast('Bàn đã ngưng hoạt động.');
            } catch (error) {
                showToast(error.message, true);
            }
        });
        return;
    }
    const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${encodeURIComponent(link)}`;
    window.open(qrUrl, '_blank', 'noopener');
}

function handleEmployeeAction(event) {
    const button = event.target.closest('[data-employee-action]');
    if (!button) return;
    const employee = state.employees.find((item) => item.id === button.dataset.id);
    if (employee) openEmployeeModal(employee);
}

function openConfirm(title, message, action) {
    document.getElementById('confirmTitle').textContent = title;
    document.getElementById('confirmMessage').textContent = message;
    confirmAction = action;
    openModal('confirmModal');
}

function openModal(id) {
    const modal = document.getElementById(id);
    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
    if (id === 'confirmModal') confirmAction = null;
}

function customerTableUrl(tableId) {
    return `${window.location.origin}${contextPath}/home/index?tableId=${encodeURIComponent(tableId)}`;
}

function statusClass(status) {
    return `status-${String(status || '').toLocaleLowerCase('en-US').replaceAll('_', '-')}`;
}

function productStatusLabel(status) {
    return { AVAILABLE: 'Đang bán', OUT_OF_STOCK: 'Tạm hết hàng', DISCONTINUED: 'Ngưng kinh doanh' }[status] || status;
}

function tableStatusLabel(status) {
    return { AVAILABLE: 'Trống', FULL: 'Đang phục vụ', WAITING_PAYMENT: 'Chờ thanh toán', INACTIVE: 'Ngưng hoạt động' }[status] || status;
}

function employeeStatusLabel(status) {
    return { ACTIVE: 'Đang hoạt động', INACTIVE: 'Đã khóa', PENDING: 'Chờ xác thực email' }[status] || status;
}

function roleLabel(role) {
    return { ADMIN: 'Quản trị', STAFF: 'Nhân viên phục vụ', KITCHEN: 'Nhân viên bếp' }[role] || role;
}

function formatDate(value) {
    if (!value) return '';
    const [year, month, day] = String(value).split('-');
    return `${day}/${month}`;
}

function formatMoney(value) {
    return moneyFormatter.format(Number(value || 0));
}

function escapeHtml(value) {
    return String(value ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

function escapeAttribute(value) {
    return escapeHtml(value).replaceAll('`', '&#096;');
}

function showToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = `toast${isError ? ' error' : ''}`;
    toast.textContent = message;
    document.getElementById('toastRegion').append(toast);
    window.setTimeout(() => toast.remove(), 4500);
}

// ---------- Real-time: WebSocket /ws/admin ----------
// Moi su kien chi tai lai phan du lieu lien quan; nhieu su kien lien tiep duoc gom lai (debounce).
const realtimeReloaders = {
    overview: async () => renderOverview(await request('overview')),
    tables: loadTableData,
    menu: loadMenuData,
    employees: loadEmployeeData,
    report: async () => {
        if (reportIncludesToday()) await loadReport();
    }
};

const realtimeTargets = {
    ORDER_CONFIRMED: ['overview', 'tables'],
    TABLE_STATUS_UPDATED: ['overview', 'tables'],
    PAYMENT_REQUEST: ['overview', 'tables'],
    SERVICE_REQUEST: ['overview'],
    TABLE_REQUEST_COMPLETED: ['overview'],
    ORDER_PAID: ['overview', 'tables', 'report'],
    TABLES_UPDATED: ['overview', 'tables'],
    MENU_UPDATED: ['menu'],
    EMPLOYEE_UPDATED: ['employees', 'overview']
};

const pendingRealtimeReloads = new Set();
let realtimeReloadTimer = null;
let adminSocket = null;
let adminSocketRetryDelay = 1000;
let adminSocketConnectedOnce = false;

function connectAdminSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    adminSocket = new WebSocket(`${protocol}${window.location.host}${contextPath}/ws/admin`);
    adminSocket.addEventListener('open', () => {
        adminSocketRetryDelay = 1000;
        setLiveStatus(true);
        // Ket noi lai sau khi mat mang: tai lai toan bo de khong bo sot su kien trong luc mat ket noi.
        if (adminSocketConnectedOnce) loadAll();
        adminSocketConnectedOnce = true;
    });
    adminSocket.addEventListener('message', (event) => handleAdminEvent(event.data));
    adminSocket.addEventListener('close', () => {
        setLiveStatus(false);
        window.setTimeout(connectAdminSocket, adminSocketRetryDelay);
        adminSocketRetryDelay = Math.min(adminSocketRetryDelay * 2, 30000);
    });
}

function handleAdminEvent(raw) {
    let event;
    try {
        event = JSON.parse(raw);
    } catch {
        return;
    }
    notifyAdminEvent(event);
    (realtimeTargets[event.status] || []).forEach((target) => pendingRealtimeReloads.add(target));
    if (!pendingRealtimeReloads.size) return;
    window.clearTimeout(realtimeReloadTimer);
    realtimeReloadTimer = window.setTimeout(runRealtimeReloads, 400);
}

async function runRealtimeReloads() {
    const targets = [...pendingRealtimeReloads];
    pendingRealtimeReloads.clear();
    await Promise.all(targets.map(async (target) => {
        try {
            await realtimeReloaders[target]();
        } catch (error) {
            console.error(`Cannot refresh ${target}`, error);
        }
    }));
}

function notifyAdminEvent(event) {
    const tableName = event.tableName || tableNameById(event.tableId);
    if (event.status === 'PAYMENT_REQUEST') {
        showToast(`${tableName} yêu cầu thanh toán ${formatMoney(event.totalAmount)}.`);
    } else if (event.status === 'SERVICE_REQUEST') {
        showToast(`${tableName} yêu cầu: ${event.type}${event.note ? ` (${event.note})` : ''}.`);
    } else if (event.status === 'ORDER_PAID') {
        showToast(`${tableName} đã thanh toán xong.`);
    }
}

function tableNameById(tableId) {
    const table = state.tables.find((item) => String(item.id) === String(tableId));
    return table ? table.tableName : `Bàn ${tableId ?? ''}`.trim();
}

function reportIncludesToday() {
    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    const from = document.getElementById('reportFrom').value;
    const to = document.getElementById('reportTo').value;
    return (!from || from <= today) && (!to || to >= today);
}

function setLiveStatus(connected) {
    const badge = document.getElementById('liveStatus');
    if (!badge) return;
    badge.classList.toggle('is-offline', !connected);
    badge.title = connected ? 'Dữ liệu đang tự cập nhật theo thời gian thực' : 'Mất kết nối, đang thử kết nối lại...';
    badge.querySelector('span').textContent = connected ? 'Trực tiếp' : 'Mất kết nối';
}

// ---------- Tong quan: bam vao o chi so de xem chi tiet ----------
const overviewDetailConfig = {
    paid: { title: 'Hóa đơn đã thanh toán hôm nay', empty: 'Hôm nay chưa có hóa đơn nào được thanh toán.' },
    open: { title: 'Bàn đang phục vụ', empty: 'Hiện không có bàn nào đang phục vụ.' },
    waiting: { title: 'Bàn chờ thanh toán', empty: 'Không có bàn nào đang chờ thanh toán.' },
    employees: { title: 'Nhân sự đang hoạt động', empty: 'Chưa có tài khoản nào đang hoạt động.' },
    requests: { title: 'Yêu cầu đang chờ xử lý', empty: 'Không có yêu cầu nào đang chờ.' }
};

document.addEventListener('DOMContentLoaded', () => {
    const overview = document.getElementById('overviewView');
    overview.addEventListener('click', (event) => {
        const target = event.target.closest('[data-detail]');
        if (target) openOverviewDetail(target.dataset.detail);
    });
    overview.addEventListener('keydown', (event) => {
        const target = event.target.closest('[data-detail]');
        if (target && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            openOverviewDetail(target.dataset.detail);
        }
    });
    document.getElementById('detailBody').addEventListener('click', (event) => {
        const button = event.target.closest('[data-payment-image]');
        if (button) showPaymentImage(button.dataset.paymentImage, button.dataset.label);
    });
});

async function openOverviewDetail(type) {
    const config = overviewDetailConfig[type];
    if (!config) return;
    document.getElementById('detailTitle').textContent = config.title;
    const body = document.getElementById('detailBody');
    body.innerHTML = '<p class="detail-loading">Đang tải...</p>';
    openModal('detailModal');
    try {
        const rows = await request(`overview/details?type=${encodeURIComponent(type)}`);
        body.innerHTML = rows.length ? renderOverviewDetail(type, rows) : `<p class="detail-empty">${config.empty}</p>`;
    } catch (error) {
        body.innerHTML = `<p class="detail-empty">${escapeHtml(error.message)}</p>`;
    }
}

function renderOverviewDetail(type, rows) {
    const time = (value) => value ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
    const methodLabel = (method) => ({ CASH: 'Tiền mặt', TRANSFER: 'Chuyển khoản' }[method] || 'Không rõ');
    if (type === 'paid') {
        const total = rows.reduce((sum, row) => sum + Number(row.amount || 0), 0);
        return `<div class="detail-summary"><span>${rows.length} hóa đơn</span><strong>${formatMoney(total)}</strong></div>
            <table class="detail-table"><thead><tr><th>Giờ</th><th>Bàn</th><th>Nhân viên</th><th>Thanh toán</th><th class="num">Số tiền</th></tr></thead><tbody>
            ${rows.map((row) => `<tr><td>${time(row.time)}</td><td>${escapeHtml(row.tableName)}<small>${escapeHtml(row.orderId)}</small></td><td>${escapeHtml(row.employee || '—')}</td>
                <td>${methodLabel(row.method)}${row.paymentId ? ` <button class="text-button bill-link" type="button" data-payment-image="${escapeAttribute(row.paymentId)}" data-label="${escapeAttribute(row.tableName + ' · ' + time(row.time))}"><i class="fa-regular fa-image"></i> Xem bill</button>` : ''}</td>
                <td class="num">${formatMoney(row.amount)}</td></tr>`).join('')}
            </tbody></table>`;
    }
    if (type === 'open' || type === 'waiting') {
        return `<table class="detail-table"><thead><tr><th>Bàn</th><th>Mở lúc</th><th>Món đã gửi bếp</th><th>Trạng thái</th><th class="num">Tạm tính</th></tr></thead><tbody>
            ${rows.map((row) => `<tr><td>${escapeHtml(row.tableName)}<small>${escapeHtml(row.orderId)}</small></td><td>${time(row.time)}</td>
                <td>${row.servedCount}/${row.sentCount} đã phục vụ${row.cartCount ? `<small>${row.cartCount} món còn trong giỏ</small>` : ''}</td>
                <td><span class="status-badge ${statusClass(row.tableStatus)}">${tableStatusLabel(row.tableStatus)}</span></td>
                <td class="num">${formatMoney(row.amount)}</td></tr>`).join('')}
            </tbody></table>`;
    }
    if (type === 'employees') {
        return `<table class="detail-table"><thead><tr><th>Nhân sự</th><th>Vai trò</th></tr></thead><tbody>
            ${rows.map((row) => `<tr><td>${escapeHtml(row.fullName)}<small>@${escapeHtml(row.username)}</small></td><td>${roleLabel(row.role)}</td></tr>`).join('')}
            </tbody></table>`;
    }
    return `<table class="detail-table"><thead><tr><th>Giờ</th><th>Bàn</th><th>Yêu cầu</th><th>Ghi chú</th></tr></thead><tbody>
        ${rows.map((row) => `<tr><td>${time(row.time)}</td><td>${escapeHtml(row.tableName)}</td>
            <td>${row.type === 'PAYMENT' ? `Thanh toán · ${formatMoney(row.amount)}` : escapeHtml(row.serviceType || 'Gọi nhân viên')}</td>
            <td>${escapeHtml(row.note || '—')}</td></tr>`).join('')}
        </tbody></table>`;
}

async function showPaymentImage(paymentId, label) {
    const body = document.getElementById('detailBody');
    const previous = body.innerHTML;
    const previousTitle = document.getElementById('detailTitle').textContent;
    body.innerHTML = '<p class="detail-loading">Đang tải ảnh bill...</p>';
    try {
        const data = await request(`payments/${encodeURIComponent(paymentId)}/image`);
        body.innerHTML = `<button class="text-button" type="button" id="detailBack"><i class="fa-solid fa-arrow-left"></i> Quay lại danh sách</button>
            <img class="bill-image" src="${escapeAttribute(data.image)}" alt="Ảnh bill chuyển khoản ${escapeAttribute(label)}">`;
    } catch (error) {
        body.innerHTML = `<button class="text-button" type="button" id="detailBack"><i class="fa-solid fa-arrow-left"></i> Quay lại danh sách</button><p class="detail-empty">${escapeHtml(error.message)}</p>`;
    }
    document.getElementById('detailTitle').textContent = 'Bill chuyển khoản · ' + label;
    document.getElementById('detailBack').addEventListener('click', () => {
        body.innerHTML = previous;
        document.getElementById('detailTitle').textContent = previousTitle;
    });
}

// ---------- Bao cao: hoat dong nhan vien ----------
function minutesLabel(value) {
    if (value == null) return '—';
    // Tren 60 phut doi sang gio cho de doc (vd: yeu cau de qua dem moi xu ly)
    if (value >= 60) {
        const hours = Math.floor(value / 60);
        const minutes = Math.round(value % 60);
        return `${hours} giờ${minutes ? ` ${minutes} phút` : ''}`;
    }
    return `${String(value).replace('.', ',')} phút`;
}

function renderStaffActivity() {
    const rows = state.staffActivity || [];
    const body = document.getElementById('staffActivityList');
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="10" class="detail-empty">Chưa có hoạt động trong khoảng thời gian này.</td></tr>';
        return;
    }
    const speed = (minutes, good, slow) => minutes == null ? '' : minutes <= good ? 'is-good' : minutes >= slow ? 'is-slow' : 'is-ok';
    body.innerHTML = rows.map((row) => {
        const revenue = Number(row.revenue || 0);
        const cashShare = revenue > 0 ? Math.round(Number(row.cash || 0) * 100 / revenue) : null;
        return `<tr>
            <td><div class="table-name">${escapeHtml(row.fullName)}<small>${roleLabel(row.role)}${row.status === 'INACTIVE' ? ' · đã khóa' : ''}</small></div></td>
            <td class="num">${row.invoices || '—'}</td>
            <td class="num"><strong class="money">${revenue ? formatMoney(revenue) : '—'}</strong></td>
            <td class="num">${cashShare == null ? '—' : `${cashShare}% / ${100 - cashShare}%`}</td>
            <td class="num">${row.requests || '—'}</td>
            <td class="num"><span class="speed ${speed(row.avgResponseMinutes, 3, 8)}">${minutesLabel(row.avgResponseMinutes)}</span></td>
            <td class="num">${row.itemsServed || '—'}</td>
            <td class="num">${row.itemsCooked || '—'}</td>
            <td class="num"><span class="speed ${speed(row.avgCookMinutes, 12, 25)}">${minutesLabel(row.avgCookMinutes)}</span></td>
            <td class="num">${row.cancelled || '—'}</td>
        </tr>`;
    }).join('');
}

// Xuat CSV (UTF-8 co BOM de Excel doc dung tieng Viet)
function exportStaffActivity() {
    const rows = state.staffActivity || [];
    const from = document.getElementById('reportFrom').value || 'tat-ca';
    const to = document.getElementById('reportTo').value || 'nay';
    const header = ['Nhân viên', 'Tên đăng nhập', 'Vai trò', 'Số hóa đơn', 'Tiền thu', 'Tiền mặt', 'Chuyển khoản',
        'Yêu cầu đã xử lý', 'Phản hồi TB (phút)', 'Món mang ra', 'Món bếp làm', 'Nấu TB (phút)', 'Báo hết món'];
    const lines = rows.map((row) => [row.fullName, row.username, roleLabel(row.role), row.invoices, row.revenue, row.cash,
        row.transfer, row.requests, row.avgResponseMinutes ?? '', row.itemsServed, row.itemsCooked, row.avgCookMinutes ?? '', row.cancelled]);
    const csv = [header].concat(lines)
        .map((line) => line.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
        .join('\r\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }));
    link.download = `hoat-dong-nhan-vien_${from}_${to}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(link.href);
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('exportStaffActivity').addEventListener('click', exportStaffActivity);
});
