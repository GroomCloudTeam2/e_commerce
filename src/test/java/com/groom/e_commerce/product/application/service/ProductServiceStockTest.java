package com.groom.e_commerce.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.domain.repository.ProductVariantRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceStockTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductVariantRepository productVariantRepository;

	@InjectMocks
	private ProductServiceV1 productService;

	@Test
	@DisplayName("재고 차감 - 단일 상품 성공")
	void decreaseStock_product_success() {
		// given
		UUID productId = UUID.randomUUID();
		int quantity = 5;
		Product product = Product.builder()
			.title("Test")
			.price(BigDecimal.TEN)
			.stockQuantity(10)
			.hasOptions(false)
			.build();

		given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

		// when
		productService.decreaseStock(productId, null, quantity);

		// then
		assertThat(product.getStockQuantity()).isEqualTo(5);
	}

	@Test
	@DisplayName("재고 차감 - 옵션 상품 성공")
	void decreaseStock_variant_success() {
		// given
		UUID productId = UUID.randomUUID();
		UUID variantId = UUID.randomUUID();
		int quantity = 3;
		
		Product product = Product.builder().hasOptions(true).build();
		ProductVariant variant = ProductVariant.builder()
			.product(product)
			.stockQuantity(10)
			.build();

		given(productVariantRepository.findByIdAndProductIdWithLock(variantId, productId))
			.willReturn(Optional.of(variant));

		// when
		productService.decreaseStock(productId, variantId, quantity);

		// then
		assertThat(variant.getStockQuantity()).isEqualTo(7);
	}

	@Test
	@DisplayName("재고 차감 - 재고 부족 예외")
	void decreaseStock_notEnough() {
		// given
		UUID productId = UUID.randomUUID();
		int quantity = 20;
		Product product = Product.builder()
			.title("Test")
			.stockQuantity(10)
			.hasOptions(false)
			.build();

		given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

		// when & then
		assertThatThrownBy(() -> productService.decreaseStock(productId, null, quantity))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH));
	}

	@Test
	@DisplayName("재고 차감 - Bulk 성공")
	void decreaseStockBulk_success() {
		// given
		UUID p1 = UUID.randomUUID();
		UUID p2 = UUID.randomUUID();
		
		Product prod1 = Product.builder().stockQuantity(10).hasOptions(false).build();
		Product prod2 = Product.builder().stockQuantity(10).hasOptions(false).build();

		StockManagement item1 = StockManagement.of(p1, null, 2);
		StockManagement item2 = StockManagement.of(p2, null, 3);
		List<StockManagement> items = List.of(item1, item2);

		given(productRepository.findByIdWithLock(p1)).willReturn(Optional.of(prod1));
		given(productRepository.findByIdWithLock(p2)).willReturn(Optional.of(prod2));

		// when
		productService.decreaseStockBulk(items);

		// then
		assertThat(prod1.getStockQuantity()).isEqualTo(8);
		assertThat(prod2.getStockQuantity()).isEqualTo(7);
	}
	
	@Test
	@DisplayName("재고 복원 - 단일 상품 성공")
	void increaseStock_product_success() {
		// given
		UUID productId = UUID.randomUUID();
		int quantity = 5;
		Product product = Product.builder()
			.stockQuantity(10)
			.hasOptions(false)
			.build();

		given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

		// when
		productService.increaseStock(productId, null, quantity);

		// then
		assertThat(product.getStockQuantity()).isEqualTo(15);
	}
}
