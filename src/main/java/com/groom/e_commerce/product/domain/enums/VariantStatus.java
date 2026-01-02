package com.groom.e_commerce.product.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VariantStatus {

	ON_SALE("판매중"),
	SOLD_OUT("품절"),
	DISCONTINUED("단종");

	private final String description;

	public boolean isOnSale() {
		return this == ON_SALE;
	}

	public boolean isDiscontinued() {
		return this == DISCONTINUED;
	}
}
