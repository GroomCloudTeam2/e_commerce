package com.groom.e_commerce.cart.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.groom.e_commerce.cart.domain.entity.CartItem;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;

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
	private Long price;    // 개당 단가
	private int quantity;        // 내가 담은 수량
	private Long totalPrice; // price * quantity
	private int stockQuantity;   // 실시간 재고
	private boolean isAvailable; // 현재 구매 가능 여부

	public static CartItemResponse of(CartItem item, ProductCartInfo info) {
		return CartItemResponse.builder()
			.cartItemId(item.getId())
			.productId(item.getProductId())
			.variantId(item.getVariantId())
			.productName(info.getProductName())
			.optionName(info.getOptionName())
			.thumbnailUrl(info.getThumbnailUrl())
			.price(info.getPrice())
			.quantity(item.getQuantity())
			.totalPrice(info.getPrice() * item.getQuantity())
			.stockQuantity(info.getStockQuantity())
			.isAvailable(info.isAvailable())
			.build();

	}
}
