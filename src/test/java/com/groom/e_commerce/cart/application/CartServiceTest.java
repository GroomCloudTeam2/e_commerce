package com.groom.e_commerce.cart.application;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.cart.domain.entity.Cart;
import com.groom.e_commerce.cart.domain.entity.CartItem;
import com.groom.e_commerce.cart.domain.repository.CartItemRepository;
import com.groom.e_commerce.cart.domain.repository.CartRepository;
import com.groom.e_commerce.cart.presentation.dto.request.CartAddRequest;
import com.groom.e_commerce.cart.presentation.dto.response.CartItemResponse;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.service.ProductServiceV1;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@InjectMocks
	private CartService cartService;

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private ProductServiceV1 productService;

	@Test
	@DisplayName("장바구니에 없는 새로운 상품을 담으면 새로 저장된다.")
	void addItemToCart_NewItem() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		CartAddRequest request = CartAddRequest.builder()
			.productId(productId)
			.quantity(2)
			.build();

		Cart cart = new Cart(userId);
		ProductCartInfo productInfo = createProductInfo(productId, 100); // 재고 100개

		// mocking
		given(productService.getProductCartInfos(any())).willReturn(List.of(productInfo));
		given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
		given(cartItemRepository.findByCartAndProductIdAndVariantIdIsNull(cart, productId))
			.willReturn(Optional.empty()); // 기존 아이템 없음

		// when
		cartService.addItemToCart(userId, request);

		// then
		verify(cartItemRepository, times(1)).save(any(CartItem.class));
	}

	@Test
	@DisplayName("이미 장바구니에 있는 상품을 담으면 수량이 증가한다.")
	void addItemToCart_ExistItem() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		CartAddRequest request = CartAddRequest.builder()
			.productId(productId)
			.quantity(3)
			.build();

		Cart cart = new Cart(userId);
		CartItem existItem = CartItem.builder()
			.cart(cart)
			.productId(productId)
			.quantity(5)
			.build();

		ProductCartInfo productInfo = createProductInfo(productId, 100);

		// mocking
		given(productService.getProductCartInfos(any())).willReturn(List.of(productInfo));
		given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
		given(cartItemRepository.findByCartAndProductIdAndVariantIdIsNull(cart, productId))
			.willReturn(Optional.of(existItem));

		// when
		cartService.addItemToCart(userId, request);

		// then
		assertThat(existItem.getQuantity()).isEqualTo(5 + 3); // 8개로 증가 확인
		verify(cartItemRepository, never()).save(any(CartItem.class)); // save는 호출 안 됨 (Dirty Checking)
	}

	@Test
	@DisplayName("재고보다 많은 수량을 담으려 하면 예외가 발생한다.")
	void addItemToCart_StockNotEnough() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		CartAddRequest request = CartAddRequest.builder().productId(productId).quantity(10).build();

		ProductCartInfo productInfo = createProductInfo(productId, 5); // 재고 5개 뿐

		given(productService.getProductCartInfos(any())).willReturn(List.of(productInfo));

		// when & then
		assertThatThrownBy(() -> cartService.addItemToCart(userId, request))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOCK_NOT_ENOUGH);
	}

	@Test
	@DisplayName("내 장바구니 목록을 조회한다.")
	void getMyCart() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		Cart cart = new Cart(userId);
		CartItem item = CartItem.builder().cart(cart).productId(productId).quantity(2).build();

		ProductCartInfo productInfo = createProductInfo(productId, 100);

		given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
		given(cartItemRepository.findAllByCart(cart)).willReturn(List.of(item));
		given(productService.getProductCartInfos(any())).willReturn(List.of(productInfo));

		// when
		List<CartItemResponse> result = cartService.getMyCart(userId);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getProductName()).isEqualTo("Test Product");
		assertThat(result.get(0).getTotalPrice()).isEqualTo(1000 * 2);
	}

	@Test
	@DisplayName("장바구니 아이템 수량을 수정한다.")
	void updateItemQuantity() {
		// given
		UUID userId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		Cart cart = new Cart(userId);

		// Reflection 등으로 ID 주입이 불가능하다면 Mock 객체 활용 추천, 여기선 Builder 패턴 가정
		// (Entity에 id Setter가 없다면 테스트용 생성자나 Reflection 필요. 편의상 로직 흐름만 검증)
		CartItem item = CartItem.builder().cart(cart).quantity(1).build();

		ProductCartInfo productInfo = createProductInfo(UUID.randomUUID(), 50);

		given(cartItemRepository.findById(itemId)).willReturn(Optional.of(item));
		given(productService.getProductCartInfos(any())).willReturn(List.of(productInfo));

		// when
		cartService.updateItemQuantity(userId, itemId, 5);

		// then
		assertThat(item.getQuantity()).isEqualTo(5);
	}

	@Test
	@DisplayName("장바구니 아이템 단건 삭제 성공")
	void deleteCartItem() {
		// given
		UUID userId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		Cart cart = new Cart(userId);
		CartItem item = CartItem.builder().cart(cart).build();

		given(cartItemRepository.findById(itemId)).willReturn(Optional.of(item));

		// when
		cartService.deleteCartItem(userId, itemId);

		// then
		verify(cartItemRepository, times(1)).delete(item);
	}

	@Test
	@DisplayName("타인의 장바구니 아이템을 삭제하려 하면 예외 발생")
	void deleteCartItem_AccessDenied() {
		// given
		UUID myId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();

		Cart otherCart = new Cart(otherId); // 다른 사람 카트
		CartItem item = CartItem.builder().cart(otherCart).build();

		given(cartItemRepository.findById(itemId)).willReturn(Optional.of(item));

		// when & then
		assertThatThrownBy(() -> cartService.deleteCartItem(myId, itemId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
	}

	@Test
	@DisplayName("주문 완료 후 장바구니 아이템 일괄 삭제 성공")
	void removeCartItems() {
		// given
		UUID userId = UUID.randomUUID();
		Cart cart = new Cart(userId);

		CartItem item1 = CartItem.builder().cart(cart).build();
		CartItem item2 = CartItem.builder().cart(cart).build();
		List<UUID> itemIds = List.of(UUID.randomUUID(), UUID.randomUUID());
		List<CartItem> items = List.of(item1, item2);

		given(cartItemRepository.findAllById(itemIds)).willReturn(items);

		// when
		cartService.removeCartItems(userId, itemIds);

		// then
		verify(cartItemRepository, times(1)).deleteAll(items);
	}

	// --- Helper Method ---
	private ProductCartInfo createProductInfo(UUID productId, int stock) {
		// ProductCartInfo는 @Builder나 생성자가 있다고 가정
		return ProductCartInfo.builder()
			.productId(productId)
			.productName("Test Product")
			.price(1000L)
			.stockQuantity(stock)
			.isAvailable(true)
			.build();
	}
}
