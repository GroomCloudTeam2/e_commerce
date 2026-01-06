package com.groom.e_commerce.cart.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest {

	@NotNull
	private UUID productId;

	private UUID variantId;

	@Min(1)
	private int quantity;
}