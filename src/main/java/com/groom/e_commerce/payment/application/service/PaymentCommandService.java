package com.groom.e_commerce.payment.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.application.port.in.CancelPaymentByOrderUseCase;
import com.groom.e_commerce.payment.application.port.in.CancelPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ConfirmPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ReadyPaymentUseCase;
import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import com.groom.e_commerce.payment.application.port.out.OrderStatePort;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.infrastructure.api.toss.config.TossPaymentsProperties;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.request.ReqReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

@Service
@Transactional
public class PaymentCommandService implements
	ConfirmPaymentUseCase,
	CancelPaymentUseCase,
	ReadyPaymentUseCase,
	CancelPaymentByOrderUseCase {

	private final PaymentRepository paymentRepository;

	private final TossPaymentPort tossPaymentPort;
	private final TossPaymentsProperties tossPaymentsProperties;

	private final OrderQueryPort orderQueryPort;
	private final OrderStatePort orderStatePort;

	public PaymentCommandService(
		PaymentRepository paymentRepository,
		TossPaymentPort tossPaymentPort,
		TossPaymentsProperties tossPaymentsProperties,
		OrderQueryPort orderQueryPort,
		OrderStatePort orderStatePort
	) {
		this.paymentRepository = paymentRepository;
		this.tossPaymentPort = tossPaymentPort;
		this.tossPaymentsProperties = tossPaymentsProperties;
		this.orderQueryPort = orderQueryPort;
		this.orderStatePort = orderStatePort;
	}

	/**
	 * ✅ 결제 준비(READY)
	 */
	@Override
	@Transactional(readOnly = true)
	public ResReadyPaymentV1 ready(ReqReadyPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		if (requestAmount == null) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "AMOUNT_REQUIRED", "결제 금액이 필요합니다.");
		}

		// 1) 주문 요약 조회
		OrderQueryPort.OrderSummary order;
		try {
			order = orderQueryPort.getOrderSummary(orderId);
		} catch (PaymentException e) {
			throw e;
		} catch (Exception e) {
			throw new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문 정보를 찾을 수 없습니다.");
		}

		// 2) 금액 검증
		long orderTotal = order.totalPaymentAmt();
		if (orderTotal != requestAmount.longValue()) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "주문 금액과 결제 요청 금액이 일치하지 않습니다.");
		}

		// 3) 결제 레코드 확인
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 준비 정보를 찾을 수 없습니다."));

		// 4) READY 상태 확인
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_READY", "결제 준비 상태가 아닙니다.");
		}

		// 5) 내부 결제 금액 2차 검증
		if (payment.getAmount() == null || !payment.getAmount().equals(requestAmount)) {
			throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_AMOUNT_MISMATCH", "결제 준비 금액이 주문 금액과 일치하지 않습니다.");
		}

		String orderName = "주문 " + order.orderNumber();
		String customerName = order.recipientName();

		return new ResReadyPaymentV1(
			orderId,
			requestAmount,
			orderName,
			customerName,
			tossPaymentsProperties.clientKey(),
			tossPaymentsProperties.successUrl(),
			tossPaymentsProperties.failUrl()
		);
	}

	/**
	 * ✅ 결제 승인(confirm)
	 * - 토스 승인 성공 후 Payment 상태 변경
	 * - 주문 상태 변경(PENDING -> PAID)
	 */
	@Override
	public ResPaymentV1 confirm(ReqConfirmPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		if (requestAmount == null) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "AMOUNT_REQUIRED", "결제 금액이 필요합니다.");
		}

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."));

		// 멱등 처리
		if (payment.isAlreadyPaid()) {
			return ResPaymentV1.from(payment);
		}
		if (payment.isAlreadyCancelled()) {
			throw new PaymentException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "이미 취소된 결제입니다.");
		}

		// 금액 검증
		if (payment.getAmount() == null || !payment.getAmount().equals(requestAmount)) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "결제 요청 금액이 서버 금액과 일치하지 않습니다.");
		}

		// 토스 승인 호출
		TossPaymentResponse toss = tossPaymentPort.confirm(
			new TossConfirmRequest(request.paymentKey(), orderId.toString(), requestAmount)
		);

		// 토스 응답 금액 검증(선택)
		if (toss.totalAmount() != null && !toss.totalAmount().equals(requestAmount)) {
			throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYMENT_CONFIRM_AMOUNT_MISMATCH", "PG 승인 금액이 요청 금액과 일치하지 않습니다.");
		}

		// 결제 상태 반영
		payment.markPaid(toss.paymentKey(), toss.approvedAt());

		Payment saved = paymentRepository.save(payment);

		// 주문 상태 변경
		orderStatePort.payOrder(orderId);

		return ResPaymentV1.from(saved);
	}

	/**
	 * ✅ 주문 도메인에서 {orderId, cancelAmount, orderItemIds}로 요청하는 "총액 취소"
	 * - PG(토스) 취소는 cancelAmount로 1회 호출
	 */
	@Override
	public ResCancelResultV1 cancelByOrder(UUID orderId, Long cancelAmount, List<UUID> orderItemIds) {

		if (orderId == null) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "ORDER_ID_REQUIRED", "orderId가 필요합니다.");
		}
		if (cancelAmount == null || cancelAmount <= 0) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_CANCEL_AMOUNT", "취소 금액이 올바르지 않습니다.");
		}
		if (orderItemIds == null || orderItemIds.isEmpty()) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "ORDER_ITEM_IDS_REQUIRED", "취소 대상 주문상품 목록이 필요합니다.");
		}

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."));

		// ✅ 멱등: 이미 전액 취소면 성공 응답 (+ 주문에도 멱등 통지)
		if (payment.isAlreadyCancelled()) {
			// ✅ 주문 도메인에도 "이미 취소 상태"를 동기화(멱등)
			orderStatePort.cancelOrderByPayment(orderId, 0L, payment.getCanceledAmount(), payment.getStatus().name(), orderItemIds);

			return ResCancelResultV1.of(
				payment.getPaymentKey(),
				payment.getStatus().name(),
				payment.getCanceledAmount()
			);
		}

		long remaining = payment.getAmount() - payment.getCanceledAmount();
		if (cancelAmount > remaining) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "EXCEED_CANCEL_AMOUNT", "결제 취소 가능 금액을 초과했습니다.");
		}

		// ✅ 토스 취소 1회
		TossCancelResponse tossCancel;
		try {
			tossCancel = tossPaymentPort.cancel(
				payment.getPaymentKey(),
				new TossCancelRequest("ORDER_CANCEL", cancelAmount)
			);
		} catch (TossApiException e) {
			throw e;
		}

		OffsetDateTime canceledAt = (tossCancel.canceledAt() != null) ? tossCancel.canceledAt() : OffsetDateTime.now();

		// ✅ payment_cancel 1건 기록 + 누적 반영
		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			canceledAt
		);
		payment.addCancel(cancel);

		Payment saved = paymentRepository.save(payment);

		// ✅ 취소 완료를 주문 도메인에 통지
		// - 이번에 취소한 금액(cancelAmount)
		// - 누적 취소 금액(saved.getCanceledAmount())
		// - 결제 상태(saved.getStatus())
		orderStatePort.cancelOrderByPayment(orderId, cancelAmount, saved.getCanceledAmount(), saved.getStatus().name(), orderItemIds);

		return ResCancelResultV1.of(
			saved.getPaymentKey(),
			saved.getStatus().name(),
			saved.getCanceledAmount()
		);
	}

	/**
	 * ✅ 결제 취소(cancel) (paymentKey 기반 API)
	 */
	@Override
	public ResCancelResultV1 cancel(String paymentKey, ReqCancelPaymentV1 request) {
		Payment payment = paymentRepository.findByPaymentKey(paymentKey)
			.orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."));

		UUID orderId = payment.getOrderId();

		// 멱등: 이미 전액 취소면 성공 응답 (+ 주문에도 멱등 통지)
		if (payment.isAlreadyCancelled()) {
			orderStatePort.cancelOrderByPayment(orderId, 0L, payment.getCanceledAmount(), payment.getStatus().name(), List.of());

			return ResCancelResultV1.of(
				payment.getPaymentKey(),
				payment.getStatus().name(),
				payment.getCanceledAmount()
			);
		}

		long remaining = payment.getAmount() - payment.getCanceledAmount();
		Long cancelAmount = (request.cancelAmount() == null) ? remaining : request.cancelAmount();

		if (cancelAmount == null || cancelAmount <= 0) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_CANCEL_AMOUNT", "취소 금액이 올바르지 않습니다.");
		}
		if (cancelAmount > remaining) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "EXCEED_CANCEL_AMOUNT", "취소 가능 금액을 초과했습니다.");
		}

		TossCancelResponse tossCancel;
		try {
			tossCancel = tossPaymentPort.cancel(
				paymentKey,
				new TossCancelRequest(request.cancelReason(), cancelAmount)
			);
		} catch (TossApiException e) {
			// ✅ 토스: 이미 취소됨 -> 성공처럼 처리 + DB 보정
			if ("ALREADY_CANCELED_PAYMENT".equals(e.getTossErrorCode())) {

				long remainingNow = payment.getAmount() - payment.getCanceledAmount();
				if (remainingNow > 0) {
					OffsetDateTime canceledAt = OffsetDateTime.now();

					PaymentCancel cancel = new PaymentCancel(
						paymentKey,
						remainingNow,
						canceledAt
					);

					payment.addCancel(cancel);
					paymentRepository.save(payment);
				}

				// ✅ DB 보정 후 주문 도메인에도 통지
				orderStatePort.cancelOrderByPayment(orderId, cancelAmount, payment.getCanceledAmount(), payment.getStatus().name(), List.of());

				return ResCancelResultV1.of(
					payment.getPaymentKey(),
					payment.getStatus().name(),
					payment.getCanceledAmount()
				);
			}

			throw e;
		}

		OffsetDateTime canceledAt = (tossCancel.canceledAt() != null) ? tossCancel.canceledAt() : OffsetDateTime.now();

		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			canceledAt
		);

		payment.addCancel(cancel);

		Payment saved = paymentRepository.save(payment);

		// ✅ 취소 완료를 주문 도메인에 통지
		orderStatePort.cancelOrderByPayment(orderId, cancelAmount, saved.getCanceledAmount(), saved.getStatus().name(), List.of());

		return ResCancelResultV1.of(
			saved.getPaymentKey(),
			saved.getStatus().name(),
			saved.getCanceledAmount()
		);
	}
}
