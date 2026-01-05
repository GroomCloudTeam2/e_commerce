package com.groom.e_commerce.payment.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.application.port.in.CancelOrderItemPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.CancelPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ConfirmPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ReadyPaymentUseCase;
import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import com.groom.e_commerce.payment.application.port.out.OrderStatePort;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.entity.PaymentSplit;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.domain.repository.PaymentSplitRepository;
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
	CancelOrderItemPaymentUseCase {

	private static final String PG_PROVIDER_TOSS = "toss";

	private final PaymentRepository paymentRepository;
	private final PaymentSplitRepository paymentSplitRepository;

	private final TossPaymentPort tossPaymentPort;
	private final TossPaymentsProperties tossPaymentsProperties;

	private final OrderQueryPort orderQueryPort;
	private final OrderStatePort orderStatePort;

	public PaymentCommandService(
		PaymentRepository paymentRepository,
		PaymentSplitRepository paymentSplitRepository,
		TossPaymentPort tossPaymentPort,
		TossPaymentsProperties tossPaymentsProperties,
		OrderQueryPort orderQueryPort,
		OrderStatePort orderStatePort
	) {
		this.paymentRepository = paymentRepository;
		this.paymentSplitRepository = paymentSplitRepository;
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
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"AMOUNT_REQUIRED",
				"결제 금액이 필요합니다."
			);
		}

		// 1) 주문 요약 조회
		OrderQueryPort.OrderSummary order;
		try {
			order = orderQueryPort.getOrderSummary(orderId);
		} catch (PaymentException e) {
			throw e;
		} catch (Exception e) {
			throw new PaymentException(
				HttpStatus.NOT_FOUND,
				"ORDER_NOT_FOUND",
				"주문 정보를 찾을 수 없습니다."
			);
		}

		// 2) 금액 검증
		long orderTotal = order.totalPaymentAmt();
		if (orderTotal != requestAmount.longValue()) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_AMOUNT",
				"주문 금액과 결제 요청 금액이 일치하지 않습니다."
			);
		}

		// 3) 결제 레코드 확인
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 준비 정보를 찾을 수 없습니다."
			));

		// 4) READY 상태 확인
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"PAYMENT_NOT_READY",
				"결제 준비 상태가 아닙니다."
			);
		}

		// 5) 내부 결제 금액 2차 검증
		if (payment.getAmount() == null || !payment.getAmount().equals(requestAmount)) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"PAYMENT_AMOUNT_MISMATCH",
				"결제 준비 금액이 주문 금액과 일치하지 않습니다."
			);
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
	 * - ✅ 주문상품 목록 조회 후 PaymentSplit 생성/저장
	 * - 주문 상태 변경 (PENDING -> PAID)
	 */
	@Override
	public ResPaymentV1 confirm(ReqConfirmPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		if (requestAmount == null) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"AMOUNT_REQUIRED",
				"결제 금액이 필요합니다."
			);
		}

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 정보를 찾을 수 없습니다."
			));

		// 멱등 처리
		if (payment.isAlreadyPaid()) {
			return ResPaymentV1.from(payment);
		}
		if (payment.isAlreadyCancelled()) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"ALREADY_CANCELLED",
				"이미 취소된 결제입니다."
			);
		}

		// 금액 검증
		if (payment.getAmount() == null || !payment.getAmount().equals(requestAmount)) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_AMOUNT",
				"결제 요청 금액이 서버 금액과 일치하지 않습니다."
			);
		}

		// 토스 승인 호출
		TossPaymentResponse toss = tossPaymentPort.confirm(
			new TossConfirmRequest(request.paymentKey(), orderId.toString(), requestAmount)
		);

		// 토스 응답 금액 검증(선택)
		if (toss.totalAmount() != null && !toss.totalAmount().equals(requestAmount)) {
			throw new PaymentException(
				HttpStatus.BAD_GATEWAY,
				"PAYMENT_CONFIRM_AMOUNT_MISMATCH",
				"PG 승인 금액이 요청 금액과 일치하지 않습니다."
			);
		}

		// ✅ 결제 상태 반영
		payment.markPaid(toss.paymentKey(), toss.approvedAt());

		// ✅ PaymentSplit 생성/저장 (주문상품 기준)
		createPaymentSplitsIfNeeded(payment, orderId);

		Payment saved = paymentRepository.save(payment);

		// ✅ Order 상태 변경 (PENDING -> PAID)
		orderStatePort.payOrder(orderId);

		return ResPaymentV1.from(saved);
	}

	/**
	 * ✅ (신규) 아이템 단위 부분취소
	 * - Order -> Payment: {orderId, orderItemId, cancelAmount} 로 호출
	 * - PaymentSplit(orderItemId) 조회/검증 -> 토스 부분취소 -> payment/payment_cancel/split 반영
	 *
	 * ⚠️ 전제:
	 * - PaymentSplitRepository에 findByOrderItemIdWithLock(UUID) 가 존재해야 함
	 * - PaymentSplit 엔티티에 addCancel(long) 이 구현되어 있어야 함
	 */
	@Override
	public ResCancelResultV1 cancelOrderItem(UUID orderId, UUID orderItemId, long cancelAmount) {

		if (cancelAmount <= 0) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_CANCEL_AMOUNT",
				"취소 금액이 올바르지 않습니다."
			);
		}

		// 1) split 조회 (락 권장: 동시 취소 방지)
		PaymentSplit split = paymentSplitRepository.findByOrderItemIdWithLock(orderItemId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"SPLIT_NOT_FOUND",
				"취소 대상 결제 라인(split)을 찾을 수 없습니다."
			));

		// 2) 요청 orderId 검증
		if (!split.getOrderId().equals(orderId)) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"ORDER_MISMATCH",
				"주문 정보가 일치하지 않습니다."
			);
		}

		// 3) split 기준 취소 가능 금액 검증
		long splitRemaining = split.getItemAmount() - split.getCanceledAmount();
		if (cancelAmount > splitRemaining) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"EXCEED_SPLIT_CANCEL_AMOUNT",
				"아이템 취소 가능 금액을 초과했습니다."
			);
		}

		Payment payment = split.getPayment();

		// 4) payment 기준 취소 가능 금액 검증(2중 안전장치)
		long paymentRemaining = payment.getAmount() - payment.getCanceledAmount();
		if (cancelAmount > paymentRemaining) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"EXCEED_PAYMENT_CANCEL_AMOUNT",
				"결제 취소 가능 금액을 초과했습니다."
			);
		}

		// 5) 토스 부분취소 호출(금액 기반)
		TossCancelResponse tossCancel;
		try {
			tossCancel = tossPaymentPort.cancel(
				payment.getPaymentKey(),
				new TossCancelRequest("부분취소", cancelAmount)
			);
		} catch (TossApiException e) {
			// 토스가 "이미 취소됨" 같은 멱등 에러를 주는 케이스가 있으면 여기서 처리 가능
			throw e;
		}

		OffsetDateTime canceledAt = (tossCancel.canceledAt() != null)
			? tossCancel.canceledAt()
			: OffsetDateTime.now();

		// 6) payment_cancel 기록 + 누적 반영
		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			"부분취소",
			canceledAt
		);

		// payment 누적/상태 반영
		payment.addCancel(cancel);

		// split 누적/상태 반영
		split.addCancel(cancelAmount);

		// 저장 (split은 별 repo라 명시적으로 저장)
		paymentRepository.save(payment);
		paymentSplitRepository.save(split);

		return ResCancelResultV1.of(
			payment.getPaymentKey(),
			payment.getStatus().name(),
			payment.getCanceledAmount()
		);
	}

	/**
	 * ✅ 결제 취소(cancel)
	 */
	@Override
	public ResCancelResultV1 cancel(String paymentKey, ReqCancelPaymentV1 request) {
		Payment payment = paymentRepository.findByPaymentKey(paymentKey)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 정보를 찾을 수 없습니다."
			));

		// ✅ 멱등: 이미 전액 취소면 성공 응답
		if (payment.isAlreadyCancelled()) {
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
						"(IDEMPOTENT) already canceled in PG",
						canceledAt
					);

					payment.addCancel(cancel);
					paymentRepository.save(payment);
				}

				return ResCancelResultV1.of(
					payment.getPaymentKey(),
					payment.getStatus().name(),
					payment.getCanceledAmount()
				);
			}

			throw e;
		}

		OffsetDateTime canceledAt = (tossCancel.canceledAt() != null)
			? tossCancel.canceledAt()
			: OffsetDateTime.now();

		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			request.cancelReason(),
			canceledAt
		);

		payment.addCancel(cancel);

		Payment saved = paymentRepository.save(payment);

		return ResCancelResultV1.of(
			saved.getPaymentKey(),
			saved.getStatus().name(),
			saved.getCanceledAmount()
		);
	}

	/**
	 * 결제 승인 성공 후 주문상품 목록을 조회하여 PaymentSplit을 생성한다.
	 * - UNIQUE(order_item_id) 제약이 있는 경우, 이미 존재하면 생성 스킵(멱등)
	 */
	private void createPaymentSplitsIfNeeded(Payment payment, UUID orderId) {
		List<OrderQueryPort.OrderItemSnapshot> items = orderQueryPort.getOrderItems(orderId);
		if (items == null || items.isEmpty()) {
			throw new PaymentException(
				HttpStatus.BAD_GATEWAY,
				"ORDER_ITEMS_EMPTY",
				"주문상품 정보를 조회할 수 없습니다."
			);
		}

		List<PaymentSplit> splits = items.stream()
			.filter(i -> !paymentSplitRepository.existsByOrderItemId(i.orderItemId()))
			.map(i -> PaymentSplit.of(
				payment,
				orderId,
				i.orderItemId(),
				i.ownerId(),
				i.subtotal()
			))
			.collect(Collectors.toList());

		if (!splits.isEmpty()) {
			paymentSplitRepository.saveAll(splits);
		}
	}
}
