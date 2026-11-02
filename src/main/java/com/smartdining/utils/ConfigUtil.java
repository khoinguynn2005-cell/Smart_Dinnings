package com.smartdining.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigUtil {
	
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/config.properties")) {
            
            if (input == null) {
                System.out.println("Không tìm thấy file config.properties!");
            } else {
                loadProperties(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        String value = properties.getProperty(key);
        return value == null ? null : value.trim();
    }

    private static void loadProperties(InputStream input) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                properties.setProperty(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
    }
}
