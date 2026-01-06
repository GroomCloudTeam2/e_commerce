package com.groom.e_commerce.product.application.dto;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


public interface StockManagement {
	UUID getProductId();
	UUID getVariantId();
	Integer getQuantity();


}
