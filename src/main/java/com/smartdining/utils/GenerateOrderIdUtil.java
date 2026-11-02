package com.smartdining.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GenerateOrderIdUtil {
	public static String get() {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

		String timestamp = LocalDateTime.now().format(formatter);

		String orderId = "HD-" + timestamp;
		
		return orderId;
	}
}
