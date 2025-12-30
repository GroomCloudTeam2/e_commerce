package com.groom.e_commerce.payment.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.application.port.in.GetPaymentUseCase;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;
import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

@Service
@Transactional(readOnly = true)
public class PaymentQueryService implements GetPaymentUseCase {

	private final PaymentRepository paymentRepository;
	private final TossPaymentPort tossPaymentPort;

	public PaymentQueryService(PaymentRepository paymentRepository, TossPaymentPort tossPaymentPort) {
		this.paymentRepository = paymentRepository;
		this.tossPaymentPort = tossPaymentPort;
	}

	@Override
	public ResPaymentV1 getByPaymentKey(String paymentKey) {
		// 내부에 있으면 내부 우선
		Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
		if (payment != null) {
			return ResPaymentV1.from(payment);
		}

		// 없으면 토스 조회 후 반환(필요 시 저장 로직 추가 가능)
		TossPaymentResponse toss = tossPaymentPort.getPayment(paymentKey);
		return ResPaymentV1.fromToss(toss);
	}

	@Override
	public ResPaymentV1 getByOrderId(String orderId) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."));
		return ResPaymentV1.from(payment);
	}
}
