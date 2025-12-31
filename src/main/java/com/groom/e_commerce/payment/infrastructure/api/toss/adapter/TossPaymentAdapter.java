package com.groom.e_commerce.payment.infrastructure.api.toss.adapter;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.infrastructure.api.toss.client.TossPaymentsClient;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;

@Component
public class TossPaymentAdapter implements TossPaymentPort {

	private final TossPaymentsClient tossPaymentsClient;

	public TossPaymentAdapter(TossPaymentsClient tossPaymentsClient) {
		this.tossPaymentsClient = tossPaymentsClient;
	}

	@Override
	public TossPaymentResponse confirm(TossConfirmRequest request) {
		return tossPaymentsClient.confirm(request);
	}

	@Override
	public TossCancelResponse cancel(String paymentKey, TossCancelRequest request) {
		return tossPaymentsClient.cancel(paymentKey, request);
	}

	@Override
	public TossPaymentResponse getPayment(String paymentKey) {
		return tossPaymentsClient.getPayment(paymentKey);
	}
}
