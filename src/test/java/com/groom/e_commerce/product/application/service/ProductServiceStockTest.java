package com.groom.e_commerce.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.enums.VariantStatus;
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

	@Nested
	@DisplayName("장바구니 상품 정보 조회 테스트")
	class GetProductCartInfosTest {

		@Test
		@DisplayName("빈 리스트 입력 시 빈 리스트 반환")
		void getProductCartInfos_emptyList() {
			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(Collections.emptyList());

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("null 입력 시 빈 리스트 반환")
		void getProductCartInfos_null() {
			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(null);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("단일 상품 조회 성공")
		void getProductCartInfos_singleProduct_success() {
			// given
			UUID productId = UUID.randomUUID();
			Product product = Product.builder()
				.title("테스트 상품")
				.thumbnailUrl("http://test.com/img.jpg")
				.price(BigDecimal.valueOf(10000))
				.stockQuantity(100)
				.hasOptions(false)
				.build();
			ReflectionTestUtils.setField(product, "id", productId);
			ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);

			StockManagement item = StockManagement.of(productId, null, 2);

			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(List.of(product));

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).hasSize(1);
			ProductCartInfo info = result.get(0);
			assertThat(info.getProductId()).isEqualTo(productId);
			assertThat(info.getProductName()).isEqualTo("테스트 상품");
			assertThat(info.getPrice()).isEqualTo(BigDecimal.valueOf(10000));
			assertThat(info.getStockQuantity()).isEqualTo(100);
			assertThat(info.isAvailable()).isTrue();
			assertThat(info.getVariantId()).isNull();
		}

		@Test
		@DisplayName("옵션 상품(Variant) 조회 성공")
		void getProductCartInfos_withVariant_success() {
			// given
			UUID productId = UUID.randomUUID();
			UUID variantId = UUID.randomUUID();

			Product product = Product.builder()
				.title("옵션 상품")
				.thumbnailUrl("http://test.com/img.jpg")
				.price(BigDecimal.valueOf(10000))
				.hasOptions(true)
				.build();
			ReflectionTestUtils.setField(product, "id", productId);
			ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);

			ProductVariant variant = ProductVariant.builder()
				.product(product)
				.optionName("Red / L")
				.price(BigDecimal.valueOf(12000))
				.stockQuantity(50)
				.build();
			ReflectionTestUtils.setField(variant, "id", variantId);
			ReflectionTestUtils.setField(variant, "status", VariantStatus.ON_SALE);

			StockManagement item = StockManagement.of(productId, variantId, 1);

			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(List.of(product));
			given(productVariantRepository.findByIdIn(anyList())).willReturn(List.of(variant));

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).hasSize(1);
			ProductCartInfo info = result.get(0);
			assertThat(info.getProductId()).isEqualTo(productId);
			assertThat(info.getVariantId()).isEqualTo(variantId);
			assertThat(info.getOptionName()).isEqualTo("Red / L");
			assertThat(info.getPrice()).isEqualTo(BigDecimal.valueOf(12000));
			assertThat(info.getStockQuantity()).isEqualTo(50);
			assertThat(info.isAvailable()).isTrue();
		}

		@Test
		@DisplayName("삭제된 상품은 결과에서 제외됨")
		void getProductCartInfos_deletedProduct_excluded() {
			// given
			UUID productId = UUID.randomUUID();
			StockManagement item = StockManagement.of(productId, null, 1);

			// 삭제된 상품은 findByIdInAndNotDeleted에서 반환되지 않음
			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(Collections.emptyList());

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("Variant가 다른 Product에 속할 때 스킵")
		void getProductCartInfos_variantBelongsToDifferentProduct_skipped() {
			// given
			UUID productId = UUID.randomUUID();
			UUID otherProductId = UUID.randomUUID();
			UUID variantId = UUID.randomUUID();

			Product product = Product.builder()
				.title("상품 A")
				.hasOptions(true)
				.build();
			ReflectionTestUtils.setField(product, "id", productId);
			ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);

			// 다른 상품에 속한 Variant
			Product otherProduct = Product.builder().title("상품 B").build();
			ReflectionTestUtils.setField(otherProduct, "id", otherProductId);

			ProductVariant variant = ProductVariant.builder()
				.product(otherProduct)  // 다른 상품에 속함!
				.stockQuantity(10)
				.build();
			ReflectionTestUtils.setField(variant, "id", variantId);

			StockManagement item = StockManagement.of(productId, variantId, 1);

			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(List.of(product));
			given(productVariantRepository.findByIdIn(anyList())).willReturn(List.of(variant));

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("판매 중지 상품은 isAvailable = false")
		void getProductCartInfos_suspendedProduct_notAvailable() {
			// given
			UUID productId = UUID.randomUUID();
			Product product = Product.builder()
				.title("판매 중지 상품")
				.price(BigDecimal.valueOf(10000))
				.stockQuantity(100)
				.hasOptions(false)
				.build();
			ReflectionTestUtils.setField(product, "id", productId);
			ReflectionTestUtils.setField(product, "status", ProductStatus.SUSPENDED);

			StockManagement item = StockManagement.of(productId, null, 1);

			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(List.of(product));

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).isAvailable()).isFalse();
		}

		@Test
		@DisplayName("Variant가 DISCONTINUED면 isAvailable = false")
		void getProductCartInfos_discontinuedVariant_notAvailable() {
			// given
			UUID productId = UUID.randomUUID();
			UUID variantId = UUID.randomUUID();

			Product product = Product.builder()
				.title("상품")
				.hasOptions(true)
				.build();
			ReflectionTestUtils.setField(product, "id", productId);
			ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);

			ProductVariant variant = ProductVariant.builder()
				.product(product)
				.price(BigDecimal.valueOf(10000))
				.stockQuantity(50)
				.build();
			ReflectionTestUtils.setField(variant, "id", variantId);
			ReflectionTestUtils.setField(variant, "status", VariantStatus.DISCONTINUED);

			StockManagement item = StockManagement.of(productId, variantId, 1);

			given(productRepository.findByIdInAndNotDeleted(anyList())).willReturn(List.of(product));
			given(productVariantRepository.findByIdIn(anyList())).willReturn(List.of(variant));

			// when
			List<ProductCartInfo> result = productService.getProductCartInfos(List.of(item));

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).isAvailable()).isFalse();
		}
	}
}
