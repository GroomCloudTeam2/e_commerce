package com.groom.e_commerce.payment.application.service;

import com.groom.e_commerce.payment.application.port.out.OrderStatePort;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import com.groom.e_commerce.payment.infrastructure.api.toss.config.TossPaymentsProperties;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPaymentV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

	@InjectMocks
	private PaymentCommandService paymentCommandService;

	@Mock private PaymentRepository paymentRepository;
	@Mock private TossPaymentPort tossPaymentPort;
	@Mock private OrderStatePort orderStatePort;
	@Mock private TossPaymentsProperties tossPaymentsProperties;
	@Mock private OrderQueryPort orderQueryPort;

	// -------------------------------------------------------------------
	// 테스트 1: 해킹 시도 방어 (금액 위변조)
	// -------------------------------------------------------------------
	@Test
	@DisplayName("🚨 금액 위변조 감지: 주문은 100만원인데 결제 요청이 100원이면 예외가 터져야 한다.")
	void shouldThrowException_when_AmountMisMatch() {
		// given
		UUID orderId = UUID.randomUUID();
		long realAmount = 1_000_000L; // DB (진짜 가격)
		long hackedAmount = 100L;     // 해커 요청 (가짜 가격)

		// DB 데이터 Mocking
		Payment paymentInDb = Payment.builder()
			.orderId(orderId)
			.amount(realAmount)
			.status(PaymentStatus.READY)
			.build();

		given(paymentRepository.findByOrderId(orderId))
			.willReturn(Optional.of(paymentInDb));

		// 해커의 요청 객체
		// ✅ [수정] 생성자 순서 맞춤: (String paymentKey, UUID orderId, Long amount)
		ReqConfirmPaymentV1 hackRequest = new ReqConfirmPaymentV1(
			"fake-payment-key",
			orderId,
			hackedAmount
		);

		// when & then
		assertThatThrownBy(() -> paymentCommandService.confirm(hackRequest))
			.isInstanceOf(PaymentException.class)
			.hasMessageContaining("일치하지 않습니다"); // 메시지는 실제 Exception 메시지에 맞게 조정

		// 검증: 토스 호출 금지, 주문 상태 변경 금지
		verify(tossPaymentPort, never()).confirm(any());
		verify(orderStatePort, never()).payOrder(any());
	}

	// -------------------------------------------------------------------
	// 테스트 2: 정상 결제 승인 및 Order 연동 확인
	// -------------------------------------------------------------------
	@Test
	@DisplayName("결제 승인(confirm) 성공 시, OrderStatePort를 호출하여 주문 상태를 PAID로 변경해야 한다.")
	void shouldCallPayOrder_whenConfirmSuccess() {
		// given
		UUID orderId = UUID.randomUUID();
		long amount = 50000L;
		String paymentKey = "test_payment_key";

		Payment payment = Payment.builder()
			.orderId(orderId)
			.amount(amount)
			.status(PaymentStatus.READY)
			.build();

		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// ✅ [수정] TossPaymentResponse 생성자 순서 맞춤 (10개)
		TossPaymentResponse tossResponse = new TossPaymentResponse(
			paymentKey,         // paymentKey
			orderId.toString(), // orderId
			"orderName",        // orderName
			"customerName",     // customerName
			"CARD",             // method
			"KRW",              // currency
			amount,             // totalAmount
			"DONE",             // status
			OffsetDateTime.now(),// requestedAt
			OffsetDateTime.now() // approvedAt
		);

		given(tossPaymentPort.confirm(any(TossConfirmRequest.class))).willReturn(tossResponse);

		// save 호출 시 자기 자신 반환
		given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

		// 요청 객체
		// ✅ [수정] 생성자 순서 맞춤: (String, UUID, Long)
		ReqConfirmPaymentV1 request = new ReqConfirmPaymentV1(paymentKey, orderId, amount);

		// when
		paymentCommandService.confirm(request);

		// then
		// ★ 핵심 검증: OrderStatePort 호출 여부
		verify(orderStatePort).payOrder(orderId);
	}
}