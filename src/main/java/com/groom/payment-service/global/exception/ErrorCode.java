package com.groom.e_commerce.global.presentation.advice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "ALREADY_WITHDRAWN", "이미 탈퇴한 사용자입니다."),

    // Address
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "배송지를 찾을 수 없습니다."),
    ALREADY_DEFAULT_ADDRESS(HttpStatus.CONFLICT, "ALREADY_DEFAULT_ADDRESS", "이미 기본 배송지로 설정되어 있습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
    VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND", "상품 옵션(SKU)을 찾을 수 없습니다."),
    DUPLICATE_SKU_CODE(HttpStatus.CONFLICT, "DUPLICATE_SKU_CODE", "이미 존재하는 SKU 코드입니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_ON_SALE", "판매 중인 상품이 아닙니다."),
    PRODUCT_ALREADY_SUSPENDED(HttpStatus.CONFLICT, "PRODUCT_ALREADY_SUSPENDED", "이미 판매 정지된 상품입니다."),
    PRODUCT_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_SUSPENDED", "판매 정지 상태가 아닙니다."),
    PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PRODUCT_ACCESS_DENIED", "해당 상품에 대한 접근 권한이 없습니다."),
    STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "STOCK_NOT_ENOUGH", "재고가 부족합니다."),
    PRODUCT_HAS_ORDERS(HttpStatus.CONFLICT, "PRODUCT_HAS_ORDERS", "주문이 존재하는 상품은 삭제할 수 없습니다."),
    CATEGORY_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "CATEGORY_HAS_CHILDREN", "하위 카테고리가 존재하여 삭제할 수 없습니다."),
    CATEGORY_HAS_PRODUCTS(HttpStatus.BAD_REQUEST, "CATEGORY_HAS_PRODUCTS", "등록된 상품이 존재하여 삭제할 수 없습니다."),
    VARIANT_REQUIRED(HttpStatus.BAD_REQUEST, "VARIANT_REQUIRED", "옵션 상품은 variantId가 필요합니다."),
    VARIANT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "VARIANT_NOT_ALLOWED", "옵션이 없는 상품에는 variantId를 보낼 수 없습니다."),

	// Cart
	CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "장바구니에서 상품을 찾을 수 없습니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "해당 리소스에 대한 접근 권한이 없습니다."),
	// Cart
	CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", "장바구니를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
