package com.smartdining.service.impl;

import com.smartdining.dao.EmployeeDAO;
import com.smartdining.dao.impl.EmployeeDAOImpl;
import com.smartdining.entity.Employee;
import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.EmployeeStatus;
import com.smartdining.service.EmployeeService;
import com.smartdining.utils.PasswordUtil;
import com.smartdining.utils.VerificationCodeUtil;

public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeDAO dao = new EmployeeDAOImpl();

    @Override
    public boolean create(Employee employee) {
        if (employee == null || (employee.getRole() != EmployeeRole.STAFF
                && employee.getRole() != EmployeeRole.KITCHEN)) {
            return false;
        }

        if (employee.getUsername() == null || employee.getUsername().isBlank()
                || employee.getPassword() == null || employee.getPassword().length() < 8
                || employee.getEmail() == null || !employee.getEmail().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || dao.findByUsername(employee.getUsername()) != null) {
            return false;
        }

        try {
            employee.setId(dao.generateEmployeeIdByRole(employee.getRole().name()));
            employee.setPassword(PasswordUtil.forStorage(employee.getPassword()));
            employee.setEmail(employee.getEmail().trim());
            employee.setStatus(EmployeeStatus.PENDING);
            if (!dao.create(employee)) {
                return false;
            }
            VerificationCodeUtil.issueAndSend(employee);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Employee getById(String id) {
        return dao.findById(id);
    }
}
