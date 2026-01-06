package com.groom.e_commerce.product.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.e_commerce.product.domain.enums.ProductStatus;

class ProductTest {

	@Nested
	@DisplayName("decreaseStock")
	class DecreaseStockTest {

		@Test
		@DisplayName("재고 감소 성공 - 재고가 충분한 경우")
		void decreaseStock_success() {
			// given
			Product product = Product.builder()
				.stockQuantity(10)
				.price(BigDecimal.valueOf(10000))
				.build();

			// when
			product.decreaseStock(5);

			// then
			assertThat(product.getStockQuantity()).isEqualTo(5);
			assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		}

		@Test
		@DisplayName("재고가 0이 되면 SOLD_OUT 상태로 변경")
		void decreaseStock_soldOut() {
			// given
			Product product = Product.builder()
				.stockQuantity(10)
				.price(BigDecimal.valueOf(10000))
				.build();

			// when
			product.decreaseStock(10);

			// then
			assertThat(product.getStockQuantity()).isEqualTo(0);
			assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
		}

		@Test
		@DisplayName("재고 부족 시 예외 발생")
		void decreaseStock_fail() {
			// given
			Product product = Product.builder().stockQuantity(5).build();

			// when & then
			assertThatThrownBy(() -> product.decreaseStock(10))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("재고가 부족합니다.");
		}
	}

	@Nested
	@DisplayName("suspend")
	class SuspendTest {

		@Test
		@DisplayName("상품 정지 처리 성공")
		void suspend_success() {
			// given
			Product product = Product.builder()
				.ownerId(UUID.randomUUID())
				.title("테스트 상품")
				.stockQuantity(10)
				.build();

			// when
			product.suspend("규정 위반");

			// then
			assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
			assertThat(product.getSuspendReason()).isEqualTo("규정 위반");
			assertThat(product.getSuspendedAt()).isNotNull();
		}
	}

	@Nested
	@DisplayName("restore")
	class RestoreTest {

		@Test
		@DisplayName("정지 해제 성공")
		void restore_success() {
			// given
			Product product = Product.builder()
				.ownerId(UUID.randomUUID())
				.title("테스트 상품")
				.stockQuantity(10)
				.build();
			product.suspend("규정 위반");

			// when
			product.restore();

			// then
			assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
			assertThat(product.getSuspendReason()).isNull();
			assertThat(product.getSuspendedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("softDelete")
	class SoftDeleteTest {

		@Test
		@DisplayName("소프트 삭제 성공")
		void softDelete_success() {
			// given
			UUID deletedBy = UUID.randomUUID();
			Product product = Product.builder()
				.ownerId(UUID.randomUUID())
				.title("테스트 상품")
				.stockQuantity(10)
				.build();

			// when
			product.softDelete(deletedBy);

			// then
			assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
			assertThat(product.isDeleted()).isTrue();
		}
	}

	@Nested
	@DisplayName("isOwnedBy")
	class IsOwnedByTest {

		@Test
		@DisplayName("소유자 확인 - 일치")
		void isOwnedBy_true() {
			// given
			UUID ownerId = UUID.randomUUID();
			Product product = Product.builder()
				.ownerId(ownerId)
				.title("테스트 상품")
				.build();

			// when & then
			assertThat(product.isOwnedBy(ownerId)).isTrue();
		}

		@Test
		@DisplayName("소유자 확인 - 불일치")
		void isOwnedBy_false() {
			// given
			UUID ownerId = UUID.randomUUID();
			UUID otherId = UUID.randomUUID();
			Product product = Product.builder()
				.ownerId(ownerId)
				.title("테스트 상품")
				.build();

			// when & then
			assertThat(product.isOwnedBy(otherId)).isFalse();
		}
	}

	@Nested
	@DisplayName("addOption")
	class AddOptionTest {

		@Test
		@DisplayName("옵션 추가 시 hasOptions가 true로 변경")
		void addOption_success() {
			// given
			Product product = Product.builder()
				.ownerId(UUID.randomUUID())
				.title("테스트 상품")
				.hasOptions(false)
				.build();
			ProductOption option = ProductOption.builder()
				.product(product)
				.name("색상")
				.sortOrder(1)
				.build();

			// when
			product.addOption(option);

			// then
			assertThat(product.getHasOptions()).isTrue();
			assertThat(product.getOptions()).hasSize(1);
		}
	}
}
