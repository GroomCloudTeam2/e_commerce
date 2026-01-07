// src/main/java/com/groom/e_commerce/payment/application/port/out/OrderItemSnapshot.java
package com.groom.e_commerce.payment.application.port.out;

/**
 * 결제 도메인이 주문 도메인(또는 Stub)에서 필요한 최소 정보만 받기 위한 스냅샷 모델.
 * - Payment Split 생성에 필요한 값만 포함
 */
public class OrderItemSnapshot {

	private final String orderItemId;
	private final String ownerId;
	private final Long subtotal;

	public OrderItemSnapshot(String orderItemId, String ownerId, Long subtotal) {
		if (orderItemId == null || orderItemId.isBlank()) {
			throw new IllegalArgumentException("orderItemId must not be blank");
		}
		if (ownerId == null || ownerId.isBlank()) {
			throw new IllegalArgumentException("ownerId must not be blank");
		}
		if (subtotal == null || subtotal <= 0) {
			throw new IllegalArgumentException("subtotal must be > 0");
		}
		this.orderItemId = orderItemId;
		this.ownerId = ownerId;
		this.subtotal = subtotal;
	}

	public String getOrderItemId() {
		return orderItemId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public Long getSubtotal() {
		return subtotal;
	}
}
