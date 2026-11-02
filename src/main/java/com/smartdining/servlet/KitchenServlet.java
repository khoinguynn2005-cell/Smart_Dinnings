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
@WebServlet("/kitchen/dashboard")
public class KitchenServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SecurityUtil.hasRole(request, EmployeeRole.ADMIN, EmployeeRole.KITCHEN)) {
            response.sendRedirect(request.getContextPath() + "/login/kitchen");
            return;
        }

        // Danh sach ban duoc kitchen.jsp tai qua /api/kitchen/tables (cap nhat real-time khi admin doi ban).
        request.getRequestDispatcher("/kitchen.jsp").forward(request, response);
    }
}
