package com.groom.e_commerce.payment.presentation.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;

public record ResPaymentV1(
	String paymentKey,
	UUID orderId,
	String status,
	Long amount,
	Long canceledAmount,
	String pgProvider,
	OffsetDateTime approvedAt
) {
	public static ResPaymentV1 from(Payment payment) {
		return new ResPaymentV1(
			payment.getPaymentKey(),
			payment.getOrderId(),
			payment.getStatus().name(),
			payment.getAmount(),
			payment.getCanceledAmount(),
			payment.getPgProvider(),
			payment.getApprovedAt()
		);
	}

	/**
	 * 내부에 Payment 레코드가 없거나(또는 조회 시),
	 * 토스 응답을 그대로 내려줄 때 사용.
	 *
	 * ⚠️ toss.orderId()는 String인 경우가 많아서 UUID 변환이 필요함.
	 * 토스 orderId를 우리 UUID로 사용한다는 전제(=order_id)라면 아래처럼 변환.
	 */
	public static ResPaymentV1 fromToss(TossPaymentResponse toss) {
		UUID orderUuid = null;
		try {
			if (toss.orderId() != null) {
				orderUuid = UUID.fromString(toss.orderId());
			}
		} catch (IllegalArgumentException ignored) {
			// 토스 orderId가 UUID 형식이 아니면 null 유지 (혹은 예외 던져도 됨)
		}

		return new ResPaymentV1(
			toss.paymentKey(),
			orderUuid,
			toss.status(),            // 토스 status 문자열
			toss.totalAmount(),       // 토스 결제금액
			0L,                       // 토스 취소이력 상세를 안쓰면 0
			"TOSS",
			toss.approvedAt()
		);
	}
}
