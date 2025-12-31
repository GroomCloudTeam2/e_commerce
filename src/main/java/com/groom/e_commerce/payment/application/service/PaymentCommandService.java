package com.groom.e_commerce.payment.application.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.application.port.in.CancelPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ConfirmPaymentUseCase;
import com.groom.e_commerce.payment.application.port.in.ReadyPaymentUseCase;
import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
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

@Service
@Transactional
public class PaymentCommandService implements ConfirmPaymentUseCase, CancelPaymentUseCase, ReadyPaymentUseCase {

	private static final String PG_PROVIDER_TOSS = "toss";

	private final PaymentRepository paymentRepository;
	private final TossPaymentPort tossPaymentPort;
	private final TossPaymentsProperties tossPaymentsProperties;
	private final OrderQueryPort orderQueryPort;

	public PaymentCommandService(
		PaymentRepository paymentRepository,
		TossPaymentPort tossPaymentPort,
		TossPaymentsProperties tossPaymentsProperties,
		OrderQueryPort orderQueryPort
	) {
		this.paymentRepository = paymentRepository;
		this.tossPaymentPort = tossPaymentPort;
		this.tossPaymentsProperties = tossPaymentsProperties;
		this.orderQueryPort = orderQueryPort;
	}

	/**
	 * ✅ 결제 준비(READY)
	 * - 주문 존재 확인(Port)
	 * - 금액 위변조 검증 (서버 기준)
	 * - 결제 레코드 존재 확인(주문 1 : 결제 1, READY 레코드가 이미 존재한다는 전제)
	 * - 결제 상태 READY 확인
	 * - 토스 결제창 호출에 필요한 값(clientKey/successUrl/failUrl) 반환
	 */
	@Override
	@Transactional(readOnly = true)
	public ResReadyPaymentV1 ready(ReqReadyPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		// 1) 주문 요약 조회 (Order 도메인 직접 의존 제거)
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

		// 4) READY 상태 확인
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"PAYMENT_NOT_READY",
				"결제 준비 상태가 아닙니다."
			);
		}

		// (선택) 내부 amount가 주문 금액과 일치하는지 2차 검증
		if (!payment.getAmount().equals(requestAmount)) {
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
	 * - (권장) orderId 기준으로 Payment(READY) 찾고 금액 검증
	 * - 토스 confirm 호출
	 * - paymentKey/approvedAt 저장 + status PAID로 변경
	 *
	 * ⚠️ ERD 100% 버전 Payment에는 method/currency/orderName/customerName/requestedAt 없음
	 */
	@Override
	public ResPaymentV1 confirm(ReqConfirmPaymentV1 request) {
		UUID orderId = request.orderId();
		Long requestAmount = request.amount();

		// 1) 내부 Payment(READY) 조회 (주문 1 : 결제 1)
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 정보를 찾을 수 없습니다."
			));

		// 2) 멱등 처리: 이미 PAID면 그대로 반환
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

		// 3) 금액 위변조 검증 (내부 결제 amount vs 요청 amount)
		if (!payment.getAmount().equals(requestAmount)) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_AMOUNT",
				"결제 요청 금액이 서버 금액과 일치하지 않습니다."
			);
		}

		// 4) 토스 승인 호출
		TossPaymentResponse toss = tossPaymentPort.confirm(
			new TossConfirmRequest(request.paymentKey(), orderId.toString(), requestAmount)
		);

		// 5) (선택) 토스 응답 금액 검증
		if (toss.totalAmount() != null && !toss.totalAmount().equals(requestAmount)) {
			throw new PaymentException(
				HttpStatus.BAD_GATEWAY,
				"PAYMENT_CONFIRM_AMOUNT_MISMATCH",
				"PG 승인 금액이 요청 금액과 일치하지 않습니다."
			);
		}

		// 6) 내부 상태 업데이트 (ERD 기준)
		payment.markPaid(toss.paymentKey(), toss.approvedAt());

		Payment saved = paymentRepository.save(payment);
		return ResPaymentV1.from(saved);
	}

	/**
	 * ✅ 결제 취소(cancel)
	 * - 취소 누적합은 PaymentCancel 합산으로 계산 (ERD에 canceled_amount 없음)
	 * - 남은 금액 = amount - getCanceledAmount()
	 */
	@Override
	public ResCancelResultV1 cancel(String paymentKey, ReqCancelPaymentV1 request) {
		Payment payment = paymentRepository.findByPaymentKey(paymentKey)
			.orElseThrow(() -> new PaymentException(
				HttpStatus.NOT_FOUND,
				"PAYMENT_NOT_FOUND",
				"결제 정보를 찾을 수 없습니다."
			));

		if (payment.isAlreadyCancelled()) {
			throw new PaymentException(
				HttpStatus.CONFLICT,
				"ALREADY_CANCELLED",
				"이미 전액 취소된 결제입니다."
			);

		}

		// 부분취소 검증
		long remaining = payment.getAmount() - payment.getCanceledAmount();
		Long cancelAmount = (request.cancelAmount() == null) ? remaining : request.cancelAmount();

		if (cancelAmount == null || cancelAmount <= 0) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_CANCEL_AMOUNT", "취소 금액이 올바르지 않습니다.");
		}
		if (cancelAmount > remaining) {
			throw new PaymentException(HttpStatus.BAD_REQUEST, "EXCEED_CANCEL_AMOUNT", "취소 가능 금액을 초과했습니다.");
		}

		// 토스 취소 호출
		TossCancelResponse tossCancel = tossPaymentPort.cancel(
			paymentKey,
			new TossCancelRequest(request.cancelReason(), cancelAmount)
		);

		// 취소 이력 저장
		PaymentCancel cancel = new PaymentCancel(
			tossCancel.paymentKey(),
			cancelAmount,
			request.cancelReason(),
			tossCancel.canceledAt()
		);
		payment.addCancel(cancel); // 전액 취소면 내부에서 status CANCELLED로 바뀜

		Payment saved = paymentRepository.save(payment);

		return ResCancelResultV1.of(
			saved.getPaymentKey(),
			saved.getStatus().name(),
			saved.getCanceledAmount()
		);
	}
}
