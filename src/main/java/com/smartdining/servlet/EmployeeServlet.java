package com.smartdining.servlet;

import java.io.IOException;

import com.smartdining.entity.Employee;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.service.EmployeeService;
import com.smartdining.service.impl.EmployeeServiceImpl;
import com.smartdining.utils.SecurityUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/employee/create")
public class EmployeeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final transient EmployeeService employeeService = new EmployeeServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!SecurityUtil.hasRole(request, EmployeeRole.ADMIN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EmployeeRole role;
        try {
            role = EmployeeRole.valueOf(request.getParameter("role"));
        } catch (IllegalArgumentException | NullPointerException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vai tro khong hop le");
            return;
        }

        if (role != EmployeeRole.STAFF && role != EmployeeRole.KITCHEN) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Chi duoc tao tai khoan nhan vien hoac bep");
            return;
        }

        Employee employee = new Employee();
        employee.setUsername(request.getParameter("username"));
        employee.setPassword(request.getParameter("password"));
        employee.setFullName(request.getParameter("fullName"));
        employee.setPhone(request.getParameter("phone"));
        employee.setEmail(request.getParameter("email"));
        employee.setRole(role);

        if (!employeeService.create(employee)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Khong the tao tai khoan");
            return;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
    }
}
