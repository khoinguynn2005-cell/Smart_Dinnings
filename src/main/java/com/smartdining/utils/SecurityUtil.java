package com.smartdining.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.smartdining.entity.Employee;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.EmployeeStatus;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public final class SecurityUtil {
    private static final String COOKIE_PREFIX = "SMARTDINING_AUTH_";
    private static final Duration EMPLOYEE_SESSION_LIFETIME = Duration.ofHours(8);
    private static final Map<String, EmployeeSession> EMPLOYEE_SESSIONS = new ConcurrentHashMap<>();

    private SecurityUtil() {
    }

    public static void signIn(HttpServletRequest request, HttpServletResponse response, Employee employee) {
        if (employee == null || employee.getRole() == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            return;
        }

        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        EMPLOYEE_SESSIONS.put(token, new EmployeeSession(employee, Instant.now().plus(EMPLOYEE_SESSION_LIFETIME)));
        addCookie(request, response, employee.getRole(), token, (int) EMPLOYEE_SESSION_LIFETIME.toSeconds());
    }

    public static void signOut(HttpServletRequest request, HttpServletResponse response, EmployeeRole role) {
        if (role == null) {
            return;
        }

        String token = findCookie(request.getCookies(), cookieName(role));
        if (token != null) {
            EMPLOYEE_SESSIONS.remove(token);
        }
        addCookie(request, response, role, "", 0);
    }

    public static void invalidateEmployeeSessions(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return;
        }
        EMPLOYEE_SESSIONS.entrySet().removeIf(entry -> employeeId.equals(entry.getValue().employee().getId()));
    }

    public static Employee getEmployee(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return getEmployee(request.getCookies(), path);
    }

    public static Employee getEmployeeForRole(HttpServletRequest request, EmployeeRole role) {
        if (request == null || role == null) {
            return null;
        }
        return findEmployee(role, findCookie(request.getCookies(), cookieName(role)));
    }

    public static Employee getEmployeeForWebSocket(String path, List<String> cookieHeaders) {
        return getEmployee(parseCookieHeaders(cookieHeaders), path);
    }

    public static Employee getEmployee(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object employee = session.getAttribute("employee");
        return employee instanceof Employee ? (Employee) employee : null;
    }

    public static boolean hasRole(HttpServletRequest request, EmployeeRole... roles) {
        return hasRole(getEmployee(request), roles);
    }

    public static boolean hasRole(HttpSession session, EmployeeRole... roles) {
        return hasRole(getEmployee(session), roles);
    }

    public static boolean hasRole(Employee employee, EmployeeRole... roles) {
        if (employee == null || employee.getRole() == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            return false;
        }
        for (EmployeeRole role : roles) {
            if (employee.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    public static String loginPath(EmployeeRole role) {
        if (role == null) {
            return "/login";
        }
        return switch (role) {
            case ADMIN -> "/login/admin";
            case STAFF -> "/login/staff";
            case KITCHEN -> "/login/kitchen";
        };
    }

    public static EmployeeRole preferredLoginRole(String path) {
        if (path.endsWith("/ws/staff")) {
            return EmployeeRole.STAFF;
        }
        if (path.endsWith("/ws/kitchen")) {
            return EmployeeRole.KITCHEN;
        }
        if (path.endsWith("/ws/admin")) {
            return EmployeeRole.ADMIN;
        }
        if (path.startsWith("/admin/") || path.equals("/admin.jsp") || path.startsWith("/employee/")) {
            return EmployeeRole.ADMIN;
        }
        if (path.startsWith("/kitchen/") || path.equals("/kitchen.jsp") || path.startsWith("/api/kitchen/")) {
            return EmployeeRole.KITCHEN;
        }
        if (path.startsWith("/staff/") || path.equals("/staff.jsp") || path.startsWith("/api/staff/")) {
            return EmployeeRole.STAFF;
        }
        return null;
    }

    private static Employee getEmployee(Cookie[] cookies, String path) {
        Map<String, String> values = new HashMap<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                values.put(cookie.getName(), cookie.getValue());
            }
        }
        return getEmployee(values, path);
    }

    private static Employee getEmployee(Map<String, String> cookies, String path) {
        EmployeeRole preferred = preferredLoginRole(path == null ? "" : path);
        if (preferred != null) {
            Employee employee = findEmployee(preferred, cookies.get(cookieName(preferred)));
            if (employee != null) {
                return employee;
            }
            if (preferred != EmployeeRole.ADMIN) {
                return findEmployee(EmployeeRole.ADMIN, cookies.get(cookieName(EmployeeRole.ADMIN)));
            }
            return null;
        }

        for (EmployeeRole role : EmployeeRole.values()) {
            Employee employee = findEmployee(role, cookies.get(cookieName(role)));
            if (employee != null) {
                return employee;
            }
        }
        return null;
    }

    private static Employee findEmployee(EmployeeRole expectedRole, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        EmployeeSession employeeSession = EMPLOYEE_SESSIONS.get(token);
        if (employeeSession == null) {
            return null;
        }
        if (employeeSession.expiresAt().isBefore(Instant.now())) {
            EMPLOYEE_SESSIONS.remove(token);
            return null;
        }
        return employeeSession.employee().getRole() == expectedRole ? employeeSession.employee() : null;
    }

    private static String cookieName(EmployeeRole role) {
        return COOKIE_PREFIX + role.name();
    }

    private static String findCookie(Cookie[] cookies, String expectedName) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (expectedName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static Map<String, String> parseCookieHeaders(List<String> cookieHeaders) {
        Map<String, String> cookies = new HashMap<>();
        if (cookieHeaders == null) {
            return cookies;
        }
        for (String header : cookieHeaders) {
            if (header == null) {
                continue;
            }
            for (String item : header.split(";")) {
                int equalsIndex = item.indexOf('=');
                if (equalsIndex > 0) {
                    cookies.put(item.substring(0, equalsIndex).trim(), item.substring(equalsIndex + 1).trim());
                }
            }
        }
        return cookies;
    }

    private static void addCookie(HttpServletRequest request, HttpServletResponse response,
            EmployeeRole role, String value, int maxAge) {
        Cookie cookie = new Cookie(cookieName(role), value);
        String contextPath = request.getContextPath();
        cookie.setPath(contextPath == null || contextPath.isBlank() ? "/" : contextPath);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private record EmployeeSession(Employee employee, Instant expiresAt) {
    }
}
