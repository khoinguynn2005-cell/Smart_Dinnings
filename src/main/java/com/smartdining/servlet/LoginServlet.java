package com.smartdining.servlet;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartdining.dao.EmployeeDAO;
import com.smartdining.dao.impl.EmployeeDAOImpl;
import com.smartdining.entity.Employee;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.EmployeeStatus;
import com.smartdining.enums.WebSocketEvent;
import com.smartdining.utils.PasswordUtil;
import com.smartdining.utils.SecurityUtil;
import com.smartdining.utils.VerificationCodeUtil;
import com.smartdining.websocket.AdminWebSocketServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@SuppressWarnings("serial")
@WebServlet({ "/login", "/login/*" })
public class LoginServlet extends HttpServlet {
    /** Id nhan vien da nhap dung mat khau nhung chua xac thuc email. */
    private static final String PENDING_EMPLOYEE_ID = "pendingEmployeeId";
    private static final String VERIFY_PATH = "/verify";
    private static final String RESEND_PATH = "/verify/resend";

    /** Sau bao nhieu lan sai lien tiep thi khoa dang nhap theo ten dang nhap trong LOGIN_LOCK. */
    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(15);
    private static final Map<String, LoginAttempts> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    private final EmployeeDAO employeeDAO = new EmployeeDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (VERIFY_PATH.equals(request.getPathInfo())) {
            Employee pending = findPendingEmployee(request);
            if (pending == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            forwardVerify(request, response, pending, null, null);
            return;
        }

        EmployeeRole loginRole = requestedLoginRole(request);
        if (loginRole == null) {
            forwardLogin(request, response, null);
            return;
        }

        Employee employee = SecurityUtil.getEmployeeForRole(request, loginRole);
        if (employee != null) {
            redirectToWorkspace(request, response, employee);
            return;
        }
        forwardLogin(request, response, loginRole);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (VERIFY_PATH.equals(request.getPathInfo())) {
            verifyCode(request, response);
            return;
        }
        if (RESEND_PATH.equals(request.getPathInfo())) {
            resendCode(request, response);
            return;
        }

        EmployeeRole loginRole = requestedLoginRole(request);
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String throttleKey = username == null ? "" : username.trim().toLowerCase();

        if (isLocked(throttleKey)) {
            request.setAttribute("error", "Dang nhap sai qua nhieu lan. Vui long thu lai sau "
                    + LOGIN_LOCK.toMinutes() + " phut.");
            forwardLogin(request, response, loginRole);
            return;
        }

        Employee employee = employeeDAO.findByUsername(username);
        boolean passwordMatches = employee != null && PasswordUtil.matches(password, employee.getPassword());
        if (!passwordMatches) {
            recordFailedLogin(throttleKey);
        }
        if (employee == null || (loginRole != null && employee.getRole() != loginRole)) {
            request.setAttribute("error", loginRole == null
                    ? "Sai ten dang nhap hoac mat khau."
                    : "Tai khoan khong thuoc khu vuc dang nhap nay.");
            forwardLogin(request, response, loginRole);
            return;
        }
        if (employee.getStatus() == null || employee.getStatus() == EmployeeStatus.INACTIVE) {
            request.setAttribute("error", "Tai khoan cua ban da bi khoa.");
            forwardLogin(request, response, loginRole);
            return;
        }
        if (!passwordMatches) {
            request.setAttribute("error", "Sai ten dang nhap hoac mat khau.");
            forwardLogin(request, response, loginRole);
            return;
        }

        LOGIN_ATTEMPTS.remove(throttleKey);
        if (PasswordUtil.needsRehash(employee.getPassword())) {
            // Mat khau con luu dang chu thuong: ma hoa lai ngay sau lan dang nhap dung dau tien.
            employee.setPassword(PasswordUtil.forStorage(password));
            employeeDAO.update(employee);
        }

        if (employee.getStatus() == EmployeeStatus.PENDING) {
            startVerification(request, response, employee);
            return;
        }

        SecurityUtil.signIn(request, response, employee);
        redirectToWorkspace(request, response, employee);
    }

    private static boolean isLocked(String throttleKey) {
        LoginAttempts attempts = LOGIN_ATTEMPTS.get(throttleKey);
        if (attempts == null || attempts.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isBefore(attempts.lockedUntil())) {
            return true;
        }
        LOGIN_ATTEMPTS.remove(throttleKey, attempts);
        return false;
    }

    private static void recordFailedLogin(String throttleKey) {
        LOGIN_ATTEMPTS.compute(throttleKey, (key, attempts) -> {
            int failures = attempts == null ? 1 : attempts.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILED_LOGINS ? Instant.now().plus(LOGIN_LOCK) : null;
            return new LoginAttempts(failures, lockedUntil);
        });
    }

    private record LoginAttempts(int failures, Instant lockedUntil) {
    }

    /** Lan dau dang nhap: dung mat khau nhung chua xac thuc email -> chuyen sang buoc nhap ma. */
    private void startVerification(HttpServletRequest request, HttpServletResponse response, Employee employee)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute(PENDING_EMPLOYEE_ID, employee.getId());

        String message = null;
        String error = null;
        if (!VerificationCodeUtil.hasActiveCode(employee.getId())) {
            // Ma cu het han, da nhap sai qua nhieu lan, hoac server vua khoi dong lai -> gui ma moi.
            if (VerificationCodeUtil.issueAndSend(employee)) {
                message = "Ma xac thuc moi da duoc gui toi " + maskEmail(employee.getEmail()) + ".";
            } else {
                error = "Tai khoan chua co email. Vui long lien he quan ly.";
            }
        } else {
            message = "Nhap ma xac thuc da duoc gui toi " + maskEmail(employee.getEmail()) + ".";
        }
        forwardVerify(request, response, employee, message, error);
    }

    private void verifyCode(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Employee employee = findPendingEmployee(request);
        if (employee == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        switch (VerificationCodeUtil.verify(employee.getId(), request.getParameter("code"))) {
            case OK -> {
                employee.setStatus(EmployeeStatus.ACTIVE);
                Employee activated = employeeDAO.update(employee);
                if (activated == null) {
                    forwardVerify(request, response, employee, null, "Khong the kich hoat tai khoan. Vui long thu lai.");
                    return;
                }
                request.getSession().removeAttribute(PENDING_EMPLOYEE_ID);
                AdminWebSocketServer.broadcast(Map.of("status", WebSocketEvent.EMPLOYEE_UPDATED.name()));
                SecurityUtil.signIn(request, response, activated);
                redirectToWorkspace(request, response, activated);
            }
            case WRONG -> forwardVerify(request, response, employee, null, "Ma xac thuc khong dung. Con "
                    + VerificationCodeUtil.remainingAttempts(employee.getId()) + " lan thu.");
            case EXPIRED -> forwardVerify(request, response, employee, null,
                    "Ma xac thuc da het han. Bam \"Gui lai ma\" de nhan ma moi.");
            case TOO_MANY_ATTEMPTS -> forwardVerify(request, response, employee, null,
                    "Ban da nhap sai qua nhieu lan. Bam \"Gui lai ma\" de nhan ma moi.");
        }
    }

    private void resendCode(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Employee employee = findPendingEmployee(request);
        if (employee == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        long wait = VerificationCodeUtil.secondsUntilResend(employee.getId());
        if (wait > 0) {
            forwardVerify(request, response, employee, null, "Vui long doi " + wait + " giay truoc khi gui lai ma.");
            return;
        }
        if (!VerificationCodeUtil.issueAndSend(employee)) {
            forwardVerify(request, response, employee, null, "Tai khoan chua co email. Vui long lien he quan ly.");
            return;
        }
        forwardVerify(request, response, employee,
                "Ma xac thuc moi da duoc gui toi " + maskEmail(employee.getEmail()) + ".", null);
    }

    /** Nhan vien dang cho xac thuc trong session; null neu khong co hoac khong con o trang thai PENDING. */
    private Employee findPendingEmployee(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(PENDING_EMPLOYEE_ID);
        if (!(id instanceof String employeeId)) {
            return null;
        }
        Employee employee = employeeDAO.findById(employeeId);
        if (employee == null || employee.getStatus() != EmployeeStatus.PENDING) {
            session.removeAttribute(PENDING_EMPLOYEE_ID);
            return null;
        }
        return employee;
    }

    private void forwardVerify(HttpServletRequest request, HttpServletResponse response, Employee employee,
            String message, String error) throws ServletException, IOException {
        request.setAttribute("maskedEmail", maskEmail(employee.getEmail()));
        request.setAttribute("verifyUsername", employee.getUsername());
        request.setAttribute("codeMinutes", VerificationCodeUtil.CODE_LIFETIME.toMinutes());
        // Cho dong ho dem nguoc tren trang xac thuc
        request.setAttribute("codeExpiresIn", VerificationCodeUtil.secondsUntilExpiry(employee.getId()));
        request.setAttribute("resendWait", VerificationCodeUtil.secondsUntilResend(employee.getId()));
        request.setAttribute("message", message);
        request.setAttribute("error", error);
        request.getRequestDispatcher("/verify.jsp").forward(request, response);
    }

    /** an***@gmail.com - chi hien 2 ky tu dau de nhan vien nhan ra email cua minh. */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String name = email.substring(0, at);
        return name.substring(0, Math.min(2, name.length())) + "***" + email.substring(at);
    }

    private void forwardLogin(HttpServletRequest request, HttpServletResponse response, EmployeeRole loginRole)
            throws ServletException, IOException {
        if (loginRole != null) {
            request.setAttribute("loginRole", loginRole.name());
            request.setAttribute("loginPath", loginRole.name().toLowerCase());
            request.setAttribute("loginTitle", switch (loginRole) {
                case ADMIN -> "Quan tri vien";
                case STAFF -> "Nhan vien phuc vu";
                case KITCHEN -> "Nhan vien bep";
            });
        }
        request.setAttribute("loginAction", loginRole == null ? "/login" : "/login/" + loginRole.name().toLowerCase());
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    private EmployeeRole requestedLoginRole(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            return null;
        }
        return switch (pathInfo.toLowerCase()) {
            case "/admin" -> EmployeeRole.ADMIN;
            case "/staff" -> EmployeeRole.STAFF;
            case "/kitchen" -> EmployeeRole.KITCHEN;
            default -> null;
        };
    }

    private void redirectToWorkspace(HttpServletRequest request, HttpServletResponse response, Employee employee)
            throws IOException {
        String destination = switch (employee.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case STAFF -> "/staff/dashboard";
            case KITCHEN -> "/kitchen/dashboard";
        };
        response.sendRedirect(request.getContextPath() + destination);
    }
}
