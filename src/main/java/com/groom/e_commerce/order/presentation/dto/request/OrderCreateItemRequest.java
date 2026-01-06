package com.groom.e_commerce.order.presentation.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateItemRequest {

	private UUID productId;
	private Integer quantity;
}
