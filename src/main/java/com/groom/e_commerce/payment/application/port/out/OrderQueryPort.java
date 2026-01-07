// src/main/java/com/groom/e_commerce/payment/application/port/out/OrderQueryPort.java
package com.groom.e_commerce.payment.application.port.out;

import java.util.List;
import java.util.UUID;

public interface OrderQueryPort {

	OrderSummary getOrderSummary(UUID orderId);

	/**
	 * 결제 도메인이 split 생성을 위해 주문상품(아이템) 스냅샷을 조회한다.
	 * (주문 도메인 연동 전에는 Stub에서 더미 데이터 반환)
	 */
	List<OrderItemSnapshot> getOrderItems(UUID orderId);

	record OrderSummary(
		UUID orderId,
		long totalPaymentAmt,
		String orderNumber,
		String recipientName
	) {}

	/**
	 * 주문 도메인의 엔티티를 직접 참조하지 않고, 결제에 필요한 최소 정보만 전달하기 위한 스냅샷.
	 */
	record OrderItemSnapshot(
		UUID orderItemId,
		UUID ownerId,
		long subtotal
	) {}
}
