package com.groom.e_commerce.product.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

	ON_SALE("판매중"),
	SOLD_OUT("품절"),
	HIDDEN("숨김"),
	SUSPENDED("정지"),
	DELETED("삭제");

	private final String description;

	public boolean isOnSale() {
		return this == ON_SALE;
	}

	public boolean isSuspended() {
		return this == SUSPENDED;
	}

	public boolean isDeleted() {
		return this == DELETED;
	}
}
