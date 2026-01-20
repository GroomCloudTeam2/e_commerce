package com.groom.e_commerce.globalv2.presentation.advice;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// =====================
	// Common
	// =====================
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 요청입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다."),

	// =====================
	// Auth
	// =====================
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 토큰입니다."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_005", "비밀번호가 일치하지 않습니다."),

	// =====================
	// User
	// =====================
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 닉네임입니다."),
	ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "USER_004", "이미 탈퇴한 사용자입니다."),

	// =====================
	// Address
	// =====================
	ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS_001", "배송지를 찾을 수 없습니다."),
	ALREADY_DEFAULT_ADDRESS(HttpStatus.CONFLICT, "ADDRESS_002", "이미 기본 배송지로 설정되어 있습니다."),

	// =====================
	// Cart
	// =====================
	CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001", "장바구니에 상품이 없습니다."),
	CART_EMPTY(HttpStatus.BAD_REQUEST, "CART_002", "장바구니가 비어 있습니다."),

	// =====================
	// Product
	// =====================
	CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "카테고리를 찾을 수 없습니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_002", "상품을 찾을 수 없습니다."),
	PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "PRODUCT_003", "판매 중인 상품이 아닙니다."),
	STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "PRODUCT_004", "재고가 부족합니다."),

	// =====================
	// External / Feign
	// =====================
	PRODUCT_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "EXT_001", "상품 서비스 응답 오류");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
