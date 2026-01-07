package com.groom.e_commerce.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;

@ExtendWith(MockitoExtension.class)
class ProductSuspensionScenarioTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductQueryRepository productQueryRepository;

	@InjectMocks
	private ProductServiceV1 productService;

	private Product product;
	private UUID productId;
	private UUID ownerId;
	private MockedStatic<SecurityUtil> securityUtilMock;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
		ownerId = UUID.randomUUID();
		Category category = Category.builder().name("Test Category").build();

		product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Test Product")
			.price(Long.valueOf(10000))
			.stockQuantity(10)
			.build();

		securityUtilMock = mockStatic(SecurityUtil.class);
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("시나리오 1: 정지된 상품은 구매자가 조회할 수 없다 (상세 조회)")
	void buyer_cannot_view_suspended_product() {
		// given
		product.suspend("Violation"); // 상품 정지
		given(productRepository.findByIdWithCategory(productId)).willReturn(Optional.of(product));

		// when & then
		assertThatThrownBy(() -> productService.getProductDetail(productId))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE));
	}

	@Test
	@DisplayName("시나리오 2: 정지된 상품이라도 판매자는 목록에서 조회할 수 있다")
	void seller_can_view_suspended_product() {
		// given
		product.suspend("Violation"); // 상품 정지
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);
		
		Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
		given(productQueryRepository.findSellerProducts(eq(ownerId), any(), any(), any()))
			.willReturn(productPage);

		// when
		Page<ResProductListDtoV1> result = productService.getSellerProducts(null, null, PageRequest.of(0, 10));

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getStatus()).isEqualTo(ProductStatus.SUSPENDED);
	}

	@Test
	@DisplayName("시나리오 3: 정지 해제 후에는 다시 구매자가 정상적으로 조회할 수 있다")
	void restored_product_is_accessible_to_buyer() {
		// given
		product.suspend("Violation"); // 먼저 정지
		product.restore(); // 정지 해제 (상태가 ON_SALE로 변경됨)
		
		given(productRepository.findByIdWithCategory(productId)).willReturn(Optional.of(product));
		
		// when
		ResProductDetailDtoV1 result = productService.getProductDetail(productId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(result.getTitle()).isEqualTo("Test Product");
	}
}
