package com.groom.e_commerce.order.presentation.dto.request;

import java.util.UUID;

import com.groom.e_commerce.product.application.dto.StockManagement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateItemRequest {
        private UUID productId;
        private UUID variantId;
		private Integer quantity;
	}
