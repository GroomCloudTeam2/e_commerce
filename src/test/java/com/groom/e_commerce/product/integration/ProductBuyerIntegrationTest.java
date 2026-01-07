package com.groom.e_commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
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
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.ProductSortType;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductSearchDtoV1;
import com.groom.e_commerce.user.application.service.UserServiceV1;

@Transactional
class ProductBuyerIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ProductServiceV1 productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@MockBean
	private UserServiceV1 userService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID ownerId;
	private Category category;

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
		categoryRepository.save(category);
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("1. 상품 목록 검색/필터링")
	void searchProducts() {
		// given
		createProduct("iPhone 13", 1000000L, ProductStatus.ON_SALE);
		createProduct("iPhone 14", 1200000L, ProductStatus.ON_SALE);
		createProduct("Galaxy S23", 1100000L, ProductStatus.ON_SALE);
		createProduct("Hidden Phone", 500000L, ProductStatus.HIDDEN);

		// when: 검색어 + 가격 필터 + 정렬
		Page<ResProductSearchDtoV1> result = productService.searchProducts(
			category.getId(),
			"iPhone",
			900000L,
			1300000L,
			ProductSortType.PRICE_ASC,
			PageRequest.of(0, 10)
		);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).extracting("title")
			.containsExactly("iPhone 13", "iPhone 14"); // 가격 낮은순
	}

	@Test
	@DisplayName("2. 상품 상세 조회")
	void getProductDetail() {
		// given
		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Detail Test Product")
			.price(5000L)
			.stockQuantity(10)
			.hasOptions(false)
			.build();
		productRepository.save(product);

		// when
		ResProductDetailDtoV1 detail = productService.getProductDetail(product.getId());

		// then
		assertThat(detail.getTitle()).isEqualTo("Detail Test Product");
		assertThat(detail.getPrice()).isEqualTo(5000L);
	}

	@Test
	@DisplayName("3. 장바구니 담기 전 조회 (getProductCartInfos)")
	void getProductCartInfos() {
		// given
		Product p1 = createProduct("P1", 1000L, ProductStatus.ON_SALE);
		
		Product p2 = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("P2 Option")
			.hasOptions(true)
			.build();
		productRepository.save(p2);
		
		ProductVariant variant = ProductVariant.builder()
			.product(p2)
			.skuCode("SKU-CART-TEST")
			.price(2000L)
			.stockQuantity(10)
			.build();
		p2.addVariant(variant);
		Product savedP2 = productRepository.save(p2);
		ProductVariant savedVariant = savedP2.getVariants().get(0);

		// when
		List<StockManagement> items = List.of(
			StockManagement.of(p1.getId(), null, 1),
			StockManagement.of(savedP2.getId(), savedVariant.getId(), 1)
		);
		
		List<ProductCartInfo> infos = productService.getProductCartInfos(items);

		// then
		assertThat(infos).hasSize(2);
		
		ProductCartInfo p1Info = infos.stream()
			.filter(i -> i.getProductName().equals("P1"))
			.findFirst().orElseThrow();
		assertThat(p1Info.getPrice()).isEqualTo(1000L);

		ProductCartInfo p2Info = infos.stream()
			.filter(i -> i.getProductName().equals("P2 Option"))
			.findFirst().orElseThrow();
		assertThat(p2Info.getPrice()).isEqualTo(2000L);
		
		assertThat(infos).extracting("isAvailable")
			.containsOnly(true);
	}

	private Product createProduct(String title, Long price, ProductStatus status) {
		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title(title)
			.price(price)
			.stockQuantity(100)
			.hasOptions(false)
			.build();
		product.updateStatus(status);
		return productRepository.save(product);
	}
}