package com.groom.e_commerce.cart.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.groom.e_commerce.cart.domain.entity.Cart;
import com.groom.e_commerce.cart.domain.entity.CartItem;
import com.groom.e_commerce.cart.domain.repository.CartItemRepository;
import com.groom.e_commerce.cart.domain.repository.CartRepository;
import com.groom.e_commerce.cart.presentation.dto.request.CartAddRequest;
import com.groom.e_commerce.cart.presentation.dto.response.CartItemResponse;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductServiceV1 productService;


	public void addItemToCart(UUID userId, CartAddRequest request) {
		// 0. 상품 정보 조회 및 검증
		ProductCartInfo productInfo = productService.getProductCartInfo(request.getProductId(), request.getVariantId());

		if (!productInfo.isAvailable()) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
		}

		if (productInfo.getStockQuantity() < request.getQuantity()) {
			throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
		}

		// 1. 장바구니 존재 확인 (없으면 생성)
		Cart cart = cartRepository.findByUserId(userId)
			.orElseGet(() -> cartRepository.save(new Cart(userId)));

		// 2. 중복 상품 확인 (하이브리드 체크)
		CartItem existItem = findExistItem(cart, request);

		if (existItem != null) {
			// 3-1. 이미 있으면 수량만 추가 (Dirty Checking)
			// 재고 체크 (기존 수량 + 추가 수량)
			if (productInfo.getStockQuantity() < existItem.getQuantity() + request.getQuantity()) {
				throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
			}
			existItem.addQuantity(request.getQuantity());
		} else {
			// 3-2. 없으면 신규 생성
			CartItem newItem = CartItem.builder()
				.cart(cart)
				.productId(request.getProductId())
				.variantId(request.getVariantId())
				.quantity(request.getQuantity())
				.build();
			cartItemRepository.save(newItem);
		}
	}

	private CartItem findExistItem(Cart cart, CartAddRequest request) {
		if (request.getVariantId() != null) {
			return cartItemRepository.findByCartAndVariantId(cart, request.getVariantId())
				.orElse(null);
		} else {
			return cartItemRepository.findByCartAndProductIdAndVariantIdIsNull(cart, request.getProductId())
				.orElse(null);
		}
	}


}
