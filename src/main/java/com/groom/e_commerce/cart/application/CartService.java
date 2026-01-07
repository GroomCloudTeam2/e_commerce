package com.groom.e_commerce.cart.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.groom.e_commerce.cart.domain.entity.Cart;
import com.groom.e_commerce.cart.domain.entity.CartItem;
import com.groom.e_commerce.cart.domain.repository.CartItemRepository;
import com.groom.e_commerce.cart.domain.repository.CartRepository;
import com.groom.e_commerce.cart.presentation.dto.request.CartAddRequest;
import com.groom.e_commerce.cart.presentation.dto.response.CartItemResponse;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

import org.springframework.transaction.annotation.Transactional;
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
		StockManagement stockInfo = StockManagement.of(
			request.getProductId(),
			request.getVariantId(),
			request.getQuantity()
		);

		List<ProductCartInfo> productInfos = productService.getProductCartInfos(List.of(stockInfo));

		if (productInfos.isEmpty()) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		ProductCartInfo productInfo = productInfos.get(0);

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

	@Transactional(readOnly = true)
	public List<CartItemResponse> getMyCart(UUID userId) {
		Cart cart = cartRepository.findByUserId(userId).orElse(null);
		if (cart == null) {
			return List.of();
		}

		// 1. 내 장바구니의 모든 아이템 ID 리스트 가져오기
		List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);

		if (cartItems.isEmpty()) {
			return List.of();
		}
		List<StockManagement> stockInfos = cartItems.stream()
			.map(CartItem::toStockManagement) // DTO로 변환!
			.toList();
		List<ProductCartInfo> productInfos = productService.getProductCartInfos(stockInfos);

		// 3. 매핑을 위해 Map으로 변환 (Key: productId + variantId)
		Map<String, ProductCartInfo> infoMap = productInfos.stream()
			.collect(Collectors.toMap(
				info -> generateLookupKey(info.getProductId(), info.getVariantId()),
				Function.identity()
			));

		// 4. CartItem과 실시간 Product 정보를 합쳐서 Response 생성
		return cartItems.stream()
			.map(item -> {
				String key = generateLookupKey(item.getProductId(), item.getVariantId());
				ProductCartInfo info = infoMap.get(key);

				if (info == null) return null; // 상품이 삭제된 경우 등 예외처리

				return CartItemResponse.builder()
					.cartItemId(item.getId())
					.productId(item.getProductId())
					.variantId(item.getVariantId())
					.productName(info.getProductName())
					.optionName(info.getOptionName())
					.thumbnailUrl(info.getThumbnailUrl())
					.price(info.getPrice())
					.quantity(item.getQuantity())
					.totalPrice(info.getPrice() * item.getQuantity())
					.stockQuantity(info.getStockQuantity())
					.isAvailable(info.isAvailable())
					.build();
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private String generateLookupKey(UUID productId, UUID variantId) {
		return productId.toString() + (variantId != null ? variantId.toString() : "");
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

	public void updateItemQuantity(UUID userId, UUID itemId, int quantity) {
		CartItem item = cartItemRepository.findById(itemId)
			.orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

		if (!item.getCart().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.ACCESS_DENIED);
		}

		List<ProductCartInfo> productInfos = productService.getProductCartInfos(
			List.of(item.toStockManagement())
		);

		if (productInfos.isEmpty()) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		ProductCartInfo info = productInfos.get(0);
		if (!info.isAvailable()) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
		}
		if (info.getStockQuantity() < quantity) {
			throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
		}
		item.updateQuantity(quantity);
	}

	public void deleteCartItem(UUID userId, UUID itemId) {
		// 1. 삭제할 아이템 조회
		CartItem item = cartItemRepository.findById(itemId)
			.orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

		// 2. 권한 체크 (남의 장바구니 아이템을 지우면 안 됨)
		if (!item.getCart().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.ACCESS_DENIED);
		}

		// 3. 삭제
		cartItemRepository.delete(item);
	}

	public void removeCartItems(UUID userId, List<UUID> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}

		// 1. 삭제할 아이템들을 한 번에 조회
		List<CartItem> items = cartItemRepository.findAllById(itemIds);

		// 2. 본인의 장바구니 아이템인지 검증 (보안)
		items.forEach(item -> {
			if (!item.getCart().getUserId().equals(userId)) {
				throw new CustomException(ErrorCode.ACCESS_DENIED);
			}
		});

		// 3. 일괄 삭제 (쿼리 최적화)
		cartItemRepository.deleteAll(items);
	}
}
