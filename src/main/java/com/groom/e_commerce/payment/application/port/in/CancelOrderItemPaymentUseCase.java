// src/main/java/com/groom/e_commerce/payment/application/port/in/CancelOrderItemPaymentUseCase.java
package com.groom.e_commerce.payment.application.port.in;

import java.util.UUID;

import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

public interface CancelOrderItemPaymentUseCase {

	/**
	 * 주문상품(orderItemId) 단위 부분취소
	 * @param orderId 주문 ID
	 * @param orderItemId 주문상품 ID
	 * @param cancelAmount 취소할 금액
	 */
	ResCancelResultV1 cancelOrderItem(UUID orderId, UUID orderItemId, long cancelAmount);
}
