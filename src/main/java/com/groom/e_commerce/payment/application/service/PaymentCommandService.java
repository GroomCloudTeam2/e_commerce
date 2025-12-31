package com.groom.e_commerce.payment.application.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.application.port.in.CancelPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ConfirmPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ReadyPaymentUseCase;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentMethod;
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

// ✅ 주문 조회는 결제 준비에서 필요
// 너 프로젝트 실제 경로에 맞게 import 수정
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderRepository;

@Service
@Transactional
public class PaymentCommandService implements ConfirmPaymentUseCase, CancelPaymentUseCase, ReadyPaymentUseCase {

	private final PaymentRepository paymentRepository;
	private final TossPaymentPort tossPaymentPort;
	private final TossPaymentsProperties tossPaymentsProperties;
	private final OrderRepository orderRepository;

	public PaymentCommandService(
		PaymentRepository paymentRepository,
		TossPaymentPort tossPaymentPort,
		TossPaymentsProperties tossPaymentsProperties,
		OrderRepository orderRepository
	) {
		this.paymentRepository = paymentRepository;
		this.tossPaymentPort = tossPaymentPort;
		this.tossPaymentsProperties = tossPaymentsProperties;
		this.orderRepository = orderRepository;
	}

	/**
	 * ✅ 결제 준비(READY)
	 * - 주문 존재 확인
	 * - 금액 위변조 검증 (서버 기준)
	 * - 결제 레코드 존재 확인(주문 생성 시 Payment READY가 이미 생성돼있다는 전제)
	 * - 결제 상태 READY 확인
	 * - 토스 결제창 호출에 필요한 값(clientKey/successUrl/failUrl) 반환
	 *
	 * ⚠️ paymentKey는 이 단계에서 없음 (토스 결제 인증 후 successUrl redirect로 넘어올 때 생김)
	 */
	@Override
	@Transactional(readOnly = true)
	public ResReadyPaymentV1 ready(ReqReadyPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		// 1) 주문 조회
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"ORDER_NOT_FOUND",
				"주문 정보를 찾을 수 없습니다."
			));

		// 2) 금액 검증 (주문 총액 vs 요청 amount)
		long orderTotal = order.getTotalPaymentAmt(); // 너 주문 엔티티 필드명에 맞게 수정
		if (orderTotal != requestAmount) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_AMOUNT",
				"주문 금액과 결제 요청 금액이 일치하지 않습니다."
			);
		}

		// 3) 결제 레코드 확인 (주문 1 : 결제 1)
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 준비 정보를 찾을 수 없습니다."
			));

		// 4) 상태 READY 확인
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"PAYMENT_NOT_READY",
				"결제 준비 상태가 아닙니다."
			);
		}

		// 5) 결제창 표시용 이름들
		String orderName = "주문 " + order.getOrderNumber(); // 너 주문번호 필드명에 맞게 수정
		String customerName = order.getRecipientName();     // 또는 buyer nickname 등

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

	@Override
	public ResPaymentV1 confirm(ReqConfirmPaymentV1 request) {
		// 1) 내부 중복 방지(멱등)
		Payment existing = paymentRepository.findByPaymentKey(request.paymentKey()).orElse(null);
		if (existing != null && existing.isAlreadyDone()) {
			return ResPaymentV1.from(existing);
		}

		// 2) 토스 승인 호출
		TossPaymentResponse toss = tossPaymentPort.confirm(
			new TossConfirmRequest(request.paymentKey(), request.orderId(), request.amount())
		);

		// 3) 내부 저장/업데이트
		Payment payment = existing;
		if (payment == null) {
			payment = new Payment(toss.orderId(), toss.paymentKey(), toss.totalAmount());
		}

		payment.markApproved(
			toss.totalAmount(),
			mapMethod(toss.method()),
			toss.currency(),
			toss.orderName(),
			toss.customerName(),
			toss.requestedAt(),
			toss.approvedAt()
		);

		Payment saved = paymentRepository.save(payment);
		return ResPaymentV1.from(saved);
	}

	@Override
	public ResCancelResultV1 cancel(String paymentKey, ReqCancelPaymentV1 request) {
		Payment payment = paymentRepository.findByPaymentKey(paymentKey)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 정보를 찾을 수 없습니다."
			));

		if (payment.getStatus().name().equals("CANCELED")) {
			throw new PaymentException(HttpStatus.CONFLICT, "ALREADY_CANCELED", "이미 전액 취소된 결제입니다.");
		}

		// 부분취소 검증
		Long cancelAmount =
			request.cancelAmount() == null
				? (payment.getApprovedAmount() - payment.getCanceledAmount())
				: request.cancelAmount();

		if (cancelAmount <= 0) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_CANCEL_AMOUNT", "취소 금액이 올바르지 않습니다.");
		}
		if (payment.getCanceledAmount() + cancelAmount > payment.getApprovedAmount()) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "EXCEED_CANCEL_AMOUNT", "취소 가능 금액을 초과했습니다.");
		}

		// 토스 취소 호출
		TossCancelResponse tossCancel = tossPaymentPort.cancel(
			paymentKey,
			new TossCancelRequest(request.cancelReason(), cancelAmount)
		);

		// 토스 응답에서 취소 이력 1건을 대표로 저장
		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			request.cancelReason(),
			tossCancel.canceledAt()
		);
		payment.addCancel(cancel);

		Payment saved = paymentRepository.save(payment);

		return ResCancelResultV1.of(saved.getPaymentKey(), saved.getStatus().name(), saved.getCanceledAmount());
	}

	private PaymentMethod mapMethod(String tossMethod) {
		if (tossMethod == null) {
			return PaymentMethod.UNKNOWN;
		}
		return switch (tossMethod.toUpperCase()) {
			case "CARD" -> PaymentMethod.CARD;
			case "EASY_PAY" -> PaymentMethod.EASY_PAY;
			case "TRANSFER" -> PaymentMethod.TRANSFER;
			case "MOBILE_PHONE" -> PaymentMethod.MOBILE;
			default -> PaymentMethod.UNKNOWN;
		};
	}
}
