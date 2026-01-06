package com.groom.e_commerce.payment.infrastructure.adapter.in;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.application.port.out.PaymentPort;
import com.groom.e_commerce.payment.application.port.in.CancelOrderItemPaymentUseCase;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentPortAdapter implements PaymentPort {

	private final CancelOrderItemPaymentUseCase cancelOrderItemPaymentUseCase;

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
		if (orderItemIds == null || orderItemIds.isEmpty()) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"ORDER_ITEM_IDS_REQUIRED",
				"취소 대상 주문상품(orderItemIds)이 필요합니다."
			);
		}

		// ✅ 단순/명료 버전: 주문이 아이템별 cancelAmount를 계산해서 "1개 아이템" 단위로 호출
		if (orderItemIds.size() != 1) {
			throw new PaymentException(
				HttpStatus.BAD_REQUEST,
				"MULTI_ITEM_CANCEL_NOT_SUPPORTED",
				"단순 구현에서는 orderItemIds는 1개만 허용됩니다. (Order에서 아이템별로 따로 호출하세요)"
			);
		}

		UUID orderItemId = orderItemIds.get(0);

		// ✅ 주문이 계산한 금액을 그대로 사용해서 결제 도메인 유스케이스 호출
		cancelOrderItemPaymentUseCase.cancelOrderItem(orderId, orderItemId, cancelAmount);
	}
}
