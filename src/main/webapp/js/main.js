let currentQty = 1;
let currentPrice = 0;
let cartItems = [];
let confirmedTotalAmount = 0;
let currentOrderTotal = 0;
let projectedTotal = 0;
let customerSocket;
let customerToastTimer;

const orderApiUrl = (path) => `${contextPath}/api/order/${path}`;

const formatPrice = (price) => `${new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 0
}).format(Number(price) || 0)} đ`;

const escapeHtml = (value) => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.price-display[data-price]').forEach((element) => {
        element.textContent = formatPrice(element.dataset.price);
    });
    fetchCartData();
    connectCustomerWebSocket();
});

function connectCustomerWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    customerSocket = new WebSocket(protocol + window.location.host + contextPath
        + '/ws/customer?qr=' + encodeURIComponent(customerQrToken));

    customerSocket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.status === 'MENU_UPDATED') {
                window.location.reload();
                return;
            }
            if (data.status === 'ITEM_CANCELLED') {
                const cancelledItem = cartItems.find((item) => String(item.id) === String(data.orderDetailId));
                const itemName = cancelledItem?.name || 'Một món trong đơn';
                showCustomerToast(`${itemName} đã hết hàng và được bếp hủy. Vui lòng chọn món khác.`);
                fetchCartData();
                return;
            }
            if (data.status === 'STATUS_UPDATED') {
                const updatedItem = cartItems.find((item) => String(item.id) === String(data.orderDetailId));
                const itemName = updatedItem?.name || 'Món ăn của bạn';
                if (data.newStatus === 'READY') {
                    showCustomerToast(`${itemName} đã được bếp chuẩn bị xong.`);
                } else if (data.newStatus === 'SERVED') {
                    showCustomerToast(`Nhân viên đã phục vụ ${itemName}.`);
                }
                fetchCartData();
                return;
            }
            if (data.status === 'ORDER_PAID') {
                showCustomerToast('Nhân viên đã xác nhận thanh toán. Cảm ơn bạn!');
                fetchCartData();
                return;
            }
            if (['CART_UPDATED', 'STATUS_UPDATED', 'ORDER_PAID'].includes(data.status)) {
                fetchCartData();
            }
        } catch (_) {
            // Ignore malformed realtime messages.
        }
    };

    customerSocket.onclose = () => window.setTimeout(connectCustomerWebSocket, 5000);
}

function filterCategory(categoryId, button) {
    document.querySelectorAll('.cat-btn').forEach((element) => element.classList.remove('active'));
    button.classList.add('active');
    document.querySelectorAll('.product-card').forEach((card) => {
        // '' giu nguyen kieu hien thi cua the (flex), chi an khi khong thuoc danh muc
        card.style.display = categoryId === 'all' || card.dataset.categoryId === String(categoryId) ? '' : 'none';
    });
}

function showCustomerToast(message) {
    const toast = document.getElementById('customerToast');
    const toastMessage = document.getElementById('customerToastMessage');
    if (!toast || !toastMessage) return;

    toastMessage.textContent = message;
    toast.classList.add('show');
    window.clearTimeout(customerToastTimer);
    customerToastTimer = window.setTimeout(() => toast.classList.remove('show'), 7000);
}

function openProductModal(id, name, price) {
    document.getElementById('modalProductId').value = id;
    document.getElementById('modalProductName').textContent = name;
    currentPrice = price;
    currentQty = 1;
    document.getElementById('modalQuantity').textContent = currentQty;
    document.getElementById('modalPrice').textContent = formatPrice(currentPrice);
    document.getElementById('modalNote').value = '';
    document.getElementById('productModal').classList.add('active');
}

function closeProductModal() {
    document.getElementById('productModal').classList.remove('active');
}

function changeQty(delta) {
    const nextQty = currentQty + delta;
    if (nextQty < 1 || nextQty > 100) {
        return;
    }
    currentQty = nextQty;
    document.getElementById('modalQuantity').textContent = currentQty;
    document.getElementById('modalPrice').textContent = formatPrice(currentPrice * currentQty);
}

async function fetchCartData() {
    try {
        const response = await fetch(orderApiUrl('get-cart'));
        const result = await response.json();
        if (result.status === 'success') {
            cartItems = result.items || [];
            confirmedTotalAmount = result.totalAmount || 0;
            currentOrderTotal = result.currentOrderTotal || 0;
            projectedTotal = result.projectedTotal || confirmedTotalAmount + currentOrderTotal;
            updateCartUI();
        }
    } catch (error) {
        console.error('Cannot load cart', error);
    }
}

async function addToCart() {
    const button = document.querySelector('#productModal .primary-btn');
    const originalHtml = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang thêm...';
    try {
        const response = await fetch(orderApiUrl('add-cart'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productId: document.getElementById('modalProductId').value,
                quantity: currentQty,
                note: document.getElementById('modalNote').value
            })
        });
        const result = await response.json();
        if (result.status === 'success') {
            await fetchCartData();
            closeProductModal();
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('Cannot add item', error);
    } finally {
        button.disabled = false;
        button.innerHTML = originalHtml;
    }
}

// Bo tang/giam so luong: chi cho mon chua gui bep. Giam ve 0 thi dung nut "Huy mon".
function cartQtyStepper(item) {
    const id = escapeHtml(item.id);
    return `<span class="cart-qty" role="group" aria-label="Số lượng">
        <button type="button" class="cart-qty-btn" aria-label="Giảm số lượng" data-order-detail-id="${id}" data-qty="${item.qty - 1}" onclick="changeCartItemQty(this)" ${item.qty <= 1 ? 'disabled' : ''}><i class="fa-solid fa-minus"></i></button>
        <span class="cart-qty-value">${item.qty}</span>
        <button type="button" class="cart-qty-btn" aria-label="Tăng số lượng" data-order-detail-id="${id}" data-qty="${item.qty + 1}" onclick="changeCartItemQty(this)" ${item.qty >= 100 ? 'disabled' : ''}><i class="fa-solid fa-plus"></i></button>
    </span>`;
}

async function changeCartItemQty(button) {
    const group = button.closest('.cart-qty');
    group.querySelectorAll('button').forEach((element) => { element.disabled = true; });
    try {
        const response = await fetch(orderApiUrl('update-cart-item'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderDetailId: button.dataset.orderDetailId, quantity: Number(button.dataset.qty) })
        });
        const result = await response.json();
        if (result.status !== 'success') {
            showCustomerToast(result.message || 'Không thể đổi số lượng.');
        }
    } catch (error) {
        console.error('Cannot update cart item quantity', error);
    } finally {
        await fetchCartData();
    }
}

async function removeCartItem(orderDetailId) {
    if (!confirm('Hủy món này khỏi đợt gọi hiện tại?')) {
        return;
    }

    try {
        const response = await fetch(orderApiUrl('remove-cart-item'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderDetailId })
        });
        const result = await response.json();
        if (result.status === 'success') {
            await fetchCartData();
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('Cannot remove cart item', error);
    }
}

function toggleCart() {
    const cartIsOpen = document.getElementById('cartSidebar').classList.toggle('active');
    document.body.classList.toggle('cart-open', cartIsOpen);
    fetchCartData();
}

function updateCartUI() {
    let html = '';
    let itemCount = 0;
    if (cartItems.length === 0) {
        html = '<div class="empty-cart">Giỏ hàng đang trống.</div>';
    } else {
        const statusOrder = { CART: 0, PENDING: 1, PREPARING: 2, READY: 3, SERVED: 4, CANCELLED: 5 };
        const sortedItems = [...cartItems].sort((first, second) => {
            const statusDifference = (statusOrder[first.status] ?? 99) - (statusOrder[second.status] ?? 99);
            if (statusDifference !== 0) {
                return statusDifference;
            }
            return new Date(second.createdTime || 0) - new Date(first.createdTime || 0);
        });

        const labels = {
            CART: ['Chưa gửi bếp', 'neutral'],
            PENDING: ['Chờ bếp nhận', 'info'],
            PREPARING: ['Đang chế biến', 'warning'],
            READY: ['Chờ mang ra', 'success'],
            SERVED: ['Đã phục vụ', 'success'],
            CANCELLED: ['Đã hủy', 'danger']
        };
        let currentGroup = null;
        sortedItems.forEach((item) => {
            itemCount += item.qty;
            const group = item.status === 'CART' ? 'Chưa gửi bếp' : 'Đã gửi bếp';
            if (group !== currentGroup) {
                html += `<p class="cart-group-title">${group}</p>`;
                currentGroup = group;
            }
            const [label, tone] = labels[item.status] || [item.status, 'neutral'];
            const safeName = escapeHtml(item.name);
            const safeNote = escapeHtml(item.note);
            html += `
                <div class="cart-item ${item.status === 'SERVED' ? 'cart-item-served' : ''}">
                    <div class="cart-item-head">
                        <span class="cart-item-name">${safeName}${item.status === 'CART' ? '' : ` × ${item.qty}`}</span>
                        <span class="cart-item-price">${formatPrice(item.price * item.qty)}</span>
                    </div>
                    <div class="cart-item-meta">
                        ${item.status === 'CART' ? cartQtyStepper(item) : `<span class="sd-pill sd-pill--${tone}">${escapeHtml(label)}</span>`}
                        ${safeNote ? `<span class="cart-item-note"><i class="fa-regular fa-note-sticky"></i> ${safeNote}</span>` : ''}
                        ${item.status === 'CART' ? `<button type="button" class="cart-remove-btn" data-order-detail-id="${escapeHtml(item.id)}" onclick="removeCartItem(this.dataset.orderDetailId)"><i class="fa-solid fa-trash-can"></i> Hủy món</button>` : ''}
                    </div>
                </div>`;
        });
    }
    document.getElementById('cartCountBadge').textContent = itemCount;
    document.getElementById('cartTotalAmount').textContent = formatPrice(confirmedTotalAmount);
    document.getElementById('cartItemsContainer').innerHTML = html;
    document.getElementById('currentOrderTotal').textContent = formatPrice(currentOrderTotal);
    document.getElementById('projectedTotal').textContent = formatPrice(projectedTotal);
    document.getElementById('confirmedOrderTotal').textContent = formatPrice(confirmedTotalAmount);
    const hasCurrentOrder = currentOrderTotal > 0;
    document.getElementById('projectedTotalRow').classList.toggle('is-hidden', !hasCurrentOrder);
    document.getElementById('cartTotals').classList.toggle('has-projected', hasCurrentOrder);
}

async function confirmOrder() {
    if (!cartItems.some((item) => item.status === 'CART')) {
        alert('Không có món mới trong giỏ để gửi bếp.');
        return;
    }
    const button = document.querySelector('.checkout-btn');
    button.disabled = true;
    try {
        const response = await fetch(orderApiUrl('confirm'), { method: 'POST' });
        const result = await response.json();
        if (result.status === 'success') {
            await fetchCartData();
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('Cannot confirm order', error);
    } finally {
        button.disabled = false;
    }
}

function openServiceModal() {
    document.getElementById('serviceNote').value = '';
    document.getElementById('serviceModal').classList.add('active');
}

function closeServiceModal() {
    document.getElementById('serviceModal').classList.remove('active');
}

async function requestService() {
    const response = await fetch(orderApiUrl('request-service'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            type: document.getElementById('serviceType').value,
            note: document.getElementById('serviceNote').value
        })
    });
    const result = await response.json();
    if (result.status === 'success') {
        closeServiceModal();
    } else {
        alert(result.message);
    }
}

async function requestPayment() {
    if (currentOrderTotal > 0) {
        alert('Vui lòng bấm Đặt món cho đợt gọi hiện tại trước khi yêu cầu thanh toán.');
        return;
    }
    if (confirmedTotalAmount <= 0) {
        alert('Bạn chưa gọi món nào.');
        return;
    }
    try {
        const response = await fetch(orderApiUrl('request-payment'), { method: 'POST' });
        const result = await response.json();
        if (result.status === 'success') {
            toggleCart();
            alert('Yêu cầu thanh toán đã được gửi tới nhân viên.');
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('Cannot request payment', error);
    }
}
