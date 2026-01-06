package com.groom.e_commerce.order.presentation.dto.response;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.status.OrderStatus;

public record OrderResponse(
	UUID orderId,
	String orderNo,
	OrderStatus status,
	BigInteger totalAmount,
	LocalDateTime orderedAt,
	List<OrderItemResponse> items // 핵심: 리스트 포함
) {
	public static OrderResponse from(Order order) {
		List<OrderItemResponse> itemResponses = order.getItem().stream()
			.map(OrderItemResponse::from)
			.toList();

		// // 총 주문 금액 계산 (도메인 로직에 있다면 그것을 사용, 여기선 단순 합계)
		// BigDecimal total = itemResponses.stream()
		// 	.map(OrderItemResponse::subtotal)
		// 	.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new OrderResponse(
			order.getOrderId(),
			order.getOrderNumber(),
			order.getStatus(),
			order.getTotalPaymentAmount(),
			order.getCreatedAt(),
			// order.getTotalPaymentAmount(),
			itemResponses
		);
	}
}
