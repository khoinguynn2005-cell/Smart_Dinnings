package com.smartdining.utils;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaUtil {
	static EntityManagerFactory factory;
	
	static {
		try {

            Map<String, String> properties = new HashMap<>();
            
            properties.put("jakarta.persistence.jdbc.url", ConfigUtil.get("db.url"));
            properties.put("jakarta.persistence.jdbc.user", ConfigUtil.get("db.username"));
            properties.put("jakarta.persistence.jdbc.password", ConfigUtil.get("db.password"));

            factory = Persistence.createEntityManagerFactory("SmartDining", properties);
            
        } catch (Throwable ex) {
            System.err.println("Lỗi khởi tạo EntityManagerFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
	}
	
	public static EntityManager getEntityManager() {
		return factory.createEntityManager();

	}
	
	public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }
}
