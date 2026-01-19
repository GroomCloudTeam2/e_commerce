package com.groom.e_commerce.order.presentation.dto.response;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.status.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderResponse(
	@Schema(description = "주문 ID", example = "order_uuid_1234")
	UUID orderId,
	@Schema(description = "주문 번호", example = "20240105-0001")
	String orderNo,
	@Schema(description = "주문 상태", example = "PENDING")
	OrderStatus status,
	Long totalAmount,
	LocalDateTime orderedAt,
	List<OrderItemResponse> items // 핵심: 리스트 포함
) {
	public static OrderResponse from(Order order) {
		List<OrderItemResponse> itemResponses = order.getItem().stream()
			.map(OrderItemResponse::from)
			.toList();

		return new OrderResponse(
			order.getOrderId(),
			order.getOrderNumber(),
			order.getStatus(),
			order.getTotalPaymentAmount(),
			order.getCreatedAt(),
			itemResponses
		);
	}
}
