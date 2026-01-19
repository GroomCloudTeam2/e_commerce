package com.groom.e_commerce.payment.presentation.dto.response;

import java.util.UUID;

public record ResReadyPaymentV1(
	UUID orderId,
	Long amount,
	String orderName,
	String customerName,
	String clientKey,
	String successUrl,
	String failUrl
) {
}
