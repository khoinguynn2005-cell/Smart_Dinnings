<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực tài khoản - SmartDining</title>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css?v=20261012-5">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css?v=20261012-5">
</head>
<body>
    <main class="auth-card">
        <div class="auth-mark is-success"><i class="fa-solid fa-envelope-circle-check" aria-hidden="true"></i></div>
        <p class="auth-brand">SmartDining · <c:out value="${verifyUsername}" /></p>
        <h1 class="auth-title">Xác thực tài khoản</h1>
        <p class="auth-subtitle">Lần đăng nhập đầu tiên: nhập mã 6 số đã gửi tới <strong><c:out value="${maskedEmail}" /></strong>.</p>

        <c:if test="${not empty message}"><div class="success-msg" role="status"><i class="fa-solid fa-paper-plane" aria-hidden="true"></i> <span><c:out value="${message}" /></span></div></c:if>
        <c:if test="${not empty error}"><div class="error-msg" role="alert"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> <span><c:out value="${error}" /></span></div></c:if>

        <form id="verifyForm" action="${pageContext.request.contextPath}/login/verify" method="POST">
            <!-- Khong co JS: nhap ma vao o nay. Co JS: o nay an di, thay bang 6 o nho. -->
            <div class="input-group" id="plainCodeGroup">
                <label for="code">Mã xác thực</label>
                <input type="text" id="code" name="code" class="input-field code-field" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" required autofocus>
            </div>
            <div class="code-boxes" id="codeBoxes" hidden aria-label="Mã xác thực 6 số"></div>
            <p class="code-meta" id="expiryText" data-seconds="${codeExpiresIn}">Mã có hiệu lực ${codeMinutes} phút.</p>
            <button type="submit" class="login-btn">Xác thực <i class="fa-solid fa-check" aria-hidden="true"></i></button>
        </form>
        <form action="${pageContext.request.contextPath}/login/verify/resend" method="POST">
            <button type="submit" class="link-btn" id="resendButton" data-seconds="${resendWait}">Gửi lại mã</button>
        </form>
        <p class="auth-footer"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
    </main>

    <script>
        (function () {
            const plainGroup = document.getElementById('plainCodeGroup');
            const plainInput = document.getElementById('code');
            const boxesWrap = document.getElementById('codeBoxes');
            const form = document.getElementById('verifyForm');

            // 6 o nhap ma: tu nhay sang o tiep theo, dan ca ma 6 so vao mot lan.
            const boxes = Array.from({ length: 6 }, (_, index) => {
                const box = document.createElement('input');
                box.className = 'code-box';
                box.inputMode = 'numeric';
                box.maxLength = 1;
                box.autocomplete = index === 0 ? 'one-time-code' : 'off';
                box.setAttribute('aria-label', 'Số thứ ' + (index + 1));
                boxesWrap.appendChild(box);
                return box;
            });
            plainGroup.hidden = true;
            plainInput.required = false;
            boxesWrap.hidden = false;
            boxes[0].focus();

            function fillFrom(start, digits) {
                digits.split('').forEach((digit, offset) => {
                    if (boxes[start + offset]) boxes[start + offset].value = digit;
                });
                const next = Math.min(start + digits.length, boxes.length - 1);
                boxes[next].focus();
            }

            boxes.forEach((box, index) => {
                box.addEventListener('input', () => {
                    const digits = box.value.replace(/\D/g, '');
                    box.value = '';
                    if (digits) fillFrom(index, digits.slice(0, boxes.length - index));
                    if (boxes.every((item) => item.value)) form.requestSubmit();
                });
                box.addEventListener('keydown', (event) => {
                    if (event.key === 'Backspace' && !box.value && index > 0) boxes[index - 1].focus();
                });
                box.addEventListener('paste', (event) => {
                    event.preventDefault();
                    const digits = (event.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
                    if (digits) fillFrom(0, digits);
                    if (boxes.every((item) => item.value)) form.requestSubmit();
                });
            });

            form.addEventListener('submit', (event) => {
                plainInput.value = boxes.map((box) => box.value).join('');
                if (plainInput.value.length !== 6) {
                    event.preventDefault();
                    boxes.find((box) => !box.value).focus();
                }
            });

            // Dong ho hieu luc cua ma va dem nguoc gui lai ma
            const expiryText = document.getElementById('expiryText');
            const resendButton = document.getElementById('resendButton');
            let expiresIn = Number(expiryText.dataset.seconds) || 0;
            let resendIn = Number(resendButton.dataset.seconds) || 0;
            const format = (seconds) => Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');

            function tick() {
                if (expiresIn > 0) {
                    expiryText.textContent = 'Mã còn hiệu lực ' + format(expiresIn) + '.';
                } else if (expiryText.dataset.seconds !== '') {
                    expiryText.textContent = 'Mã đã hết hạn. Bấm "Gửi lại mã" để nhận mã mới.';
                    expiryText.classList.add('is-expired');
                }
                resendButton.disabled = resendIn > 0;
                resendButton.textContent = resendIn > 0 ? 'Gửi lại mã sau ' + resendIn + ' giây' : 'Gửi lại mã';
                expiresIn = Math.max(0, expiresIn - 1);
                resendIn = Math.max(0, resendIn - 1);
            }
            tick();
            window.setInterval(tick, 1000);
        })();
    </script>
</body>
</html>
