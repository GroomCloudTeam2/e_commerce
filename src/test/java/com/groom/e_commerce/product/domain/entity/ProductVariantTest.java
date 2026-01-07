package com.groom.e_commerce.product.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.domain.enums.VariantStatus;

class ProductVariantTest {

	@Nested
	@DisplayName("decreaseStock")
	class DecreaseStockTest {

		@Test
		@DisplayName("재고 감소 성공 - 재고가 충분한 경우")
		void decreaseStock_success() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(10)
				.build();

			// when
			variant.decreaseStock(5);

			// then
			assertThat(variant.getStockQuantity()).isEqualTo(5);
			assertThat(variant.getStatus()).isEqualTo(VariantStatus.ON_SALE);
		}

		@Test
		@DisplayName("재고가 0이 되면 SOLD_OUT 상태로 변경")
		void decreaseStock_soldOut() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(10)
				.build();

			// when
			variant.decreaseStock(10);

			// then
			assertThat(variant.getStockQuantity()).isEqualTo(0);
			assertThat(variant.getStatus()).isEqualTo(VariantStatus.SOLD_OUT);
		}

		@Test
		@DisplayName("재고 부족 시 예외 발생")
		void decreaseStock_fail() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(5)
				.build();

			// when & then
			assertThatThrownBy(() -> variant.decreaseStock(10))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH));
		}
	}

	@Nested
	@DisplayName("increaseStock")
	class IncreaseStockTest {

		@Test
		@DisplayName("재고 증가 성공")
		void increaseStock_success() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(5)
				.build();

			// when
			variant.increaseStock(10);

			// then
			assertThat(variant.getStockQuantity()).isEqualTo(15);
		}

		@Test
		@DisplayName("SOLD_OUT 상태에서 재고 증가 시 ON_SALE로 변경")
		void increaseStock_restoreOnSale() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(5)
				.build();
			variant.decreaseStock(5); // SOLD_OUT 상태로 만들기
			assertThat(variant.getStatus()).isEqualTo(VariantStatus.SOLD_OUT);

			// when
			variant.increaseStock(10);

			// then
			assertThat(variant.getStockQuantity()).isEqualTo(10);
			assertThat(variant.getStatus()).isEqualTo(VariantStatus.ON_SALE);
		}
	}

	@Nested
	@DisplayName("discontinue")
	class DiscontinueTest {

		@Test
		@DisplayName("단종 처리 성공")
		void discontinue_success() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.price(10000L)
				.stockQuantity(10)
				.build();

			// when
			variant.discontinue();

			// then
			assertThat(variant.getStatus()).isEqualTo(VariantStatus.DISCONTINUED);
		}
	}

	@Nested
	@DisplayName("update")
	class UpdateTest {

		@Test
		@DisplayName("옵션명, 가격, 재고 수정 성공")
		void update_success() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.optionName("빨강/M")
				.price(10000L)
				.stockQuantity(10)
				.build();

			// when
			variant.update("파랑/L", 15000L, 20);

			// then
			assertThat(variant.getOptionName()).isEqualTo("파랑/L");
			assertThat(variant.getPrice()).isEqualTo(15000L);
			assertThat(variant.getStockQuantity()).isEqualTo(20);
		}

		@Test
		@DisplayName("null 값은 무시하고 기존 값 유지")
		void update_ignoreNull() {
			// given
			ProductVariant variant = ProductVariant.builder()
				.skuCode("SKU-001")
				.optionName("빨강/M")
				.price(10000L)
				.stockQuantity(10)
				.build();

			// when
			variant.update(null, null, null);

			// then
			assertThat(variant.getOptionName()).isEqualTo("빨강/M");
			assertThat(variant.getPrice()).isEqualTo(10000L);
			assertThat(variant.getStockQuantity()).isEqualTo(10);
		}
	}
}
