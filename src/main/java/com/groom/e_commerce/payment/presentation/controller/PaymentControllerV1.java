package com.groom.e_commerce.payment.presentation.controller;

import com.groom.e_commerce.payment.application.port.in.CancelPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ConfirmPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.GetPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ReadyPaymentUseCase;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.request.ReqReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResReadyPaymentV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제", description = "토스페이먼츠 결제 준비/승인/조회/취소 API")
public class PaymentControllerV1 {

	private final ConfirmPaymentUseCase confirmPaymentUseCase;
	private final CancelPaymentUseCase cancelPaymentUseCase;
	private final GetPaymentUseCase getPaymentUseCase;
	private final ReadyPaymentUseCase readyPaymentUseCase;

	public PaymentControllerV1(
		ConfirmPaymentUseCase confirmPaymentUseCase,
		CancelPaymentUseCase cancelPaymentUseCase,
		GetPaymentUseCase getPaymentUseCase,
		ReadyPaymentUseCase readyPaymentUseCase
	) {
		this.confirmPaymentUseCase = confirmPaymentUseCase;
		this.cancelPaymentUseCase = cancelPaymentUseCase;
		this.getPaymentUseCase = getPaymentUseCase;
		this.readyPaymentUseCase = readyPaymentUseCase;
	}

	// 결제 준비(리다이렉트 결제에 필요한 파라미터 반환)
	@Operation(
		summary = "결제 준비(ready)",
		description = "결제창 호출에 필요한 파라미터(clientKey, orderId, amount, successUrl/failUrl 등)를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 준비 성공",
			content = @Content(schema = @Schema(implementation = ResReadyPaymentV1.class))),
		@ApiResponse(responseCode = "400", description = "요청값 검증 실패")
	})
	@PostMapping("/ready")
	public ResponseEntity<ResReadyPaymentV1> ready(@Valid @RequestBody ReqReadyPaymentV1 request) {
		return ResponseEntity.ok(readyPaymentUseCase.ready(request));
	}

	// 결제 승인(토스 confirm)
	@Operation(
		summary = "결제 승인(confirm)",
		description = "paymentKey/orderId/amount를 받아 토스 결제 승인(confirm)을 수행하고 결제 정보를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 승인 성공",
			content = @Content(schema = @Schema(implementation = ResPaymentV1.class))),
		@ApiResponse(responseCode = "400", description = "요청값 오류 또는 결제 승인 실패"),
		@ApiResponse(responseCode = "409", description = "중복 승인(같은 orderId 재시도 등)")
	})
	@PostMapping("/confirm")
	public ResponseEntity<ResPaymentV1> confirm(@Valid @RequestBody ReqConfirmPaymentV1 request) {
		return ResponseEntity.ok(confirmPaymentUseCase.confirm(request));
	}

	// 결제 조회(토스 조회 or 내부 조회)
	@Operation(
		summary = "결제 조회(paymentKey)",
		description = "paymentKey로 결제 정보를 조회합니다. (내부 저장 데이터 또는 토스 조회 연동)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = ResPaymentV1.class))),
		@ApiResponse(responseCode = "404", description = "결제 정보 없음")
	})
	@GetMapping("/{paymentKey}")
	public ResponseEntity<ResPaymentV1> getByPaymentKey(
		@Parameter(description = "토스 결제 키(paymentKey)", example = "tviva2026...")
		@PathVariable String paymentKey
	) {
		return ResponseEntity.ok(getPaymentUseCase.getByPaymentKey(paymentKey));
	}

	// 주문ID로 결제 조회(내부)
	@Operation(
		summary = "결제 조회(orderId)",
		description = "orderId로 결제 정보를 조회합니다. (내부 저장 데이터 기준)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = ResPaymentV1.class))),
		@ApiResponse(responseCode = "404", description = "결제 정보 없음")
	})
	@GetMapping("/by-order/{orderId}")
	public ResponseEntity<ResPaymentV1> getByOrderId(
		@Parameter(description = "주문 ID(orderId). 현재 DTO가 UUID면 UUID 형식으로 입력", example = "550e8400-e29b-41d4-a716-446655440000")
		@PathVariable String orderId
	) {
		return ResponseEntity.ok(getPaymentUseCase.getByOrderId(orderId));
	}

	// 결제 취소(토스 cancel)
	@Operation(
		summary = "결제 취소(cancel)",
		description = "paymentKey 기준으로 토스 결제 취소를 수행합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 성공",
			content = @Content(schema = @Schema(implementation = ResCancelResultV1.class))),
		@ApiResponse(responseCode = "400", description = "요청값 오류 또는 취소 실패"),
		@ApiResponse(responseCode = "404", description = "결제 정보 없음")
	})
	@PostMapping("/{paymentKey}/cancel")
	public ResponseEntity<ResCancelResultV1> cancel(
		@Parameter(description = "취소할 결제의 paymentKey", example = "tviva2026...")
		@PathVariable String paymentKey,
		@Valid @RequestBody ReqCancelPaymentV1 request
	) {
		return ResponseEntity.ok(cancelPaymentUseCase.cancel(paymentKey, request));
	}
}
