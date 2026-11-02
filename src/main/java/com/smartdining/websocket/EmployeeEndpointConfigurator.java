package com.smartdining.websocket;

import com.smartdining.entity.Employee;
import com.smartdining.utils.SecurityUtil;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

public class EmployeeEndpointConfigurator extends ServerEndpointConfig.Configurator {
    public static final String EMPLOYEE_PROPERTY = "employee";

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
            HandshakeResponse response) {
        super.modifyHandshake(config, request, response);
        Employee employee = SecurityUtil.getEmployeeForWebSocket(
                request.getRequestURI().getPath(), request.getHeaders().get("Cookie"));
        if (employee != null) {
            config.getUserProperties().put(EMPLOYEE_PROPERTY, employee);
        }
    }

    public static Employee getEmployee(jakarta.websocket.Session session) {
        Object employee = session.getUserProperties().get(EMPLOYEE_PROPERTY);
        return employee instanceof Employee ? (Employee) employee : null;
    }
}
