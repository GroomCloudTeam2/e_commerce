package com.groom.e_commerce.payment.infrastructure.adapter.in;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.application.port.out.PaymentPort;
import com.groom.e_commerce.payment.application.port.in.CancelPaymentByOrderUseCase;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentPortAdapter implements PaymentPort {

	private final CancelPaymentByOrderUseCase cancelPaymentByOrderUseCase;

	@Override
	public void cancelPayment(UUID orderId, Long cancelAmount, List<UUID> orderItemIds) {

		if (orderId == null) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"ORDER_ID_REQUIRED",
				"orderId가 필요합니다."
			);
		}

		if (cancelAmount == null || cancelAmount <= 0) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"INVALID_CANCEL_AMOUNT",
				"취소 금액이 올바르지 않습니다."
			);
		}

		// ✅ 선택지 B에서는 orderItemIds는 "Order 도메인 내부 정합성/검증용"일 뿐,
		// Payment는 split 안 쓰니까 itemIds로 뭘 안 함.
		// (원하면 null/empty만 막아도 되고, 아예 검증 제거도 가능)
		if (orderItemIds == null || orderItemIds.isEmpty()) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"ORDER_ITEM_IDS_REQUIRED",
				"취소 대상 주문상품(orderItemIds)이 필요합니다."
			);
		}

		// ✅ Order가 계산한 cancelAmount 그대로 결제 도메인에 위임
		cancelPaymentByOrderUseCase.cancelByOrder(orderId, cancelAmount, orderItemIds);
	}
}
