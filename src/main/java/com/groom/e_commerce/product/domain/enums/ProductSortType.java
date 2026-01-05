package com.groom.e_commerce.product.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSortType {

	PRICE_ASC("price_asc", "가격 낮은순"),
	PRICE_DESC("price_desc", "가격 높은순"),
	NEWEST("newest", "최신순"),
	RATING("rating", "평점순");

	private final String value;
	private final String description;

	public static ProductSortType fromValue(String value) {
		if (value == null) {
			return NEWEST;
		}
		for (ProductSortType sortType : values()) {
			if (sortType.value.equalsIgnoreCase(value)) {
				return sortType;
			}
		}
		return NEWEST;
	}
}
