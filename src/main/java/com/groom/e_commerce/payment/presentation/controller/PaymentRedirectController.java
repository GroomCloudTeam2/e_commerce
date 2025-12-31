package com.groom.e_commerce.payment.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentRedirectController {

	@GetMapping("/success")
	public ResponseEntity<?> success(
		@RequestParam String paymentKey,
		@RequestParam UUID orderId,
		@RequestParam Long amount
	) {
		// 옵션 1) 테스트 단계: 파라미터 확인만
		return ResponseEntity.ok(
			java.util.Map.of(
				"paymentKey", paymentKey,
				"orderId", orderId,
				"amount", amount
			)
		);

		// 옵션 2) 여기서 바로 confirm 호출하고 싶으면:
		// paymentCommandService.confirm(new ReqConfirmPaymentV1(paymentKey, orderId, amount));
	}

	@GetMapping("/fail")
	public ResponseEntity<?> fail(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String message,
		@RequestParam(required = false) UUID orderId
	) {
		return ResponseEntity.badRequest().body(
			java.util.Map.of(
				"code", code,
				"message", message,
				"orderId", orderId
			)
		);
	}
}
