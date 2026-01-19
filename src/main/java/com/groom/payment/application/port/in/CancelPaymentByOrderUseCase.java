// src/main/java/com/groom/e_commerce/payment/application/port/in/CancelPaymentByOrderUseCase.java
package com.groom.e_commerce.payment.application.port.in;

import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

public interface CancelPaymentByOrderUseCase {

	/**
	 * 주문 도메인 요청 기반 "총액" 부분취소
	 * - PG(토스)에는 cancelAmount 1번만 취소 요청
	 */
	ResCancelResultV1 cancelByOrder(UUID orderId, Long cancelAmount, List<UUID> orderItemIds);
}
