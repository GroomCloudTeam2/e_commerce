package com.groom.e_commerce.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.groom.e_commerce.order.domain.entity.OrderItem;

public record OrderItemResponse(
	UUID productId,
	String productName,
	Long unitPrice,
	int quantity,
	Long subtotal
) {
	// Entity -> DTO 변환 메서드 (Static Factory Method)
	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
			item.getProductId(),
			item.getProductTitle(),
			item.getUnitPrice(),
			item.getQuantity(),
			item.getSubtotal()
		);
	}
}
