package com.groom.e_commerce.payment.application.port.out;

import java.util.UUID;

public interface OrderQueryPort {
	OrderSummary getOrderSummary(UUID orderId);

	record OrderSummary(
		UUID orderId,
		long totalPaymentAmt,
		String orderNumber,
		String recipientName
	) {
	}
}
