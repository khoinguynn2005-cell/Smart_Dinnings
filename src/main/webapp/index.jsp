<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>SmartDining - Đặt món tại bàn</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- FontAwesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261012-7">
</head>
<body>
    <!-- App Header -->
    <header class="app-header glass-effect">
        <div class="header-content">
            <div class="brand">
                <span class="brand-eyebrow"><i class="fa-solid fa-utensils" aria-hidden="true"></i> SmartDining · Bàn số <span id="currentTableNumber"><c:out value="${currentTableId}" default="?" /></span></span>
                <h1>Hôm nay ăn gì?</h1>
            </div>
            <div class="header-actions">
                <button class="icon-btn" type="button" onclick="openServiceModal()" title="Gọi nhân viên" aria-label="Gọi nhân viên"><i class="fa-solid fa-bell-concierge"></i></button>
                <button class="icon-btn" type="button" onclick="toggleCart()" title="Xem giỏ hàng" aria-label="Xem giỏ hàng"><i class="fa-solid fa-bag-shopping"></i></button>
            </div>
        </div>
        
        <!-- Category Filter (Horizontal Scroll) -->
        <div class="category-nav" id="categoryNav">
            <button class="cat-btn active" onclick="filterCategory('all', this)">Tất cả</button>
            <c:if test="${not empty categories}">
                <c:forEach var="c" items="${categories}">
                    <button class="cat-btn" onclick="filterCategory('${c.id}', this)"><c:out value="${c.name}" /></button>
                </c:forEach>
            </c:if>
        </div>
    </header>

    <!-- Main Content: Product List -->
    <main class="main-content">
        <!-- Dynamic Data Loaded via JSTL -->
        <div class="product-grid" id="productGrid">
            <c:choose>
                <c:when test="${not empty products}">
                    <c:forEach var="p" items="${products}">
                        <div class="product-card" data-category-id="${p.category.id}">
                            <c:choose>
                                <c:when test="${not empty p.imageUrl}">
                                    <div class="product-image" style="background-image: url('<c:out value="${p.imageUrl}" />');"></div>
                                </c:when>
                                <c:otherwise>
                                    <div class="product-image is-placeholder" aria-hidden="true"><i class="fa-solid fa-bowl-food"></i></div>
                                </c:otherwise>
                            </c:choose>
                            <div class="product-info">
                                <h3><c:out value="${p.name}" /></h3>
                                <p class="desc"><c:out value="${p.description}" /></p>
                                <div class="price-row">
                                    <span class="price price-display" data-price="${p.price}"><fmt:formatNumber value="${p.price}" type="number" groupingUsed="true" maxFractionDigits="0" /> &#273;</span>
                                    <button class="add-btn" type="button" aria-label="Thêm <c:out value='${p.name}' /> vào giỏ"
                                        data-product-id="<c:out value='${p.id}' />"
                                        data-product-name="<c:out value='${p.name}' />"
                                        data-product-price="${p.price}"
                                        onclick="openProductModal(this.dataset.productId, this.dataset.productName, Number(this.dataset.productPrice))">
                                        <i class="fa-solid fa-plus"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <p class="empty-menu">
                        Hiện chưa có món ăn nào trong thực đơn.
                    </p>
                </c:otherwise>
            </c:choose>
        </div>
    </main>

    <!-- Floating Cart Bar -->
    <div class="floating-cart glass-effect" onclick="toggleCart()">
        <div class="cart-summary">
            <div class="cart-icon-wrapper">
                <i class="fa-solid fa-bag-shopping"></i>
                <span class="cart-badge" id="cartCountBadge">0</span>
            </div>
            <div class="cart-total">
                <span class="label">Tổng hóa đơn</span>
                <span class="amount" id="cartTotalAmount">0đ</span>
            </div>
        </div>
        <button class="view-cart-btn">Xem giỏ hàng <i class="fa-solid fa-chevron-right"></i></button>
    </div>

    <!-- Product Detail Modal (To choose quantity and notes) -->
    <div class="modal-overlay" id="productModal">
        <div class="modal-content glass-panel">
            <div class="modal-header">
                <h2 id="modalProductName">Tên món</h2>
                <button class="close-btn" onclick="closeProductModal()"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="modalProductId">
                <div class="quantity-selector">
                    <button class="qty-btn" onclick="changeQty(-1)"><i class="fa-solid fa-minus"></i></button>
                    <span id="modalQuantity">1</span>
                    <button class="qty-btn" onclick="changeQty(1)"><i class="fa-solid fa-plus"></i></button>
                </div>
                <div class="note-input">
                    <label>Ghi chú cho Bếp</label>
                    <textarea id="modalNote" maxlength="255" placeholder="Ví dụ: Không cay, ít đường..."></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button class="primary-btn" onclick="addToCart()">Thêm vào giỏ <span id="modalPrice"></span></button>
            </div>
        </div>
    </div>

    <!-- Cart Sidebar/Modal -->
    <div class="cart-sidebar" id="cartSidebar">
        <div class="cart-header glass-effect">
            <h2>Giỏ hàng của bàn</h2>
            <button class="close-btn" onclick="toggleCart()"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <div class="cart-body" id="cartItemsContainer">
            <!-- Items will be loaded here via JS -->
            <div class="empty-cart">Giỏ hàng đang trống.</div>
        </div>
        <div class="cart-footer glass-effect">
            <div class="cart-totals" id="cartTotals">
                <div class="amount-detail current-order-row">
                    <span class="amount-label">Đợt gọi hiện tại</span>
                    <strong class="amount-value" id="currentOrderTotal">0đ</strong>
                </div>
                <div class="amount-detail projected-total-row is-hidden" id="projectedTotalRow">
                    <span class="amount-label">Tổng sau khi đặt</span>
                    <strong class="amount-value" id="projectedTotal">0đ</strong>
                </div>
            </div>
            <div class="total-row">
                <span class="total-label">Tổng hóa đơn</span>
                <strong class="total-amount" id="confirmedOrderTotal">0đ</strong>
            </div>
            <div class="cart-actions">
                <button class="primary-btn checkout-btn" type="button" onclick="confirmOrder()"><i class="fa-solid fa-fire-burner"></i> Gửi bếp</button>
                <button class="secondary-btn" type="button" onclick="openServiceModal()"><i class="fa-solid fa-bell-concierge"></i> Gọi nhân viên</button>
                <button class="secondary-btn" type="button" onclick="requestPayment()"><i class="fa-solid fa-receipt"></i> Thanh toán</button>
            </div>
        </div>
    </div>

    <div class="modal-overlay" id="serviceModal">
        <div class="modal-content glass-panel">
            <div class="modal-header">
                <h2>Gọi nhân viên</h2>
                <button class="close-btn" onclick="closeServiceModal()"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div class="modal-body">
                <div class="note-input">
                    <label for="serviceType">Yêu cầu</label>
                    <select id="serviceType">
                        <option value="Lấy thêm dụng cụ ăn uống">Lấy thêm dụng cụ ăn uống</option>
                        <option value="Dọn bàn">Dọn bàn</option>
                        <option value="Gọi nhân viên">Gọi nhân viên</option>
                    </select>
                </div>
                <div class="note-input">
                    <label for="serviceNote">Ghi chú thêm</label>
                    <textarea id="serviceNote" maxlength="255" placeholder="Ví dụ: Cần thêm 3 bộ muỗng nĩa"></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button class="primary-btn" onclick="requestService()">Gửi yêu cầu</button>
            </div>
        </div>
    </div>

    <div id="customerToast" class="customer-toast" role="status" aria-live="assertive">
        <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
        <span id="customerToastMessage"></span>
    </div>

    <script>
        const contextPath = "${pageContext.request.contextPath}";
        const customerTableId = Number("${currentTableId}");
        const customerQrToken = "${currentTableQr}";
    </script>
    <script src="${pageContext.request.contextPath}/js/main.js?v=20261012-2"></script>
</body>
</html>
