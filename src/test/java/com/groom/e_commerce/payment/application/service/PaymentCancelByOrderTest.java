package com.groom.e_commerce.payment.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

@DisplayName("4) CANCEL BY ORDER(주문기반 취소) 테스트")
class PaymentCancelByOrderTest extends PaymentCommandServiceTestBase {

	@Test
	@DisplayName("성공: cancelByOrder 부분취소 -> 토스 취소 1회 + PaymentCancel 1건 추가")
	void cancelByOrder_success_partial() {
		UUID orderId = UUID.randomUUID();
		List<UUID> orderItemIds = List.of(UUID.randomUUID());
		long total = 30_000L;
		long canceled = 5_000L;
		long cancelAmount = 7_000L;

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(false);
		when(payment.getAmount()).thenReturn(total);
		when(payment.getCanceledAmount()).thenReturn(canceled);
		when(payment.getPaymentKey()).thenReturn("payKey");
		when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

		TossCancelResponse tossRes = mock(TossCancelResponse.class);
		when(tossRes.paymentKey()).thenReturn("payKey");
		when(tossRes.canceledAt()).thenReturn(OffsetDateTime.now());

		when(tossPaymentPort.cancel(eq("payKey"), any(TossCancelRequest.class))).thenReturn(tossRes);
		when(paymentRepository.save(payment)).thenReturn(payment);

		ResCancelResultV1 res = service.cancelByOrder(orderId, cancelAmount, orderItemIds);

		assertNotNull(res);
		verify(tossPaymentPort, times(1)).cancel(eq("payKey"), any(TossCancelRequest.class));

		ArgumentCaptor<PaymentCancel> captor = ArgumentCaptor.forClass(PaymentCancel.class);
		verify(payment, times(1)).addCancel(captor.capture());
		assertEquals(cancelAmount, captor.getValue().getCancelAmount());

		verify(paymentRepository, times(1)).save(payment);
	}

	@Test
	@DisplayName("멱등: 이미 CANCELLED면 토스 호출 없이 성공 응답")
	void cancelByOrder_idempotent_alreadyCancelled() {
		UUID orderId = UUID.randomUUID();
		List<UUID> orderItemIds = List.of(UUID.randomUUID());

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(true);
		when(payment.getPaymentKey()).thenReturn("payKey");
		when(payment.getStatus()).thenReturn(PaymentStatus.CANCELLED);
		when(payment.getCanceledAmount()).thenReturn(10_000L);

		ResCancelResultV1 res = service.cancelByOrder(orderId, 1_000L, orderItemIds);

		assertNotNull(res);
		verify(tossPaymentPort, never()).cancel(anyString(), any());
		verify(paymentRepository, never()).save(any());
		verify(payment, never()).addCancel(any());
	}

	@Test
	@DisplayName("실패: 취소금액이 remaining 초과면 EXCEED_CANCEL_AMOUNT")
	void cancelByOrder_fail_exceedRemaining() {
		UUID orderId = UUID.randomUUID();
		List<UUID> orderItemIds = List.of(UUID.randomUUID());

		Payment payment = mock(Payment.class);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(payment.isAlreadyCancelled()).thenReturn(false);

		// ✅ remaining 계산에 필요한 stubbing만 남김
		when(payment.getAmount()).thenReturn(10_000L);
		when(payment.getCanceledAmount()).thenReturn(9_000L); // remaining 1,000

		PaymentException ex = assertThrows(
			PaymentException.class,
			() -> service.cancelByOrder(orderId, 2_000L, orderItemIds)
		);

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertEquals("EXCEED_CANCEL_AMOUNT", ex.getCode());

		verify(tossPaymentPort, never()).cancel(anyString(), any());
		verify(paymentRepository, never()).save(any());
		verify(payment, never()).addCancel(any());
	}

}
