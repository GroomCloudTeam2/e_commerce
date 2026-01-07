package com.groom.e_commerce.payment.presentation.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제-리다이렉트", description = "토스 결제창 성공/실패 리다이렉트 처리 API")
public class PaymentRedirectController {

	@Operation(
		summary = "결제 성공 리다이렉트(successUrl)",
		description = "토스 결제창 결제 성공 시 호출되는 리다이렉트 엔드포인트입니다. "
			+ "paymentKey/orderId/amount 쿼리 파라미터를 전달받습니다. "
			+ "현재는 테스트 단계로 파라미터를 그대로 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리다이렉트 파라미터 확인 성공"),
		@ApiResponse(responseCode = "400", description = "쿼리 파라미터 누락/형식 오류")
	})
	@GetMapping("/success")
	public ResponseEntity<Map<String, Object>> success(
		@Parameter(description = "토스 결제 키(paymentKey)", example = "tviva20260102abcd...")
		@RequestParam String paymentKey,

		@Parameter(description = "주문 ID(orderId) - UUID 형식", example = "550e8400-e29b-41d4-a716-446655440000")
		@RequestParam UUID orderId,

		@Parameter(description = "결제 금액(amount)", example = "15000")
		@RequestParam Long amount
	) {
		// ✅ 옵션 1) 테스트 단계: 파라미터 확인만
		return ResponseEntity.ok(
			Map.of(
				"paymentKey", paymentKey,
				"orderId", orderId,
				"amount", amount
			)
		);

		// ✅ 옵션 2) 여기서 바로 confirm 호출하고 싶으면:
		// confirmPaymentUseCase.confirm(new ReqConfirmPaymentV1(paymentKey, orderId, amount));
	}

	@Operation(
		summary = "결제 실패 리다이렉트(failUrl)",
		description = "토스 결제창 결제 실패 시 호출되는 리다이렉트 엔드포인트입니다. "
			+ "code/message/orderId 등을 쿼리 파라미터로 전달받습니다. "
			+ "현재는 테스트 단계로 파라미터를 그대로 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "실패 정보 반환(의도된 응답)")
	})
	@GetMapping("/fail")
	public ResponseEntity<Map<String, Object>> fail(
		@Parameter(description = "실패 코드", example = "PAY_PROCESS_CANCELED")
		@RequestParam(required = false) String code,

		@Parameter(description = "실패 메시지", example = "사용자가 결제를 취소했습니다.")
		@RequestParam(required = false) String message,

		@Parameter(description = "주문 ID(orderId) - UUID 형식", example = "550e8400-e29b-41d4-a716-446655440000")
		@RequestParam(required = false) UUID orderId
	) {
		return ResponseEntity.badRequest().body(
			Map.of(
				"code", code,
				"message", message,
				"orderId", orderId
			)
		);
	}
}
