package com.smartdining.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/menu")
public class MenuServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String qrToken = request.getParameter("qr");
        if (qrToken == null || qrToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Ma QR khong hop le");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/home/index?qr="
                + URLEncoder.encode(qrToken, StandardCharsets.UTF_8));
    }
}
