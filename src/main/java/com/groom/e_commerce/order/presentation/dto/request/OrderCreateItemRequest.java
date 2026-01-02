package com.groom.e_commerce.order.presentation.dto.request;

import java.util.UUID;

import lombok.Getter;

@Getter
public class OrderCreateItemRequest {

	private UUID productId;
	private Integer quantity;
}
