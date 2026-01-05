// src/main/java/com/groom/e_commerce/payment/presentation/controller/PaymentCancelItemControllerV1.java
package com.groom.e_commerce.payment.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.groom.e_commerce.payment.application.port.in.CancelOrderItemPaymentUseCase;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCancelItemControllerV1 {

	private final CancelOrderItemPaymentUseCase cancelOrderItemPaymentUseCase;

	public PaymentCancelItemControllerV1(CancelOrderItemPaymentUseCase cancelOrderItemPaymentUseCase) {
		this.cancelOrderItemPaymentUseCase = cancelOrderItemPaymentUseCase;
	}

	@PostMapping("/orders/{orderId}/items/{orderItemId}/cancel")
	public ResponseEntity<ResCancelResultV1> cancelOrderItem(
		@PathVariable UUID orderId,
		@PathVariable UUID orderItemId,
		@RequestParam long cancelAmount
	) {
		return ResponseEntity.ok(cancelOrderItemPaymentUseCase.cancelOrderItem(orderId, orderItemId, cancelAmount));
	}
}
