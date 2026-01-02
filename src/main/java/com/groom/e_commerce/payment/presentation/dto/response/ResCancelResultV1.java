package com.groom.e_commerce.payment.presentation.dto.response;

public record ResCancelResultV1(
	String paymentKey,
	String status,
	Long canceledAmount
) {
	public static ResCancelResultV1 of(String paymentKey, String status, Long canceledAmount) {
		return new ResCancelResultV1(paymentKey, status, canceledAmount);
	}
}
