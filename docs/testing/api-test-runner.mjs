// SmartDining - kiem thu chuc nang qua HTTP (black-box, API + trang).
// Chay:  node docs/testing/api-test-runner.mjs
// Bien moi truong (tuy chon):
//   BASE_URL    mac dinh http://localhost:8081/SmartDining
//   TOMCAT_LOG  file log Tomcat de doc ma xac thuc DEV (khi chua cau hinh Gmail)
// Ket qua: docs/testing/test-results.json (moi test case: id, module, title, data, expected, actual, result)
// Tai khoan dung de test la tai khoan mau trong database/SmartDining.sql.
// Du lieu test (ten bat dau bang "QA ") tu dong bi xoa truoc va sau khi chay bang cleanup-test-data.sql.
// Them --keep-data de giu lai du lieu sau khi chay (vd: muon mo SSMS xem lai).

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const BASE = process.env.BASE_URL || 'http://localhost:8081/SmartDining';
const TOMCAT_LOG = process.env.TOMCAT_LOG
    || 'D:/FPTPolytechnic/JAV103_WebAI/tomcat/apache-tomcat-10.1.20/logs/claude-run.log';
const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, 'test-results.json');
const KEEP_DATA = process.argv.includes('--keep-data');

// Chay cleanup-test-data.sql bang sqlcmd, lay thong tin DB tu config.properties cua project
function cleanupTestData(when) {
    const configPath = join(HERE, '..', '..', 'src', 'main', 'java', 'META-INF', 'config.properties');
    if (!existsSync(configPath)) {
        console.log(`[don du lieu ${when}] bo qua: khong tim thay config.properties`);
        return;
    }
    const config = Object.fromEntries(readFileSync(configPath, 'utf8').split(/\r?\n/)
        .filter((line) => line.includes('=') && !line.trim().startsWith('#'))
        .map((line) => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]));
    const match = /\/\/([^:;\/]+)(?::(\d+))?.*databaseName=([^;]+)/.exec(config['db.url'] || '');
    if (!match) {
        console.log(`[don du lieu ${when}] bo qua: khong doc duoc db.url`);
        return;
    }
    const [, host, port = '1433', database] = match;
    const result = spawnSync('sqlcmd', ['-S', `${host},${port}`, '-U', config['db.username'], '-C', '-I', '-b',
        '-d', database, '-i', join(HERE, 'cleanup-test-data.sql')],
        { env: { ...process.env, SQLCMDPASSWORD: config['db.password'] }, encoding: 'utf8' });
    if (result.error || result.status !== 0) {
        console.log(`[don du lieu ${when}] LOI: ${result.error?.message || result.stderr || result.stdout}`);
        return;
    }
    const removed = result.stdout.split(/\r?\n/).filter((line) => /: [1-9]/.test(line));
    console.log(`[don du lieu ${when}] ${removed.length ? removed.join(', ') : 'khong co du lieu test'}`);
}
const RUN = Date.now().toString().slice(-6); // hau to de du lieu test khong trung lap

const ACCOUNTS = {
    admin: ['admin', 'Admin@123'],
    staff: ['staff1', 'Staff@123'],
    kitchen: ['kitchen1', 'Kitchen@123'],
};

// ---------- HTTP client co cookie rieng cho tung "nguoi dung" ----------
class Actor {
    constructor(name) { this.name = name; this.cookies = new Map(); }
    cookieHeader() { return [...this.cookies].map(([k, v]) => `${k}=${v}`).join('; '); }
    async req(method, path, body, form) {
        const headers = { Cookie: this.cookieHeader() };
        let payload;
        if (form) {
            headers['Content-Type'] = 'application/x-www-form-urlencoded';
            payload = new URLSearchParams(form).toString();
        } else if (body !== undefined) {
            headers['Content-Type'] = 'application/json';
            payload = JSON.stringify(body);
        }
        const res = await fetch(BASE + path, { method, headers, body: payload, redirect: 'manual' });
        for (const c of res.headers.getSetCookie()) {
            const [pair] = c.split(';');
            const i = pair.indexOf('=');
            const k = pair.slice(0, i), v = pair.slice(i + 1);
            if (v === '' || /max-age=0/i.test(c)) this.cookies.delete(k); else this.cookies.set(k, v);
        }
        const text = await res.text();
        let json = null;
        try { json = JSON.parse(text); } catch { /* HTML */ }
        const location = res.headers.get('location') || '';
        return { status: res.status, text, json, location: location.replace(/^https?:\/\/[^/]+/, '') };
    }
    get(p) { return this.req('GET', p); }
    post(p, body) { return this.req('POST', p, body); }
    put(p, body) { return this.req('PUT', p, body); }
    del(p) { return this.req('DELETE', p); }
    form(p, data) { return this.req('POST', p, undefined, data); }
}

// ---------- ghi nhan ket qua ----------
const results = [];
function record(id, module, title, data, expected, actual, pass) {
    results.push({ id, module, title, data, expected, actual, result: pass ? 'PASS' : 'FAIL' });
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${title}${pass ? '' : `\n      expected: ${expected}\n      actual:   ${actual}`}`);
}
const short = (r) => `HTTP ${r.status}${r.location ? ' -> ' + r.location : ''}${r.json ? ' ' + JSON.stringify(r.json).slice(0, 140) : ''}`;

async function login(actor, area, username, password) {
    return actor.form(`/login/${area}`, { username, password });
}

// Ma xac thuc DEV duoc in ra log Tomcat khi chua cau hinh Gmail hoac gui mail that bai.
// Gui mail chay nen; neu Gmail tu choi dang nhap thi co the mat hon 10 giay moi in ma -> cho toi da 30 giay.
async function devCodeFor(username, minCount = 1) {
    for (let i = 0; i < 60; i++) {
        if (existsSync(TOMCAT_LOG)) {
            const pattern = new RegExp(`ma xac thuc cua ${username} la: (\\d{6})`, 'g');
            const codes = [...readFileSync(TOMCAT_LOG, 'utf8').matchAll(pattern)].map((m) => m[1]);
            if (codes.length >= minCount) return codes.at(-1);
        }
        await sleep(500);
    }
    return null;
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---------- WebSocket: ket noi voi cookie cua actor, ghi lai moi su kien nhan duoc ----------
function openSocket(actor, endpoint) {
    const url = BASE.replace(/^http/, 'ws') + endpoint;
    const ws = new WebSocket(url, { headers: { Cookie: actor.cookieHeader() } });
    const socket = { ws, events: [], closed: false, opened: false };
    ws.onopen = () => { socket.opened = true; };
    ws.onmessage = (e) => { try { socket.events.push(JSON.parse(e.data)); } catch { /* bo qua */ } };
    ws.onclose = () => { socket.closed = true; };
    return socket;
}
async function waitForEvent(socket, status, predicate = () => true, timeoutMs = 4000) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
        const found = socket.events.find((e) => e.status === status && predicate(e));
        if (found) return found;
        await sleep(100);
    }
    return null;
}
async function waitClosed(socket, timeoutMs = 3000) {
    const start = Date.now();
    while (!socket.closed && Date.now() - start < timeoutMs) await sleep(100);
    return socket.closed;
}

// =====================================================================
async function main() {
    const admin = new Actor('admin'), staff = new Actor('staff'), kitchen = new Actor('kitchen');
    const guest = new Actor('guest'), customer = new Actor('customer');

    // ---------------- AUTH: dieu huong & dang nhap ----------------
    let r = await guest.get('/');
    record('TC-AUTH-01', 'Xác thực', 'Truy cập trang gốc chuyển về trang đăng nhập', 'GET /',
        '302 -> /SmartDining/login', short(r), r.status === 302 && r.location.endsWith('/login'));

    r = await guest.get('/index.jsp');
    record('TC-AUTH-02', 'Xác thực', 'Truy cập thẳng index.jsp chuyển về đăng nhập', 'GET /index.jsp',
        '302 -> /SmartDining/login', short(r), r.status === 302 && r.location.endsWith('/login'));

    r = await guest.get('/login');
    record('TC-AUTH-03', 'Xác thực', 'Trang đăng nhập hiển thị hướng dẫn cho khách và nhân viên', 'GET /login',
        '200, có "quét mã QR" và "Nhân viên"', `HTTP ${r.status}`,
        r.status === 200 && r.text.includes('quét mã QR') && r.text.includes('Nhân viên'));

    for (const [area, actor, tc] of [['admin', admin, '04'], ['staff', staff, '05'], ['kitchen', kitchen, '06']]) {
        const [u, p] = ACCOUNTS[area];
        r = await login(actor, area, u, p);
        record(`TC-AUTH-${tc}`, 'Xác thực', `Đăng nhập ${area} đúng tài khoản`, `${u} / (mật khẩu đúng)`,
            `302 -> /${area}/dashboard`, short(r), r.status === 302 && r.location.endsWith(`/${area}/dashboard`));
    }

    r = await login(guest, 'staff', 'staff1', 'sai-mat-khau');
    record('TC-AUTH-07', 'Xác thực', 'Đăng nhập sai mật khẩu', 'staff1 / sai-mat-khau',
        '200, báo "Sai ten dang nhap hoac mat khau"', `HTTP ${r.status}`,
        r.status === 200 && r.text.includes('Sai ten dang nhap hoac mat khau'));

    r = await login(guest, 'staff', 'khong-ton-tai', 'abc12345');
    record('TC-AUTH-08', 'Xác thực', 'Đăng nhập tài khoản không tồn tại', 'khong-ton-tai / abc12345',
        '200, báo lỗi', `HTTP ${r.status}`, r.status === 200 && r.text.includes('error-msg'));

    r = await login(guest, 'staff', 'staff2', '1234');
    record('TC-AUTH-09', 'Xác thực', 'Đăng nhập tài khoản đã bị khóa (INACTIVE)', 'staff2 / 1234',
        '200, báo "Tai khoan cua ban da bi khoa"', `HTTP ${r.status}`,
        r.status === 200 && r.text.includes('da bi khoa'));

    r = await login(guest, 'kitchen', 'staff1', 'Staff@123');
    record('TC-AUTH-10', 'Xác thực', 'Đăng nhập sai khu vực (staff vào cổng bếp)', 'staff1 tại /login/kitchen',
        '200, báo "Tai khoan khong thuoc khu vuc"', `HTTP ${r.status}`,
        r.status === 200 && r.text.includes('khong thuoc khu vuc'));

    // ---------------- PHAN QUYEN ----------------
    r = await guest.get('/admin/dashboard');
    record('TC-PQ-01', 'Phân quyền', 'Chưa đăng nhập vào trang admin', 'GET /admin/dashboard',
        '302 -> /login', short(r), r.status === 302 && r.location.endsWith('/login'));
    r = await staff.get('/api/admin/overview');
    record('TC-PQ-02', 'Phân quyền', 'Nhân viên phục vụ gọi API admin', 'staff1: GET /api/admin/overview',
        '403', short(r), r.status === 403);
    r = await kitchen.get('/api/staff/tables');
    record('TC-PQ-03', 'Phân quyền', 'Nhân viên bếp gọi API phục vụ', 'kitchen1: GET /api/staff/tables',
        '403', short(r), r.status === 403);
    r = await guest.post('/api/kitchen/update-status', { orderDetailId: 'x', status: 'READY' });
    record('TC-PQ-04', 'Phân quyền', 'Khách chưa đăng nhập gọi API bếp', 'POST /api/kitchen/update-status',
        '403', short(r), r.status === 403);
    r = await admin.get('/api/kitchen/tasks');
    record('TC-PQ-05', 'Phân quyền', 'Admin được xem màn hình bếp', 'admin: GET /api/kitchen/tasks',
        '200', short(r), r.status === 200);

    // ---------------- REAL-TIME: mo ket noi WebSocket (giu den cuoi de ghi lai su kien) ----------------
    const anonAdminWs = openSocket(guest, '/ws/admin');
    const staffAdminWs = openSocket(staff, '/ws/admin');
    const adminWs = openSocket(admin, '/ws/admin');
    const kitchenWs = openSocket(kitchen, '/ws/kitchen');
    record('TC-RT-01', 'Real-time', 'Khách chưa đăng nhập không kết nối được kênh admin', 'WebSocket /ws/admin, không cookie',
        'Server đóng kết nối', `closed=${await waitClosed(anonAdminWs)}`, anonAdminWs.closed);
    record('TC-RT-02', 'Real-time', 'Nhân viên phục vụ không kết nối được kênh admin', 'WebSocket /ws/admin, cookie staff1',
        'Server đóng kết nối', `closed=${await waitClosed(staffAdminWs)}`, staffAdminWs.closed);
    await sleep(500);
    record('TC-RT-03', 'Real-time', 'Admin kết nối được kênh admin', 'WebSocket /ws/admin, cookie admin',
        'Kết nối mở và giữ nguyên', `opened=${adminWs.opened} closed=${adminWs.closed}`, adminWs.opened && !adminWs.closed);

    // ---------------- ADMIN: danh muc, mon, ban ----------------
    const catName = `QA Danh mục ${RUN}`;
    r = await admin.post('/api/admin/categories', { name: catName });
    const categoryId = r.json?.data?.id;
    record('TC-AD-01', 'Quản trị', 'Thêm danh mục mới', `name="${catName}"`,
        '200, trả về id', short(r), r.status === 200 && categoryId != null);

    r = await admin.post('/api/admin/categories', { name: catName });
    record('TC-AD-02', 'Quản trị', 'Thêm danh mục trùng tên', `name="${catName}" (lần 2)`,
        '400', short(r), r.status === 400);

    r = await admin.post('/api/admin/categories', { name: '   ' });
    record('TC-AD-03', 'Quản trị', 'Thêm danh mục tên rỗng', 'name="   "', '400', short(r), r.status === 400);

    // Mon test dat vao mot danh muc co san, de cuoi bai xoa duoc danh muc test (khong de lai rac)
    const existingCategory = ((await admin.get('/api/admin/categories')).json?.data || [])
        .find((c) => !String(c.name).startsWith('QA '));
    const productBody = { name: `QA Món ${RUN}`, categoryId: existingCategory?.id, price: 50000, imageUrl: '', description: 'Món dùng để test', status: 'AVAILABLE' };
    r = await admin.post('/api/admin/products', productBody);
    const productId = r.json?.data?.id;
    record('TC-AD-04', 'Quản trị', 'Thêm món ăn hợp lệ', JSON.stringify(productBody),
        '200, trả về id món', short(r), r.status === 200 && !!productId);

    r = await admin.post('/api/admin/products', { ...productBody, name: '' });
    record('TC-AD-05', 'Quản trị', 'Thêm món ăn thiếu tên', 'name=""', '400', short(r), r.status === 400);

    r = await admin.post('/api/admin/products', { ...productBody, name: `QA Âm ${RUN}`, price: -1000 });
    record('TC-AD-06', 'Quản trị', 'Thêm món ăn giá âm', 'price=-1000', '400', short(r), r.status === 400);

    r = await admin.put(`/api/admin/products/${productId}`, { ...productBody, price: 55000 });
    record('TC-AD-07', 'Quản trị', 'Sửa giá món ăn', `id=${productId}, price=55000`,
        '200, price=55000', short(r), r.status === 200 && Number(r.json?.data?.price) === 55000);

    r = await admin.del(`/api/admin/categories/${existingCategory?.id}`);
    record('TC-AD-08', 'Quản trị', 'Xóa danh mục đang có món', `categoryId=${existingCategory?.id} (${existingCategory?.name})`,
        '400 "Chỉ được xóa danh mục chưa có món ăn"', short(r), r.status === 400);

    r = await admin.del(`/api/admin/categories/${categoryId}`);
    record('TC-AD-16', 'Quản trị', 'Xóa danh mục chưa có món', `categoryId=${categoryId} ("${catName}")`,
        '200 deleted', short(r), r.status === 200 && r.json?.data?.deleted === true);

    const tableName = `QA Bàn ${RUN}`;
    r = await admin.post('/api/admin/tables', { tableName });
    const tableId = r.json?.data?.id;
    record('TC-AD-09', 'Quản trị', 'Thêm bàn mới', `tableName="${tableName}"`,
        '200, trạng thái AVAILABLE, có mã QR', short(r),
        r.status === 200 && r.json?.data?.status === 'AVAILABLE' && !!r.json?.data?.qrCodeUrl);
    const tableQr = r.json?.data?.qrCodeUrl;

    r = await admin.put(`/api/admin/tables/${tableId}`, { tableName: `${tableName} (đổi tên)` });
    record('TC-AD-10', 'Quản trị', 'Đổi tên bàn trống', `id=${tableId}`, '200', short(r), r.status === 200);

    let ev = await waitForEvent(kitchenWs, 'TABLES_UPDATED');
    r = await kitchen.get('/api/kitchen/tables');
    const kitchenTable = (r.json?.data || []).find((t) => t.id === tableId);
    record('TC-RT-04', 'Real-time', 'Màn hình bếp nhận sự kiện khi admin thêm/đổi tên bàn', 'Admin đổi tên bàn -> WS /ws/kitchen',
        'Nhận TABLES_UPDATED, /api/kitchen/tables có tên mới', `event=${!!ev} name=${kitchenTable?.name}`,
        !!ev && kitchenTable?.name === `${tableName} (đổi tên)`);
    ev = await waitForEvent(adminWs, 'TABLES_UPDATED');
    record('TC-RT-05', 'Real-time', 'Trang admin khác nhận sự kiện khi bàn thay đổi', 'Admin đổi tên bàn -> WS /ws/admin',
        'Nhận TABLES_UPDATED', `event=${!!ev}`, !!ev);
    ev = await waitForEvent(adminWs, 'MENU_UPDATED');
    record('TC-RT-06', 'Real-time', 'Trang admin nhận sự kiện khi thực đơn thay đổi', 'Admin thêm/sửa món -> WS /ws/admin',
        'Nhận MENU_UPDATED', `event=${!!ev}`, !!ev);

    // ---------------- KHACH HANG: goi mon ----------------
    r = await customer.get(`/menu?qr=${tableQr}`);
    record('TC-KH-01', 'Khách hàng', 'Quét mã QR của bàn', `GET /menu?qr=${tableQr}`,
        '302 -> /home/index?qr=...', short(r), r.status === 302 && r.location.includes('/home/index?qr='));
    r = await customer.get(r.location.replace('/SmartDining', ''));
    record('TC-KH-02', 'Khách hàng', 'Trang thực đơn hiển thị món mới thêm', 'GET /home/index?qr=...',
        `200, có "${productBody.name}"`, `HTTP ${r.status}`, r.status === 200 && r.text.includes(productBody.name));

    r = await guest.get('/menu?qr=ma-qr-khong-ton-tai');
    const r2 = await guest.get('/home/index?qr=ma-qr-khong-ton-tai');
    record('TC-KH-03', 'Khách hàng', 'Mã QR không hợp lệ', 'qr=ma-qr-khong-ton-tai',
        '404', `HTTP ${r2.status}`, r2.status === 404);

    r = await guest.get('/api/order/get-cart');
    record('TC-KH-04', 'Khách hàng', 'Xem giỏ hàng khi chưa quét QR', 'GET /api/order/get-cart (không có phiên)',
        '401 "Phien quet QR da het han"', short(r), r.status === 401);

    r = await customer.post('/api/order/add-cart', { productId, quantity: 2, note: 'ít cay' });
    record('TC-KH-05', 'Khách hàng', 'Thêm món vào giỏ', `productId=${productId}, quantity=2, note="ít cay"`,
        '200 success', short(r), r.status === 200 && r.json?.status === 'success');

    r = await customer.post('/api/order/add-cart', { productId, quantity: 0 });
    record('TC-KH-06', 'Khách hàng', 'Thêm món với số lượng 0', 'quantity=0', '400', short(r), r.status === 400);

    r = await customer.post('/api/order/add-cart', { productId, quantity: -3 });
    record('TC-KH-07', 'Khách hàng', 'Thêm món với số lượng âm', 'quantity=-3', '400', short(r), r.status === 400);

    r = await customer.post('/api/order/add-cart', { productId: 'P_KHONG_CO', quantity: 1 });
    record('TC-KH-08', 'Khách hàng', 'Thêm món không tồn tại', 'productId=P_KHONG_CO', '400', short(r), r.status === 400);

    // Them 1 mon co san khac (lay tu danh sach mon) de thu chuc nang huy mon trong gio
    const products = (await admin.get('/api/admin/products')).json?.data || [];
    const otherProductId = products.find((p) => p.status === 'AVAILABLE' && p.id !== productId)?.id;
    await customer.post('/api/order/add-cart', { productId: otherProductId, quantity: 1 });
    r = await customer.get('/api/order/get-cart');
    const cartItems = r.json?.items || [];
    record('TC-KH-09', 'Khách hàng', 'Xem giỏ hàng sau khi thêm 2 món', 'GET /api/order/get-cart',
        '200, có 2 món vừa thêm', `${short(r).slice(0, 80)} (items=${cartItems.length})`,
        r.status === 200 && cartItems.some((i) => i.productId === productId)
        && cartItems.some((i) => i.productId === otherProductId));

    const toRemove = cartItems.find((i) => i.productId === otherProductId);
    r = await customer.post('/api/order/remove-cart-item', { orderDetailId: toRemove?.id });
    record('TC-KH-10', 'Khách hàng', 'Hủy món chưa gửi bếp', `orderDetailId=${toRemove?.id} (${otherProductId})`,
        '200 success', short(r), r.status === 200);

    // Tang / giam so luong mon chua gui bep
    const cartLine = cartItems.find((i) => i.productId === productId);
    r = await customer.post('/api/order/update-cart-item', { orderDetailId: cartLine?.id, quantity: 3 });
    const afterInc = ((await customer.get('/api/order/get-cart')).json?.items || []).find((i) => i.id === cartLine?.id);
    record('TC-KH-18', 'Khách hàng', 'Tăng số lượng món trong giỏ', `orderDetailId=${cartLine?.id}, quantity 2 -> 3`,
        '200, giỏ hàng có qty=3', `${short(r)} qty=${afterInc?.qty}`, r.status === 200 && afterInc?.qty === 3);
    r = await customer.post('/api/order/update-cart-item', { orderDetailId: cartLine?.id, quantity: 2 });
    const afterDec = ((await customer.get('/api/order/get-cart')).json?.items || []).find((i) => i.id === cartLine?.id);
    record('TC-KH-19', 'Khách hàng', 'Giảm số lượng món trong giỏ', 'quantity 3 -> 2',
        '200, giỏ hàng có qty=2', `${short(r)} qty=${afterDec?.qty}`, r.status === 200 && afterDec?.qty === 2);
    r = await customer.post('/api/order/update-cart-item', { orderDetailId: cartLine?.id, quantity: 0 });
    record('TC-KH-20', 'Khách hàng', 'Giảm số lượng về 0', 'quantity=0', '400 (dùng "Hủy món" để bỏ món)', short(r), r.status === 400);
    r = await customer.post('/api/order/update-cart-item', { orderDetailId: cartLine?.id, quantity: 101 });
    record('TC-KH-21', 'Khách hàng', 'Số lượng vượt giới hạn', 'quantity=101', '400', short(r), r.status === 400);

    r = await customer.post('/api/order/confirm');
    record('TC-KH-11', 'Khách hàng', 'Xác nhận gửi đơn xuống bếp', 'POST /api/order/confirm',
        '200 "Don da duoc gui xuong bep"', short(r), r.status === 200);

    r = await customer.post('/api/order/update-cart-item', { orderDetailId: cartLine?.id, quantity: 5 });
    record('TC-KH-22', 'Khách hàng', 'Đổi số lượng món đã gửi bếp', `orderDetailId=${cartLine?.id}, quantity=5`,
        '400 "Chi doi duoc so luong mon chua gui bep"', short(r), r.status === 400);

    r = await admin.get('/api/admin/overview/details?type=open');
    const openRow = (r.json?.data || []).find((row) => row.tableName === `${tableName} (đổi tên)`);
    record('TC-AD-17', 'Quản trị', 'Tổng quan: chi tiết "Bàn đang phục vụ"', 'GET /api/admin/overview/details?type=open',
        '200, có bàn test với 2 món đã gửi bếp', `${short(r).slice(0, 60)} sent=${openRow?.sentCount}`,
        r.status === 200 && openRow?.sentCount === 2);
    r = await admin.get('/api/admin/overview/details?type=khong-co');
    record('TC-AD-18', 'Quản trị', 'Tổng quan: loại chi tiết không hợp lệ', 'type=khong-co', '400', short(r), r.status === 400);

    r = await customer.post('/api/order/confirm');
    record('TC-KH-12', 'Khách hàng', 'Xác nhận khi giỏ trống', 'POST /api/order/confirm (lần 2)',
        '400 "Gio hang dang trong"', short(r), r.status === 400);

    r = await customer.get('/api/order/get-cart');
    const sentItem = (r.json?.items || []).find((i) => i.productId === productId);
    const detailId = sentItem?.id;
    r = await customer.post('/api/order/remove-cart-item', { orderDetailId: detailId });
    record('TC-KH-13', 'Khách hàng', 'Hủy món đã gửi bếp', `orderDetailId=${detailId}`,
        '400 "Chi co the huy mon chua gui bep"', short(r), r.status === 400);

    r = await customer.post('/api/order/request-service', { type: 'Thêm nước lọc', note: '2 ly' });
    record('TC-KH-14', 'Khách hàng', 'Gửi yêu cầu phục vụ', 'type="Thêm nước lọc", note="2 ly"',
        '200 success', short(r), r.status === 200);
    r = await customer.post('/api/order/request-service', { type: '', note: '' });
    record('TC-KH-15', 'Khách hàng', 'Gửi yêu cầu phục vụ rỗng', 'type=""', '400', short(r), r.status === 400);

    // ---------------- BEP ----------------
    r = await kitchen.get('/api/kitchen/tasks');
    const task = (r.json?.data || []).find((t) => t.id === detailId);
    record('TC-BEP-01', 'Bếp', 'Món vừa gửi xuất hiện trên màn hình bếp', 'GET /api/kitchen/tasks',
        `200, có món ${detailId} trạng thái PENDING`, `${short(r).slice(0, 60)} found=${!!task} status=${task?.status}`,
        r.status === 200 && task?.status === 'PENDING');

    r = await kitchen.post('/api/kitchen/update-status', { orderDetailId: detailId, status: 'PREPARING' });
    record('TC-BEP-02', 'Bếp', 'Chuyển món sang Đang nấu', 'status=PREPARING', '200 success', short(r), r.status === 200);
    r = await kitchen.post('/api/kitchen/update-status', { orderDetailId: detailId, status: 'READY' });
    record('TC-BEP-03', 'Bếp', 'Chuyển món sang Đã xong', 'status=READY', '200 success', short(r), r.status === 200);
    r = await kitchen.post('/api/kitchen/update-status', { orderDetailId: detailId, status: 'KHONG_HOP_LE' });
    record('TC-BEP-04', 'Bếp', 'Cập nhật trạng thái không hợp lệ', 'status=KHONG_HOP_LE', '400', short(r), r.status === 400);

    // ---------------- THANH TOAN: dieu kien ----------------
    r = await staff.post('/api/staff/checkout', { tableId, method: 'CASH' });
    record('TC-TT-01', 'Thanh toán', 'Thanh toán khi khách chưa yêu cầu', `tableId=${tableId}, method=CASH`,
        '400 NOT_REQUESTED', short(r), r.status === 400 && r.json?.code === 'NOT_REQUESTED');
    await customer.post('/api/order/request-payment');
    r = await staff.post('/api/staff/checkout', { tableId, method: 'CASH' });
    record('TC-TT-02', 'Thanh toán', 'Thanh toán khi còn món chưa phục vụ', 'Khách đã yêu cầu, món đang ở trạng thái READY',
        '400 ITEMS_NOT_SERVED', short(r), r.status === 400 && r.json?.code === 'ITEMS_NOT_SERVED');

    // ---------------- PHUC VU ----------------
    r = await staff.get(`/api/staff/table-details?tableId=${tableId}`);
    const staffItem = (r.json?.data?.items || []).find((i) => i.id === detailId);
    record('TC-PV-01', 'Phục vụ', 'Xem chi tiết bàn có món đã xong', `tableId=${tableId}`,
        '200, món ở trạng thái READY', `${short(r).slice(0, 60)} status=${staffItem?.status}`,
        r.status === 200 && staffItem?.status === 'READY');
    r = await staff.post('/api/staff/serve', { orderDetailId: detailId });
    record('TC-PV-02', 'Phục vụ', 'Đánh dấu đã mang món ra', `orderDetailId=${detailId}`, '200 success', short(r), r.status === 200);
    r = await staff.post('/api/staff/serve', { orderDetailId: detailId });
    record('TC-PV-03', 'Phục vụ', 'Đánh dấu phục vụ lại món đã phục vụ', `orderDetailId=${detailId} (lần 2)`,
        '400', short(r), r.status === 400);

    const overviewBefore = (await admin.get('/api/admin/overview')).json?.data || {};
    const cartBeforePay = (await customer.get('/api/order/get-cart')).json || {};
    const orderTotal = Math.max(...['currentOrderTotal', 'totalAmount', 'projectedTotal']
        .map((key) => Number(cartBeforePay[key] || 0)));
    r = await customer.post('/api/order/request-payment');
    record('TC-KH-16', 'Khách hàng', 'Gửi yêu cầu thanh toán', 'POST /api/order/request-payment',
        '200 success', short(r), r.status === 200);

    r = await staff.get('/api/staff/requests');
    const reqs = (r.json?.data || []).filter((q) => q.tableId === tableId);
    const payReq = reqs.find((q) => q.requestType === 'PAYMENT');
    const svcReq = reqs.find((q) => q.requestType === 'SERVICE');
    record('TC-PV-04', 'Phục vụ', 'Nhân viên thấy yêu cầu phục vụ và thanh toán của bàn', 'GET /api/staff/requests',
        '200, có 1 SERVICE + 1 PAYMENT', `${short(r).slice(0, 40)} service=${!!svcReq} payment=${!!payReq}`,
        r.status === 200 && !!payReq && !!svcReq);

    r = await staff.post('/api/staff/complete-request', { requestId: svcReq?.id });
    record('TC-PV-05', 'Phục vụ', 'Hoàn tất yêu cầu phục vụ', 'requestId (SERVICE)', '200 success', short(r), r.status === 200);
    r = await staff.post('/api/staff/complete-request', { requestId: payReq?.id });
    record('TC-TT-03', 'Thanh toán', 'Không thể chốt thanh toán bằng "Đã xử lý" yêu cầu', 'complete-request với yêu cầu PAYMENT',
        '400 (phải dùng màn hình Thanh toán)', short(r), r.status === 400);

    r = await staff.get(`/api/staff/bill?tableId=${tableId}`);
    const bill = r.json?.data;
    const billLine = (bill?.items || [])[0];
    record('TC-TT-04', 'Thanh toán', 'Xem hóa đơn chi tiết trước khi thanh toán', `GET /api/staff/bill?tableId=${tableId}`,
        `200, canPay=true, 1 dòng (SL 2 × đơn giá), tổng = ${orderTotal}, có nhân viên phụ trách`,
        `${short(r).slice(0, 40)} canPay=${bill?.canPay} dong=${bill?.items?.length} sl=${billLine?.quantity} tong=${bill?.total} nv=${bill?.cashier}`,
        r.status === 200 && bill?.canPay === true && bill?.items?.length === 1 && billLine?.quantity === 2
        && Number(billLine?.lineTotal) === Number(billLine?.unitPrice) * 2 && Number(bill?.total) === orderTotal && !!bill?.cashier);

    r = await staff.post('/api/staff/checkout', { tableId, method: 'TRANSFER' });
    record('TC-TT-05', 'Thanh toán', 'Chuyển khoản nhưng không có ảnh bill', 'method=TRANSFER, image=null',
        '400 IMAGE_REQUIRED', short(r), r.status === 400 && r.json?.code === 'IMAGE_REQUIRED');
    r = await staff.post('/api/staff/checkout', { tableId, method: 'TRANSFER', image: 'khong-phai-anh' });
    record('TC-TT-06', 'Thanh toán', 'Chuyển khoản với ảnh không hợp lệ', 'image="khong-phai-anh"',
        '400 INVALID_IMAGE', short(r), r.status === 400 && r.json?.code === 'INVALID_IMAGE');
    r = await staff.post('/api/staff/checkout', { tableId, method: 'THE_TIN_DUNG' });
    record('TC-TT-07', 'Thanh toán', 'Phương thức thanh toán không hợp lệ', 'method=THE_TIN_DUNG',
        '400 INVALID_METHOD', short(r), r.status === 400 && r.json?.code === 'INVALID_METHOD');

    // Anh PNG 1x1 lam anh bill mau
    const sampleBill = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
    r = await staff.post('/api/staff/checkout', { tableId, method: 'TRANSFER', image: sampleBill });
    record('TC-PV-06', 'Phục vụ', 'Thanh toán chuyển khoản kèm ảnh bill', 'method=TRANSFER, image=(ảnh PNG)',
        '200 success', short(r), r.status === 200 && r.json?.status === 'success');

    r = await admin.get('/api/admin/overview/details?type=paid');
    const paidRow = (r.json?.data || []).find((row) => row.orderId === bill?.orderId);
    const imageResponse = paidRow?.paymentId ? await admin.get(`/api/admin/payments/${paidRow.paymentId}/image`) : null;
    record('TC-TT-08', 'Thanh toán', 'Hóa đơn lưu phương thức, nhân viên và ảnh bill', 'Admin: overview/details?type=paid + xem ảnh bill',
        'method=TRANSFER, có tên nhân viên, ảnh bill đúng ảnh đã gửi',
        `method=${paidRow?.method} nv=${paidRow?.employee} anh=${imageResponse?.json?.data?.image === sampleBill}`,
        paidRow?.method === 'TRANSFER' && !!paidRow?.employee && imageResponse?.json?.data?.image === sampleBill);

    r = await admin.get('/api/admin/tables');
    const tableAfter = (r.json?.data || []).find((t) => t.id === tableId);
    record('TC-PV-07', 'Phục vụ', 'Sau thanh toán bàn trở về Trống', `tableId=${tableId}`,
        'status=AVAILABLE', `status=${tableAfter?.status}`, tableAfter?.status === 'AVAILABLE');

    // Real-time: admin nhan du su kien van hanh cua ban test
    const forTable = (e) => e.tableId === tableId;
    for (const [tc, status, title] of [
        ['TC-RT-07', 'ORDER_CONFIRMED', 'Trang admin nhận sự kiện khi khách gửi đơn'],
        ['TC-RT-08', 'SERVICE_REQUEST', 'Trang admin nhận sự kiện khi khách yêu cầu phục vụ'],
        ['TC-RT-09', 'PAYMENT_REQUEST', 'Trang admin nhận sự kiện khi khách yêu cầu thanh toán'],
        ['TC-RT-10', 'ORDER_PAID', 'Trang admin nhận sự kiện khi thanh toán xong'],
        ['TC-RT-11', 'TABLE_STATUS_UPDATED', 'Trang admin nhận sự kiện khi trạng thái bàn đổi'],
    ]) {
        ev = await waitForEvent(adminWs, status, forTable);
        record(tc, 'Real-time', title, `Bàn test ${tableId} -> WS /ws/admin`, `Nhận ${status} của bàn ${tableId}`,
            `event=${ev ? JSON.stringify(ev).slice(0, 100) : 'không có'}`, !!ev);
    }

    // Doanh thu tinh theo gio thanh toan (bang Payments)
    const overviewAfter = (await admin.get('/api/admin/overview')).json?.data || {};
    const revenueDelta = Number(overviewAfter.todayRevenue || 0) - Number(overviewBefore.todayRevenue || 0);
    record('TC-RT-12', 'Real-time', 'Doanh thu hôm nay tăng đúng bằng hóa đơn vừa thanh toán', `Tổng hóa đơn = ${orderTotal}`,
        `todayRevenue tăng ${orderTotal}, số hóa đơn +1`,
        `tăng ${revenueDelta}, hóa đơn ${overviewBefore.todayPaidOrders} -> ${overviewAfter.todayPaidOrders}`,
        orderTotal > 0 && revenueDelta === orderTotal
        && overviewAfter.todayPaidOrders === overviewBefore.todayPaidOrders + 1);
    record('TC-RT-13', 'Real-time', 'Tổng quan có số yêu cầu đang chờ xử lý', 'GET /api/admin/overview',
        'Có trường pendingRequests', `pendingRequests=${overviewAfter.pendingRequests}`,
        typeof overviewAfter.pendingRequests === 'number');

    r = await customer.get('/api/order/get-cart');
    record('TC-KH-17', 'Khách hàng', 'Giỏ hàng sau khi đã thanh toán', 'GET /api/order/get-cart',
        '200, không còn món', `${short(r).slice(0, 120)}`, r.status === 200 && (r.json?.items || []).length === 0);

    // ---------------- MANG VE ----------------
    r = await staff.get('/api/staff/menu');
    const menuItem = (r.json?.data || []).find((p) => p.id === productId);
    record('TC-MV-01', 'Mang về', 'Nhân viên xem thực đơn để bán mang về', 'GET /api/staff/menu',
        '200, có món test kèm giá và danh mục', `${short(r).slice(0, 50)} found=${!!menuItem}`, r.status === 200 && !!menuItem?.categoryName);
    const takeawayBody = { items: [{ productId, quantity: 2, note: 'mang đi' }], customerName: `QA Khách ${RUN}`,
        customerPhone: '0901234567', pickupTime: '', method: 'CASH' };
    r = await staff.post('/api/staff/takeaway', { ...takeawayBody, items: [] });
    record('TC-MV-02', 'Mang về', 'Tạo đơn mang về không có món', 'items=[]', '400 EMPTY', short(r), r.status === 400 && r.json?.code === 'EMPTY');
    r = await staff.post('/api/staff/takeaway', { ...takeawayBody, customerPhone: 'abc' });
    record('TC-MV-03', 'Mang về', 'Số điện thoại không hợp lệ', 'customerPhone="abc"', '400 INVALID_CUSTOMER', short(r), r.status === 400 && r.json?.code === 'INVALID_CUSTOMER');
    r = await staff.post('/api/staff/takeaway', { ...takeawayBody, pickupTime: '25:99' });
    record('TC-MV-04', 'Mang về', 'Giờ lấy không hợp lệ', 'pickupTime="25:99"', '400 INVALID_PICKUP_TIME', short(r), r.status === 400 && r.json?.code === 'INVALID_PICKUP_TIME');
    r = await staff.post('/api/staff/takeaway', { ...takeawayBody, method: 'TRANSFER' });
    record('TC-MV-05', 'Mang về', 'Chuyển khoản không có ảnh bill', 'method=TRANSFER, image=null', '400 IMAGE_REQUIRED', short(r), r.status === 400 && r.json?.code === 'IMAGE_REQUIRED');
    r = await staff.post('/api/staff/takeaway', { ...takeawayBody, items: [{ productId, quantity: 0 }] });
    record('TC-MV-06', 'Mang về', 'Số lượng món bằng 0', 'quantity=0', '400 INVALID_ITEM', short(r), r.status === 400 && r.json?.code === 'INVALID_ITEM');
    r = await staff.post('/api/staff/takeaway', takeawayBody);
    const takeaway = r.json?.data;
    const takeawayTotal = Number(menuItem?.price || 0) * 2;
    record('TC-MV-07', 'Mang về', 'Tạo đơn mang về, thanh toán tiền mặt', `${productId} × 2, khách "QA Khách ${RUN}"`,
        `200, có mã lấy đồ, tổng = ${takeawayTotal}`, `${short(r)}`,
        r.status === 200 && Number.isInteger(takeaway?.pickupCode) && Number(takeaway?.total) === takeawayTotal);

    r = await kitchen.get('/api/kitchen/tasks');
    const takeawayTask = (r.json?.data || []).find((t) => t.groupKey === `order-${takeaway?.orderId}`);
    record('TC-MV-08', 'Mang về', 'Món mang về xuống bếp ngay sau khi thanh toán', 'GET /api/kitchen/tasks',
        `có món của "Mang về #${takeaway?.pickupCode}", không có bàn`, `group=${takeawayTask?.groupName} type=${takeawayTask?.orderType} tableId=${takeawayTask?.tableId}`,
        takeawayTask?.groupName === `Mang về #${takeaway?.pickupCode}` && takeawayTask?.orderType === 'TAKEAWAY' && takeawayTask?.tableId == null);
    r = await staff.post('/api/staff/takeaway/deliver', { orderId: takeaway?.orderId });
    record('TC-MV-09', 'Mang về', 'Giao đồ khi bếp chưa làm xong', 'POST /api/staff/takeaway/deliver', '400 NOT_READY', short(r), r.status === 400 && r.json?.code === 'NOT_READY');
    await kitchen.post('/api/kitchen/update-status', { orderDetailId: takeawayTask?.id, status: 'PREPARING' });
    r = await kitchen.post('/api/kitchen/update-status', { orderDetailId: takeawayTask?.id, status: 'READY' });
    let takeawayList = (await staff.get('/api/staff/takeaways')).json?.data || [];
    let takeawayRow = takeawayList.find((o) => o.orderId === takeaway?.orderId);
    record('TC-MV-10', 'Mang về', 'Bếp làm xong thì đơn chuyển sang "Chờ giao"', 'Bếp: PREPARING → READY',
        'state=ready', `state=${takeawayRow?.state}`, takeawayRow?.state === 'ready');
    r = await staff.post('/api/staff/takeaway/deliver', { orderId: takeaway?.orderId });
    takeawayList = (await staff.get('/api/staff/takeaways')).json?.data || [];
    takeawayRow = takeawayList.find((o) => o.orderId === takeaway?.orderId);
    record('TC-MV-11', 'Mang về', 'Giao đồ cho khách', 'POST /api/staff/takeaway/deliver', '200, state=delivered',
        `${short(r)} state=${takeawayRow?.state}`, r.status === 200 && takeawayRow?.state === 'delivered');
    r = await staff.post('/api/staff/takeaway/deliver', { orderId: takeaway?.orderId });
    record('TC-MV-12', 'Mang về', 'Giao lại đơn đã giao', 'deliver lần 2', '400 ALREADY_DELIVERED', short(r), r.status === 400 && r.json?.code === 'ALREADY_DELIVERED');
    r = await admin.get('/api/admin/overview/details?type=paid');
    const takeawayPaid = (r.json?.data || []).find((row) => row.orderId === takeaway?.orderId);
    record('TC-MV-13', 'Mang về', 'Doanh thu hôm nay có đơn mang về', 'Admin: overview/details?type=paid',
        `có "Mang về #${takeaway?.pickupCode}", phương thức CASH`, `tableName=${takeawayPaid?.tableName} method=${takeawayPaid?.method}`,
        takeawayPaid?.tableName === `Mang về #${takeaway?.pickupCode}` && takeawayPaid?.method === 'CASH');

    // ---------------- BAO CAO HOAT DONG NHAN VIEN ----------------
    const today = new Date().toLocaleDateString('sv-SE');
    r = await admin.get(`/api/admin/staff-activity?from=${today}&to=${today}`);
    const activity = r.json?.data || [];
    const staffRow = activity.find((row) => row.username === 'staff1');
    const kitchenRow = activity.find((row) => row.username === 'kitchen1');
    record('TC-BC-01', 'Báo cáo nhân viên', 'Nhân viên phục vụ: hóa đơn, tiền thu, món mang ra', `staff-activity?from=${today}&to=${today}`,
        'staff1 có ≥ 2 hóa đơn, tiền thu > 0, ≥ 3 món đã mang ra, có yêu cầu đã xử lý',
        `hd=${staffRow?.invoices} tien=${staffRow?.revenue} mon=${staffRow?.itemsServed} yc=${staffRow?.requests}`,
        r.status === 200 && staffRow?.invoices >= 2 && Number(staffRow?.revenue) > 0 && staffRow?.itemsServed >= 3 && staffRow?.requests >= 1);
    record('TC-BC-02', 'Báo cáo nhân viên', 'Nhân viên bếp: số món đã nấu và thời gian nấu', `staff-activity?from=${today}&to=${today}`,
        'kitchen1 có ≥ 4 món đã nấu (2 tại bàn + 2 mang về), có thời gian nấu TB',
        `mon=${kitchenRow?.itemsCooked} nauTB=${kitchenRow?.avgCookMinutes}`,
        kitchenRow?.itemsCooked >= 4 && kitchenRow?.avgCookMinutes != null);
    r = await admin.get('/api/admin/staff-activity?from=2026-12-31&to=2026-01-01');
    record('TC-BC-03', 'Báo cáo nhân viên', 'Khoảng ngày không hợp lệ', 'from > to', '400', short(r), r.status === 400);
    r = await staff.get('/api/admin/staff-activity');
    record('TC-BC-04', 'Báo cáo nhân viên', 'Nhân viên phục vụ không xem được báo cáo', 'staff1: GET /api/admin/staff-activity', '403', short(r), r.status === 403);

    r = await admin.get('/api/admin/products');
    const soldRow = (r.json?.data || []).find((p) => p.id === productId);
    record('TC-AD-20', 'Quản trị', 'Thực đơn có số phần đã bán', 'GET /api/admin/products',
        'món test đã bán 4 phần (2 tại bàn + 2 mang về)', `soldQuantity=${soldRow?.soldQuantity}`, Number(soldRow?.soldQuantity) === 4);

    // ---------------- ADMIN: ngung ban, xoa mon ----------------
    r = await admin.del(`/api/admin/tables/${tableId}`);
    record('TC-AD-11', 'Quản trị', 'Ngưng sử dụng bàn trống', `DELETE /api/admin/tables/${tableId}`,
        '200 deactivated', short(r), r.status === 200);
    r = await guest.get(`/home/index?tableId=${tableId}`);
    record('TC-AD-12', 'Quản trị', 'Khách không vào được bàn đã ngưng', `GET /home/index?tableId=${tableId}`,
        '404', short(r), r.status === 404);
    r = await admin.put(`/api/admin/tables/${tableId}`, { tableName: `${tableName} (bảo trì)` });
    record('TC-AD-19', 'Quản trị', 'Đổi tên bàn đang ngưng hoạt động', `id=${tableId}`, '200', short(r), r.status === 200);
    r = await admin.post(`/api/admin/tables/${tableId}/activate`, {});
    record('TC-AD-13', 'Quản trị', 'Kích hoạt lại bàn', `POST /api/admin/tables/${tableId}/activate`,
        '200, status=AVAILABLE', short(r), r.status === 200);
    r = await admin.del(`/api/admin/tables/${tableId}`);

    r = await admin.del(`/api/admin/products/${productId}`);
    record('TC-AD-14', 'Quản trị', 'Xóa món đã có trong hóa đơn', `DELETE /api/admin/products/${productId}`,
        '200, archived=true (chuyển Ngưng kinh doanh)', short(r), r.status === 200 && r.json?.data?.archived === true);

    r = await admin.get('/api/admin/reports');
    record('TC-AD-15', 'Quản trị', 'Xem báo cáo doanh thu', 'GET /api/admin/reports', '200, có revenue',
        short(r).slice(0, 100), r.status === 200 && r.json?.data?.revenue != null);

    // ---------------- TAO TAI KHOAN + XAC THUC EMAIL ----------------
    const empBase = { fullName: `QA Nhân viên ${RUN}`, password: 'Matkhau@123', phone: '0900000000', role: 'STAFF' };
    r = await admin.post('/api/admin/employees', { ...empBase, username: `qa${RUN}`, email: '' });
    record('TC-NV-01', 'Tài khoản', 'Tạo tài khoản không có email', `username=qa${RUN}, email=""`,
        '400', short(r), r.status === 400);
    r = await admin.post('/api/admin/employees', { ...empBase, username: `qa${RUN}`, email: 'email-sai' });
    record('TC-NV-02', 'Tài khoản', 'Tạo tài khoản email sai định dạng', 'email="email-sai"', '400', short(r), r.status === 400);
    r = await admin.post('/api/admin/employees', { ...empBase, username: `qa${RUN}`, password: '123', email: `qa${RUN}@example.com` });
    record('TC-NV-03', 'Tài khoản', 'Tạo tài khoản mật khẩu dưới 8 ký tự', 'password="123"', '400', short(r), r.status === 400);

    const newUser = `qa${RUN}`;
    r = await admin.post('/api/admin/employees', { ...empBase, username: newUser, email: `${newUser}@example.com` });
    record('TC-NV-04', 'Tài khoản', 'Tạo tài khoản hợp lệ', `username=${newUser}, email=${newUser}@example.com`,
        '200, status=PENDING', short(r), r.status === 200 && r.json?.data?.status === 'PENDING');

    r = await admin.post('/api/admin/employees', { ...empBase, username: newUser, email: `${newUser}@example.com` });
    record('TC-NV-05', 'Tài khoản', 'Tạo tài khoản trùng tên đăng nhập', `username=${newUser} (lần 2)`, '400', short(r), r.status === 400);

    await sleep(500);
    const emp = new Actor('newEmployee');
    r = await login(emp, 'staff', newUser, 'Matkhau@123');
    record('TC-NV-06', 'Tài khoản', 'Lần đầu đăng nhập yêu cầu nhập mã xác thực', `${newUser} / Matkhau@123`,
        '200, trang "Xác thực tài khoản"', `HTTP ${r.status}`, r.status === 200 && r.text.includes('Xác thực tài khoản'));

    r = await emp.get('/staff/dashboard');
    record('TC-NV-07', 'Tài khoản', 'Chưa xác thực thì không vào được trang làm việc', 'GET /staff/dashboard',
        '302 -> /login', short(r), r.status === 302 && r.location.endsWith('/login'));

    r = await emp.form('/login/verify', { code: '000000' });
    record('TC-NV-08', 'Tài khoản', 'Nhập sai mã xác thực', 'code=000000',
        '200, báo "Ma xac thuc khong dung. Con 4 lan thu"', `HTTP ${r.status}`,
        r.status === 200 && r.text.includes('Con 4 lan thu'));

    r = await emp.form('/login/verify/resend', {});
    record('TC-NV-09', 'Tài khoản', 'Gửi lại mã ngay lập tức (chưa đủ 60 giây)', 'POST /login/verify/resend',
        '200, báo "Vui long doi ... giay"', `HTTP ${r.status}`, r.status === 200 && r.text.includes('Vui long doi'));

    const code = await devCodeFor(newUser);
    r = await emp.form('/login/verify', { code: code ?? '' });
    record('TC-NV-10', 'Tài khoản', 'Nhập đúng mã xác thực', `code=(mã trong email / log DEV)`,
        '302 -> /staff/dashboard', short(r), !!code && r.status === 302 && r.location.endsWith('/staff/dashboard'));

    r = await admin.get('/api/admin/employees');
    const created = (r.json?.data || []).find((e) => e.username === newUser);
    record('TC-NV-11', 'Tài khoản', 'Sau khi xác thực tài khoản thành Đang hoạt động', `username=${newUser}`,
        'status=ACTIVE', `status=${created?.status}`, created?.status === 'ACTIVE');

    r = await emp.get('/staff/dashboard');
    record('TC-NV-12', 'Tài khoản', 'Nhân viên mới vào được trang làm việc', 'GET /staff/dashboard', '200', short(r), r.status === 200);

    r = await emp.form('/login/verify', { code: code ?? '' });
    record('TC-NV-13', 'Tài khoản', 'Dùng lại mã sau khi đã xác thực', 'POST /login/verify',
        '302 -> /login (không còn phiên chờ xác thực)', short(r), r.status === 302 && r.location.endsWith('/login'));

    // nhap sai 5 lan
    const user2 = `qb${RUN}`;
    await admin.post('/api/admin/employees', { ...empBase, role: 'KITCHEN', username: user2, email: `${user2}@example.com` });
    await sleep(500);
    const emp2 = new Actor('employee2');
    await login(emp2, 'kitchen', user2, 'Matkhau@123');
    for (let i = 0; i < 4; i++) await emp2.form('/login/verify', { code: '999999' });
    r = await emp2.form('/login/verify', { code: '999999' });
    record('TC-NV-14', 'Tài khoản', 'Nhập sai mã 5 lần', 'code=999999 x5',
        '200, báo "nhap sai qua nhieu lan"', `HTTP ${r.status}`, r.status === 200 && r.text.includes('qua nhieu lan'));
    const code2 = await devCodeFor(user2);
    r = await emp2.form('/login/verify', { code: code2 ?? '' });
    record('TC-NV-15', 'Tài khoản', 'Nhập đúng mã sau khi đã sai 5 lần', 'code=(mã đúng)',
        '200, vẫn bị chặn, yêu cầu gửi lại mã', `${short(r)}`, r.status === 200 && r.text.includes('qua nhieu lan'));

    const lockResponse = await admin.put(`/api/admin/employees/${created?.id}`, { fullName: created?.fullName, phone: '', email: created?.email, status: 'INACTIVE', password: '' });
    r = await emp.get('/staff/dashboard');
    record('TC-NV-16', 'Tài khoản', 'Khóa tài khoản đang đăng nhập thì bị đăng xuất', `admin khóa ${newUser}`,
        '302 -> /login', `khóa: ${short(lockResponse).slice(0, 80)} | vào lại: ${short(r)}`, r.status === 302 && r.location.endsWith('/login'));

    // 4 lan: tao qa..., xac thuc qa..., tao qb..., khoa qa... (cho toi da 4 giay)
    let employeeEvents = 0;
    for (let i = 0; i < 40 && employeeEvents < 4; i++) {
        employeeEvents = adminWs.events.filter((e) => e.status === 'EMPLOYEE_UPDATED').length;
        if (employeeEvents < 4) await sleep(100);
    }
    record('TC-RT-14', 'Real-time', 'Trang admin nhận sự kiện khi nhân sự thay đổi', 'Tạo tài khoản, xác thực email, khóa tài khoản',
        'Nhận EMPLOYEE_UPDATED (ít nhất 4 lần)', `số lần nhận=${employeeEvents}`, employeeEvents >= 4);
    adminWs.ws.close();
    kitchenWs.ws.close();

    // ---------------- DANG XUAT ----------------
    r = await staff.get('/logout');
    const r3 = await staff.get('/staff/dashboard');
    record('TC-AUTH-11', 'Xác thực', 'Đăng xuất rồi vào lại trang làm việc', 'GET /logout -> GET /staff/dashboard',
        '302 -> /login', short(r3), r3.status === 302 && r3.location.endsWith('/login'));

    const pass = results.filter((x) => x.result === 'PASS').length;
    console.log(`\nTong: ${results.length} | PASS: ${pass} | FAIL: ${results.length - pass}`);
    writeFileSync(OUT, JSON.stringify({ runAt: new Date().toISOString(), baseUrl: BASE, results }, null, 2));
}

cleanupTestData('truoc khi test');
main()
    .catch((e) => { console.error(e); process.exitCode = 1; })
    .finally(() => {
        if (KEEP_DATA) {
            console.log('[don du lieu] --keep-data: giu lai du lieu test (xoa sau bang cleanup-test-data.sql)');
        } else {
            cleanupTestData('sau khi test');
        }
    });
