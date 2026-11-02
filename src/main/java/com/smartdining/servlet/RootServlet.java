package com.smartdining.servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Trang goc cua ung dung: khach hang vao menu bang ma QR tai ban,
 * nen trang goc chuyen ve trang dang nhap (co huong dan cho ca khach va nhan vien).
 */
@SuppressWarnings("serial")
@WebServlet("")
public class RootServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
