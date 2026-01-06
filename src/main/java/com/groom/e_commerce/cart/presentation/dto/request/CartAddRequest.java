package com.groom.e_commerce.cart.presentation.dto.request;

import java.util.UUID;

import com.groom.e_commerce.product.application.dto.StockManagement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest implements StockManagement {

	@NotNull
	private UUID productId;

	private UUID variantId;

	@Min(1)
	private Integer quantity;
}
