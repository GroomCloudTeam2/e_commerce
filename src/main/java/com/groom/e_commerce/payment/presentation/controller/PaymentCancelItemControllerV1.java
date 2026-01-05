// src/main/java/com/groom/e_commerce/payment/presentation/controller/PaymentCancelItemControllerV1.java
package com.groom.e_commerce.payment.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.groom.e_commerce.payment.application.port.in.CancelOrderItemPaymentUseCase;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelOrderItemPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentCancelItemControllerV1 {

	private final CancelOrderItemPaymentUseCase cancelOrderItemPaymentUseCase;

	public PaymentCancelItemControllerV1(CancelOrderItemPaymentUseCase cancelOrderItemPaymentUseCase) {
		this.cancelOrderItemPaymentUseCase = cancelOrderItemPaymentUseCase;
	}

	/**
	 * Order -> Payment: {orderId, orderItemId, cancelAmount} 전달
	 * 예) POST /api/v1/payment/orders/{orderId}/items/{orderItemId}/cancel
	 */
	@PostMapping("/orders/{orderId}/items/{orderItemId}/cancel")
	public ResponseEntity<ResCancelResultV1> cancelOrderItem(
		@PathVariable UUID orderId,
		@PathVariable UUID orderItemId,
		@RequestBody ReqCancelOrderItemPaymentV1 request
	) {
		ResCancelResultV1 result =
			cancelOrderItemPaymentUseCase.cancelOrderItem(orderId, orderItemId, request.cancelAmount());

		return ResponseEntity.ok(result);
	}
}
