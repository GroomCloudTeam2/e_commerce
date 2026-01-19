package com.groom.e_commerce.payment.application.port.in;

import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;

public interface GetPaymentUseCase {
	ResPaymentV1 getByPaymentKey(String paymentKey);

	ResPaymentV1 getByOrderId(String orderId);
}
