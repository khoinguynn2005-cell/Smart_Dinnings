package com.smartdining.servlet;

import java.io.IOException;

import com.smartdining.enums.EmployeeRole;
import com.smartdining.utils.SecurityUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EmployeeRole role = parseRole(request.getParameter("role"));
        if (role == null) {
            var employee = SecurityUtil.getEmployee(request);
            role = employee == null ? null : employee.getRole();
        }
        SecurityUtil.signOut(request, response, role);
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private EmployeeRole parseRole(String value) {
        try {
            return value == null ? null : EmployeeRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
