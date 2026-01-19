package com.groom.e_commerce.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.groom.e_commerce.order.domain.entity.OrderItem;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상품 응답")
public record OrderItemResponse(
	@Schema(description = "주문 상품 ID", example = "order_item_uuid_1234")
	UUID orderItemId,
	@Schema(description = "상품 ID", example = "product_uuid_1234")
	UUID productId,
	@Schema(description = "상품명", example = "친환경 에코백")
	String productName,
	@Schema(description = "단가", example = "15000")
	Long unitPrice,
	@Schema(description = "수량", example = "2")
	int quantity,
	@Schema(description = "소계", example = "30000")
	Long subtotal
) {
	// Entity -> DTO 변환 메서드 (Static Factory Method)
	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
			item.getOrderItemId(),
			item.getProductId(),
			item.getProductTitle(),
			item.getUnitPrice(),
			item.getQuantity(),
			item.getSubtotal()
		);
	}
}
