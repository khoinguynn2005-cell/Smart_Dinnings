package com.smartdining.dao.impl;

import java.util.List;

import com.smartdining.dao.EmployeeDAO;
import com.smartdining.entity.Employee;
import com.smartdining.utils.GenericDAOImpl;
import com.smartdining.utils.JpaUtil;

import jakarta.persistence.EntityManager;

public class EmployeeDAOImpl extends GenericDAOImpl<Employee> implements EmployeeDAO {

	public EmployeeDAOImpl() {
		super(Employee.class);
	}

	@Override
	public String generateEmployeeIdByRole(String role) {
		String prefix;
        
        if (role == null) {
            prefix = "NV";
        } else {
            switch (role.toUpperCase()) {
                case "ADMIN":
                    prefix = "QL";
                    break;
                default:
                    prefix = "NV";
            }
        }

        EntityManager em = JpaUtil.getEntityManager();
        String lastId = null;

        try {
            
            String jpql = "SELECT e.id FROM Employee e WHERE e.id LIKE :prefix ORDER BY e.id DESC";
            List<String> results = em.createQuery(jpql, String.class)
                                     .setParameter("prefix", prefix + "%")
                                     .setMaxResults(1)
                                     .getResultList();

            if (!results.isEmpty()) {
                lastId = results.get(0);
            }
        } finally {
            em.close();
        }
        
        if (lastId == null) {
            return prefix + "000001";
        } else {

            String numberPart = lastId.substring(prefix.length());
            
            int nextNumber = Integer.parseInt(numberPart) + 1;
            
            return prefix + String.format("%06d", nextNumber);
        }
    }
    @Override
    public Employee findByUsername(String username) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            String jpql = "SELECT e FROM Employee e WHERE e.username = :user";
            List<Employee> results = em.createQuery(jpql, Employee.class)
                                       .setParameter("user", username)
                                       .getResultList();
            if (!results.isEmpty()) {
                return results.get(0);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }
}
