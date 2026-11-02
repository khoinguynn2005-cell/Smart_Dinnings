package com.smartdining.servlet;

import java.io.IOException;

import com.smartdining.enums.EmployeeRole;
import com.smartdining.utils.SecurityUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/admin/dashboard")
public class AdminServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!SecurityUtil.hasRole(request, EmployeeRole.ADMIN)) {
            response.sendRedirect(request.getContextPath() + "/login/admin");
            return;
        }
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }
}
