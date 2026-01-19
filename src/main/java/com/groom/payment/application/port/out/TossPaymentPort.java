package com.groom.e_commerce.payment.application.port.out;

import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;

public interface TossPaymentPort {
	TossPaymentResponse confirm(TossConfirmRequest request);

	TossPaymentResponse getPayment(String paymentKey);

	TossCancelResponse cancel(String paymentKey, TossCancelRequest request);
}
