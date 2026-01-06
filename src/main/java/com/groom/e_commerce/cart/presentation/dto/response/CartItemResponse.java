package com.groom.e_commerce.cart.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {
	private UUID cartItemId;
	private UUID productId;
	private UUID variantId;      // 단일 상품이면 null
	private String productName;
	private String optionName;   // 단일 상품이면 null
	private String thumbnailUrl;
	private BigDecimal price;    // 개당 단가
	private int quantity;        // 내가 담은 수량
	private BigDecimal totalPrice; // price * quantity
	private int stockQuantity;   // 실시간 재고
	private boolean isAvailable; // 현재 구매 가능 여부
}
