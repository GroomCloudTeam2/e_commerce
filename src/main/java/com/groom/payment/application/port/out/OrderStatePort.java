package com.groom.e_commerce.payment.application.port.out;

import java.util.List;
import java.util.UUID;

public interface OrderStatePort {
	void payOrder(UUID orderId);

	void cancelOrderByPayment(
		UUID orderId,
		Long canceledAmountThisTime,
		Long canceledAmountTotal,
		String paymentStatus,
		List<UUID> orderItemIds
	);
}

