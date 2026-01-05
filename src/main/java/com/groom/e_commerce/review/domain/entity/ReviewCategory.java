package com.groom.e_commerce.review.domain.entity;

public enum ReviewCategory {
	PRICE,
	DESIGN,
	PERFORMANCE,
	CONVENIENCE,
	QUALITY,
	ERR;

	public static ReviewCategory fromAiCategory(String aiCategory) {
		return switch (aiCategory) {
			case "디자인/외형" -> DESIGN;
			case "성능/기능" -> PERFORMANCE;
			case "편의성/사용감" -> CONVENIENCE;
			case "가격/구성" -> PRICE;
			case "품질/내구성" -> QUALITY;
			default -> null;
		};
	}
}
