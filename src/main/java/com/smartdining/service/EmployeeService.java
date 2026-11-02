package com.smartdining.service;

import com.smartdining.entity.Employee;

public interface EmployeeService {
	public boolean create(Employee employee);
	public Employee getById(String id);
}
