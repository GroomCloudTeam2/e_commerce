package com.groom.e_commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.support.IntegrationTestSupport;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductSuspendDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductSearchDtoV1;
import com.groom.e_commerce.user.application.service.UserServiceV1;

@Transactional
class ProductManagerIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ProductServiceV1 productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@MockBean
	private UserServiceV1 userService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID managerId;
	private Category category;

	@BeforeEach
	void setUp() {
		managerId = UUID.randomUUID();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(managerId);

		category = Category.builder()
			.name("Manager Test Category")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();
		categoryRepository.save(category);
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("1. 관리자 상품 목록 조회 (삭제되지 않은 모든 상품)")
	void getAllProductsForManager() {
		// given
		createProduct("Normal Product", ProductStatus.ON_SALE);
		createProduct("Suspended Product", ProductStatus.SUSPENDED);
		createProduct("Hidden Product", ProductStatus.HIDDEN);
		Product deleted = createProduct("Deleted Product", ProductStatus.ON_SALE);
		deleted.softDelete(managerId.toString());
		productRepository.save(deleted);

		// when
		Page<ResProductListDtoV1> result = productService.getAllProductsForManager(null, null, PageRequest.of(0, 10));

		// then
		assertThat(result.getTotalElements()).isEqualTo(3); // Deleted 제외
		assertThat(result.getContent()).extracting("title")
			.containsExactlyInAnyOrder("Normal Product", "Suspended Product", "Hidden Product");
	}

	@Test
	@DisplayName("2. 상품 정지 및 정지된 상품 검색 제외 확인")
	void suspendProduct() {
		// given
		Product product = createProduct("Bad Product", ProductStatus.ON_SALE);
		
		ReqProductSuspendDtoV1 suspendReq = ReqProductSuspendDtoV1.builder()
			.reason("Illegal Item")
			.build();

		// when: 정지
		productService.suspendProduct(product.getId(), suspendReq);

		// then: 상태 확인
		Product suspended = productRepository.findById(product.getId()).orElseThrow();
		assertThat(suspended.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
		assertThat(suspended.getSuspendReason()).isEqualTo("Illegal Item");

		// then: 구매자 검색에서 제외 확인
		Page<ResProductSearchDtoV1> searchResult = productService.searchProducts(
			category.getId(), "Bad Product", null, null, null, PageRequest.of(0, 10));
		assertThat(searchResult.getTotalElements()).isEqualTo(0);
	}

	@Test
	@DisplayName("3. 상품 정지 해제")
	void restoreProduct() {
		// given
		Product product = createProduct("Restorable Product", ProductStatus.SUSPENDED);
		
		// when: 복구
		productService.restoreProduct(product.getId());

		// then
		Product restored = productRepository.findById(product.getId()).orElseThrow();
		assertThat(restored.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(restored.getSuspendReason()).isNull();
	}

	private Product createProduct(String title, ProductStatus status) {
		Product product = Product.builder()
			.ownerId(UUID.randomUUID()) // Random Owner
			.category(category)
			.title(title)
			.price(10000L)
			.stockQuantity(100)
			.hasOptions(false)
			.build();
		product.updateStatus(status);
		return productRepository.save(product);
	}
}
