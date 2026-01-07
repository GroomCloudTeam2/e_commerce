package com.groom.e_commerce.product.application.dto;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockManagement {

	private UUID productId;
	private UUID variantId;
	private int quantity;

	public static StockManagement of(UUID productId, UUID variantId, int quantity) {
		return new StockManagement(productId, variantId, quantity);
	}
}
