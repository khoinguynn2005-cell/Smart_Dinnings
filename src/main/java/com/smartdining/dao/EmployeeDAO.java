package com.smartdining.dao;

import com.smartdining.entity.Employee;
import com.smartdining.utils.GenericDAO;

public interface EmployeeDAO extends GenericDAO<Employee> {
	public String generateEmployeeIdByRole(String role);
	public Employee findByUsername(String username);
}
