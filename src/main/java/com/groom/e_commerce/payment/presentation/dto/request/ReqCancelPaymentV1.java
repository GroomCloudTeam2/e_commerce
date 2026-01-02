package com.groom.e_commerce.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReqCancelPaymentV1(
	@NotBlank String cancelReason,
	Long cancelAmount // null이면 "잔여 전액 취소" 처리
) {
}
