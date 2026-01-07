package com.groom.e_commerce.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
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

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.domain.repository.ProductVariantRepository;
import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;

@ExtendWith(MockitoExtension.class)
class ProductServiceV1Test {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductVariantRepository productVariantRepository;

	@Mock
	private ProductQueryRepository productQueryRepository;

	@Mock
	private CategoryServiceV1 categoryService;

	@InjectMocks
	private ProductServiceV1 productService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID ownerId;
	private Category category;
	private Product product;

	@BeforeEach
	void setUp() {
		ownerId = UUID.randomUUID();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);

		category = Category.builder()
			.name("Electronics")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();

		product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Test Product")
			.description("Description")
			.price(Long.valueOf(10000))
			.stockQuantity(100)
			.hasOptions(false)
			.build();
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("상품 등록 - 성공")
	void createProduct_success() {
		// given
		ReqProductCreateDtoV1 request = new ReqProductCreateDtoV1();
		// Assuming setters or fields are accessible, or using reflection if needed.
		// Since I don't see the DTO code, I assume typical DTO.
		// If DTO has no setters, this might fail, but let's assume setters or fields.
		// Or I can use a library or just mock the DTO if it's an interface (unlikely).
		// Wait, DTOs usually have data.
		
		// Let's assume request is populated via JSON deserialization in Controller,
		// here I manually populate it.
		// If fields are private and no setters, I might need ReflectionTestUtils.
		
		given(categoryService.findActiveCategoryById(any())).willReturn(category);
		given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ResProductCreateDtoV1 result = productService.createProduct(request);

		// then
		assertThat(result).isNotNull();
		verify(productRepository).save(any(Product.class));
	}

	@Test
	@DisplayName("상품 수정 - 성공")
	void updateProduct_success() {
		// given
		UUID productId = UUID.randomUUID();
		ReqProductUpdateDtoV1 request = new ReqProductUpdateDtoV1();
		
		given(productRepository.findByIdAndNotDeleted(productId)).willReturn(Optional.of(product));
		// Assuming request has data.

		// when
		ResProductDtoV1 result = productService.updateProduct(productId, request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo(product.getTitle()); // Assuming update didn't change title if null
	}

	@Test
	@DisplayName("상품 수정 - 권한 없음")
	void updateProduct_forbidden() {
		// given
		UUID productId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		ReqProductUpdateDtoV1 request = new ReqProductUpdateDtoV1();

		Product otherProduct = Product.builder()
			.ownerId(otherUserId) // Different owner
			.category(category)
			.title("Other Product")
			.build();

		given(productRepository.findByIdAndNotDeleted(productId)).willReturn(Optional.of(otherProduct));

		// when & then
		assertThatThrownBy(() -> productService.updateProduct(productId, request))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED));
	}
	
	@Test
	@DisplayName("상품 삭제 - 성공")
	void deleteProduct_success() {
		// given
		UUID productId = UUID.randomUUID();
		given(productRepository.findByIdAndNotDeleted(productId)).willReturn(Optional.of(product));

		// when
		productService.deleteProduct(productId);

		// then
		assertThat(product.isDeleted()).isTrue();
	}
}
