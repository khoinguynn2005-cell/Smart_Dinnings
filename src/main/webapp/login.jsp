<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${not empty loginTitle ? loginTitle : 'Đăng nhập'} - SmartDining</title>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css?v=20261012-5">
</head>
<body>
    <main class="login-card auth-card">
        <div class="auth-mark"><i class="fa-solid fa-utensils" aria-hidden="true"></i></div>
        <p class="auth-brand">SmartDining<c:if test="${not empty loginTitle}"> · <c:out value="${loginTitle}" /></c:if></p>
        <h1 class="auth-title">Chào mừng trở lại</h1>
        <p class="auth-subtitle">Đăng nhập hệ thống</p>
        <c:if test="${empty loginRole}">
            <div class="notice-list">
                <div class="notice">
                    <i class="fa-solid fa-qrcode" aria-hidden="true"></i>
                    <div><strong>Khách hàng</strong><span>Vui lòng quét mã QR đặt trên bàn để xem thực đơn và gọi món.</span></div>
                </div>
                <div class="notice">
                    <i class="fa-solid fa-user-tie" aria-hidden="true"></i>
                    <div><strong>Nhân viên</strong><span>Đăng nhập bằng tài khoản được quản lý cấp.</span></div>
                </div>
            </div>
        </c:if>
        <c:if test="${not empty error}"><div class="error-msg" role="alert"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> <span><c:out value="${error}" /></span></div></c:if>
        <form action="${pageContext.request.contextPath}${loginAction}" method="POST">
            <div class="input-group"><label for="username">Tên đăng nhập</label><input id="username" type="text" name="username" class="input-field" autocomplete="username" required autofocus></div>
            <div class="input-group"><label for="password">Mật khẩu</label><input id="password" type="password" name="password" class="input-field" autocomplete="current-password" required></div>
            <button type="submit" class="login-btn">Đăng nhập <i class="fa-solid fa-arrow-right" aria-hidden="true"></i></button>
        </form>
    </main>
</body>
</html>
