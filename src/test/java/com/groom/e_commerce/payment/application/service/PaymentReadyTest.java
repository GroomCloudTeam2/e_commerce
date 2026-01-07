package com.groom.e_commerce.payment.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.presentation.dto.request.ReqReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

@DisplayName("1) READY(결제 준비) 테스트")
class PaymentReadyTest extends PaymentCommandServiceTestBase {

	@Test
	@DisplayName("READY 성공: 주문금액/결제금액/상태 검증 통과 시 결제창 파라미터 반환")
	void ready_success() {
		UUID orderId = UUID.randomUUID();
		long amount = 10_000L;

		OrderQueryPort.OrderSummary summary = mock(OrderQueryPort.OrderSummary.class);
		when(summary.totalPaymentAmt()).thenReturn(amount);
		when(summary.orderNumber()).thenReturn("ORDER-001");
		when(summary.recipientName()).thenReturn("홍길동");
		when(orderQueryPort.getOrderSummary(orderId)).thenReturn(summary);

		// ✅ Payment는 READY + amount 일치해야 함
		Payment payment = mock(Payment.class);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(payment.getStatus()).thenReturn(PaymentStatus.READY);
		when(payment.getAmount()).thenReturn(amount);

		when(tossPaymentsProperties.clientKey()).thenReturn("client_key");
		when(tossPaymentsProperties.successUrl()).thenReturn("http://localhost/success");
		when(tossPaymentsProperties.failUrl()).thenReturn("http://localhost/fail");

		ReqReadyPaymentV1 req = new ReqReadyPaymentV1(orderId, amount);
		ResReadyPaymentV1 res = service.ready(req);

		assertNotNull(res);
		assertEquals(orderId, res.orderId());
		assertEquals(amount, res.amount());
		assertEquals("client_key", res.clientKey());
		assertEquals("http://localhost/success", res.successUrl());
		assertEquals("http://localhost/fail", res.failUrl());
	}

	@Test
	@DisplayName("READY 실패: amount null이면 AMOUNT_REQUIRED")
	void ready_fail_amountNull() {
		UUID orderId = UUID.randomUUID();
		ReqReadyPaymentV1 req = new ReqReadyPaymentV1(orderId, null);

		PaymentException ex = assertThrows(PaymentException.class, () -> service.ready(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertEquals("AMOUNT_REQUIRED", ex.getCode());
	}

	@Test
	@DisplayName("READY 실패: 주문 총액과 요청 금액 불일치면 INVALID_AMOUNT")
	void ready_fail_amountMismatch() {
		UUID orderId = UUID.randomUUID();

		OrderQueryPort.OrderSummary summary = mock(OrderQueryPort.OrderSummary.class);
		when(summary.totalPaymentAmt()).thenReturn(20_000L);
		when(orderQueryPort.getOrderSummary(orderId)).thenReturn(summary);

		ReqReadyPaymentV1 req = new ReqReadyPaymentV1(orderId, 10_000L);

		PaymentException ex = assertThrows(PaymentException.class, () -> service.ready(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertEquals("INVALID_AMOUNT", ex.getCode());
	}

	@Test
	@DisplayName("READY 실패: payment이 READY가 아니면 PAYMENT_NOT_READY")
	void ready_fail_notReadyStatus() {
		UUID orderId = UUID.randomUUID();
		long amount = 10_000L;

		OrderQueryPort.OrderSummary summary = mock(OrderQueryPort.OrderSummary.class);
		when(summary.totalPaymentAmt()).thenReturn(amount);
		when(orderQueryPort.getOrderSummary(orderId)).thenReturn(summary);

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

		ReqReadyPaymentV1 req = new ReqReadyPaymentV1(orderId, amount);

		PaymentException ex = assertThrows(PaymentException.class, () -> service.ready(req));
		assertEquals(HttpStatus.CONFLICT, ex.getStatus());
		assertEquals("PAYMENT_NOT_READY", ex.getCode());
	}
}
