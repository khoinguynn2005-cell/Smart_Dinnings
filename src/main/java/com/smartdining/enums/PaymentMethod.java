package com.smartdining.enums;

/** Phuong thuc thanh toan luu trong Payments.PaymentMethod. */
public enum PaymentMethod {
	CASH, TRANSFER;

	public static PaymentMethod parse(String value) {
		if (value == null) {
			return null;
		}
		try {
			return PaymentMethod.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
