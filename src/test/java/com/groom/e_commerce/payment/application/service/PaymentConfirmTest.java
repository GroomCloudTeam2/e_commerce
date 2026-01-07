package com.groom.e_commerce.payment.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

@DisplayName("2) CONFIRM(결제 승인) 테스트")
class PaymentConfirmTest extends PaymentCommandServiceTestBase {

	@Test
	@DisplayName("CONFIRM 성공: 토스 승인 후 PAID 처리 + 주문상태 payOrder 호출")
	void confirm_success() {
		UUID orderId = UUID.randomUUID();
		Long amount = 15_000L;

		// ✅ 실제 엔티티 사용 (markPaid 반영 검증)
		Payment payment = new Payment(orderId, amount, "toss");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		OffsetDateTime approvedAt = OffsetDateTime.now();

		TossPaymentResponse tossRes = mock(TossPaymentResponse.class);
		when(tossRes.paymentKey()).thenReturn("payKey");
		when(tossRes.approvedAt()).thenReturn(approvedAt);
		when(tossRes.totalAmount()).thenReturn(amount);

		when(tossPaymentPort.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

		// ⚠️ 네 record 생성자 순서가 (paymentKey, orderId, amount)인 걸 네 테스트가 이미 사용중
		ReqConfirmPaymentV1 req = new ReqConfirmPaymentV1("payKey", orderId, amount);

		ResPaymentV1 res = service.confirm(req);

		assertNotNull(res);
		assertEquals(PaymentStatus.PAID, payment.getStatus());
		assertEquals("payKey", payment.getPaymentKey());
		assertEquals(0L, payment.getCanceledAmount());

		verify(tossPaymentPort, times(1)).confirm(any(TossConfirmRequest.class));
		verify(orderStatePort, times(1)).payOrder(orderId);
	}

	@Test
	@DisplayName("CONFIRM 멱등: 이미 PAID면 토스 호출 없이 그대로 응답")
	void confirm_idempotent_alreadyPaid() {
		UUID orderId = UUID.randomUUID();
		Long amount = 15_000L;

		Payment payment = new Payment(orderId, amount, "toss");
		payment.markPaid("payKey", OffsetDateTime.now());

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		ReqConfirmPaymentV1 req = new ReqConfirmPaymentV1("payKey", orderId, amount);
		ResPaymentV1 res = service.confirm(req);

		assertNotNull(res);
		verify(tossPaymentPort, never()).confirm(any());
		verify(orderStatePort, never()).payOrder(any());
	}

	@Test
	@DisplayName("CONFIRM 실패: 서버 금액과 요청 금액 불일치면 INVALID_AMOUNT")
	void confirm_fail_amountMismatch() {
		UUID orderId = UUID.randomUUID();

		Payment payment = new Payment(orderId, 9_999L, "toss");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		ReqConfirmPaymentV1 req = new ReqConfirmPaymentV1("payKey", orderId, 10_000L);

		PaymentException ex = assertThrows(PaymentException.class, () -> service.confirm(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertEquals("INVALID_AMOUNT", ex.getCode());

		verify(tossPaymentPort, never()).confirm(any());
	}
}
