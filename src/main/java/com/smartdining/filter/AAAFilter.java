package com.smartdining.filter;

import java.io.IOException;

import com.smartdining.enums.EmployeeRole;
import com.smartdining.utils.SecurityUtil;
import com.smartdining.utils.XHttp;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class AAAFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
            if (path.equals("/index.jsp")) {
                // Trang menu chi hien thi qua /home/index (can so ban hoac ma QR).
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                return;
            }
            EmployeeRole[] requiredRoles = requiredRoles(path);
            if (requiredRoles != null) {
                var employee = SecurityUtil.getEmployee(httpRequest);
                if (!SecurityUtil.hasRole(employee, requiredRoles)) {
                    if (path.startsWith("/api/") || path.startsWith("/employee/")) {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                    }
                    return;
                }
                httpRequest.setAttribute("employee", employee);
            }
        }

        XHttp.add(request, response);
        try {
            chain.doFilter(request, response);
        } finally {
            XHttp.remove();
        }
    }

    private EmployeeRole[] requiredRoles(String path) {
        if (path.startsWith("/employee/") || path.startsWith("/admin/") || path.equals("/admin.jsp")
                || path.startsWith("/api/admin/")) {
            return new EmployeeRole[] { EmployeeRole.ADMIN };
        }
        if (path.startsWith("/kitchen/") || path.equals("/kitchen.jsp") || path.startsWith("/api/kitchen/")) {
            return new EmployeeRole[] { EmployeeRole.ADMIN, EmployeeRole.KITCHEN };
        }
        if (path.startsWith("/staff/") || path.equals("/staff.jsp") || path.startsWith("/api/staff/")) {
            return new EmployeeRole[] { EmployeeRole.ADMIN, EmployeeRole.STAFF };
        }
        return null;
    }
}
