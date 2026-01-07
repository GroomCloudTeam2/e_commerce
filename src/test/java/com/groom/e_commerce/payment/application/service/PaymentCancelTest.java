package com.groom.e_commerce.payment.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

@DisplayName("3) CANCEL(결제 취소) 테스트")
class PaymentCancelTest extends PaymentCommandServiceTestBase {

	@Test
	@DisplayName("CANCEL 성공: cancelAmount null이면 remaining 전액 취소")
	void cancel_success_fullByNullAmount() {
		String paymentKey = "payKey";
		long total = 20_000L;
		long canceled = 5_000L;
		long remaining = total - canceled; // 15_000

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByPaymentKey(paymentKey)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(false);
		when(payment.getAmount()).thenReturn(total);
		when(payment.getCanceledAmount()).thenReturn(canceled);
		when(payment.getPaymentKey()).thenReturn(paymentKey);
		when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

		TossCancelResponse tossRes = mock(TossCancelResponse.class);
		when(tossRes.paymentKey()).thenReturn(paymentKey);
		when(tossRes.canceledAt()).thenReturn(OffsetDateTime.now());
		when(tossPaymentPort.cancel(eq(paymentKey), any(TossCancelRequest.class))).thenReturn(tossRes);

		when(paymentRepository.save(payment)).thenReturn(payment);

		ReqCancelPaymentV1 req = new ReqCancelPaymentV1("테스트취소", null);

		ResCancelResultV1 res = service.cancel(paymentKey, req);

		assertNotNull(res);

		verify(tossPaymentPort, times(1)).cancel(eq(paymentKey), any(TossCancelRequest.class));

		ArgumentCaptor<PaymentCancel> captor = ArgumentCaptor.forClass(PaymentCancel.class);
		verify(payment, times(1)).addCancel(captor.capture());
		assertEquals(remaining, captor.getValue().getCancelAmount());

		verify(paymentRepository, times(1)).save(payment);
	}

	@Test
	@DisplayName("CANCEL 멱등: 이미 CANCELLED면 토스 호출 없이 성공 응답")
	void cancel_idempotent_alreadyCancelled() {
		String paymentKey = "payKey";

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByPaymentKey(paymentKey)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(true);
		when(payment.getPaymentKey()).thenReturn(paymentKey);
		when(payment.getStatus()).thenReturn(PaymentStatus.CANCELLED);
		when(payment.getCanceledAmount()).thenReturn(10_000L);

		ReqCancelPaymentV1 req = new ReqCancelPaymentV1("테스트취소", 1_000L);

		ResCancelResultV1 res = service.cancel(paymentKey, req);

		assertNotNull(res);
		verify(tossPaymentPort, never()).cancel(anyString(), any());
		verify(paymentRepository, never()).save(any());
		verify(payment, never()).addCancel(any());
	}

	@Test
	@DisplayName("CANCEL 실패: cancelAmount가 remaining 초과면 EXCEED_CANCEL_AMOUNT")
	void cancel_fail_exceedRemaining() {
		String paymentKey = "payKey";

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByPaymentKey(paymentKey)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(false);
		when(payment.getAmount()).thenReturn(10_000L);
		when(payment.getCanceledAmount()).thenReturn(9_000L);

		ReqCancelPaymentV1 req = new ReqCancelPaymentV1("초과취소", 2_000L);

		PaymentException ex = assertThrows(PaymentException.class, () -> service.cancel(paymentKey, req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertEquals("EXCEED_CANCEL_AMOUNT", ex.getCode());

		verify(tossPaymentPort, never()).cancel(anyString(), any());
		verify(paymentRepository, never()).save(any());
		verify(payment, never()).addCancel(any());
	}

	@Test
	@DisplayName("CANCEL: 토스가 이미 취소됨(ALREADY_CANCELED_PAYMENT)일 때 DB 보정 후 성공")
	void cancel_tossAlreadyCanceled_shouldFixDbAndReturnOk() {
		String paymentKey = "payKey";
		long total = 10_000L;
		long canceled = 7_000L;
		long remaining = total - canceled; // 3_000

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByPaymentKey(paymentKey)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(false);
		when(payment.getAmount()).thenReturn(total);
		when(payment.getCanceledAmount()).thenReturn(canceled);
		when(payment.getPaymentKey()).thenReturn(paymentKey);
		when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

		TossApiException tossEx = mock(TossApiException.class);
		when(tossEx.getTossErrorCode()).thenReturn("ALREADY_CANCELED_PAYMENT");
		when(tossPaymentPort.cancel(eq(paymentKey), any(TossCancelRequest.class))).thenThrow(tossEx);

		when(paymentRepository.save(payment)).thenReturn(payment);

		ReqCancelPaymentV1 req = new ReqCancelPaymentV1("취소", null);

		ResCancelResultV1 res = service.cancel(paymentKey, req);

		assertNotNull(res);

		ArgumentCaptor<PaymentCancel> captor = ArgumentCaptor.forClass(PaymentCancel.class);
		verify(payment, times(1)).addCancel(captor.capture());
		assertEquals(remaining, captor.getValue().getCancelAmount());

		verify(paymentRepository, times(1)).save(payment);
	}
}
