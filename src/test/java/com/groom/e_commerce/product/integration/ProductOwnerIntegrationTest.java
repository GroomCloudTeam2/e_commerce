package com.groom.e_commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.support.IntegrationTestSupport;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.application.service.ProductOptionServiceV1;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.service.ProductVariantServiceV1;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqOptionUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1.OptionRequest;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1.OptionValueRequest;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1.VariantRequest;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResOptionDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResVariantDtoV1;
import com.groom.e_commerce.user.application.service.UserServiceV1;

@Transactional
class ProductOwnerIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ProductServiceV1 productService;

	@Autowired
	private ProductOptionServiceV1 optionService;

	@Autowired
	private ProductVariantServiceV1 variantService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@MockBean
	private UserServiceV1 userService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID ownerId;
	private UUID categoryId;

	@BeforeEach
	void setUp() {
		ownerId = UUID.randomUUID();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);

		Category category = Category.builder()
			.name("Test Category")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();
		categoryRepository.save(category);
		categoryId = category.getId();
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("1. 상품 등록 (옵션 없음)")
	void createProduct_noOptions() {
		// given
		ReqProductCreateDtoV1 request = ReqProductCreateDtoV1.builder()
			.categoryId(categoryId)
			.title("Simple Product")
			.price(10000L)
			.stockQuantity(100)
			.hasOptions(false)
			.build();

		// when
		ResProductCreateDtoV1 created = productService.createProduct(request);

		// then
		Product product = productRepository.findById(created.getProductId()).orElseThrow();
		assertThat(product.getTitle()).isEqualTo("Simple Product");
		assertThat(product.getPrice()).isEqualTo(10000L);
		assertThat(product.getStockQuantity()).isEqualTo(100);
		assertThat(product.getHasOptions()).isFalse();
	}

	@Test
	@DisplayName("2. 상품 등록 (옵션 있음) & SKU 중복 예외")
	void createProduct_withOptions() {
		// given
		OptionValueRequest red = OptionValueRequest.builder().value("Red").build();
		OptionRequest colorOption = OptionRequest.builder().name("Color").values(List.of(red)).build();
		
		VariantRequest variant = VariantRequest.builder()
			.optionValueIndexes(List.of(0))
			.skuCode("SKU-TEST-1")
			.price(12000L)
			.stockQuantity(50)
			.build();

		ReqProductCreateDtoV1 request = ReqProductCreateDtoV1.builder()
			.categoryId(categoryId)
			.title("Option Product")
			.hasOptions(true)
			.options(List.of(colorOption))
			.variants(List.of(variant))
			.build();

		// when
		ResProductCreateDtoV1 created = productService.createProduct(request);

		// then
		Product product = productRepository.findByIdWithVariants(created.getProductId()).orElseThrow();
		assertThat(product.getOptions()).hasSize(1);
		assertThat(product.getVariants()).hasSize(1);
		assertThat(product.getVariants().get(0).getSkuCode()).isEqualTo("SKU-TEST-1");

		// SKU 중복 테스트
		assertThatThrownBy(() -> productService.createProduct(request))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SKU_CODE));
	}

	@Test
	@DisplayName("3. 내 상품 목록 조회")
	void getSellerProducts() {
		// given
		createProduct("My Product 1");
		createProduct("My Product 2");
		
		// 다른 사람 상품
		securityUtilMock.close(); // 잠시 Mock 해제
		securityUtilMock = mockStatic(SecurityUtil.class); // 다시 Mocking
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(UUID.randomUUID());
		
		ReqProductCreateDtoV1 otherReq = ReqProductCreateDtoV1.builder()
			.categoryId(categoryId)
			.title("Other Product")
			.price(10L)
			.stockQuantity(10)
			.hasOptions(false)
			.build();
		productService.createProduct(otherReq);
		
		// 다시 내 ID로 복구
		securityUtilMock.close();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);

		// when
		Page<ResProductListDtoV1> result = productService.getSellerProducts(null, null, PageRequest.of(0, 10));

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).extracting("title")
			.containsExactlyInAnyOrder("My Product 1", "My Product 2");
	}

	@Test
	@DisplayName("4. 상품 수정 및 삭제 (권한 검증 포함)")
	void updateAndDeleteProduct() {
		// given
		ResProductCreateDtoV1 created = createProduct("Original Title");
		UUID productId = created.getProductId();

		// 수정
		ReqProductUpdateDtoV1 updateReq = ReqProductUpdateDtoV1.builder()
			.title("Updated Title")
			.price(20000L)
			.build();
		
		productService.updateProduct(productId, updateReq);
		Product updated = productRepository.findById(productId).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo("Updated Title");

		// 권한 검증 (다른 유저)
		securityUtilMock.close();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(UUID.randomUUID());
		
		assertThatThrownBy(() -> productService.deleteProduct(productId))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED));
			
		// 다시 내 ID로 삭제
		securityUtilMock.close();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);
		
		productService.deleteProduct(productId);
		
		// 삭제 확인
		assertThatThrownBy(() -> productService.findProductById(productId))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
	}

	@Test
	@DisplayName("5. 옵션 전체 수정 (PUT)")
	void updateOptions() {
		// given
		ResProductCreateDtoV1 created = createProduct("Option Update Test");
		UUID productId = created.getProductId();

		ReqOptionUpdateDtoV1.OptionValueRequest blue = ReqOptionUpdateDtoV1.OptionValueRequest.builder()
			.value("Blue")
			.build();
		ReqOptionUpdateDtoV1.OptionRequest color = ReqOptionUpdateDtoV1.OptionRequest.builder()
			.name("Color")
			.values(List.of(blue))
			.build();
		ReqOptionUpdateDtoV1 request = ReqOptionUpdateDtoV1.builder()
			.options(List.of(color))
			.build();

		// when
		List<ResOptionDtoV1> result = optionService.updateOptions(productId, request);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Color");
		assertThat(result.get(0).getValues().get(0).getValue()).isEqualTo("Blue");
	}

	@Test
	@DisplayName("6. SKU 관리 (추가, 수정, 삭제)")
	void manageVariants() {
		// given
		ResProductCreateDtoV1 created = createProduct("Variant Test");
		UUID productId = created.getProductId();
		
		// 옵션 먼저 생성 (SKU는 옵션값 ID 필요)
		ReqOptionUpdateDtoV1.OptionValueRequest s = ReqOptionUpdateDtoV1.OptionValueRequest.builder().value("S").build();
		ReqOptionUpdateDtoV1.OptionRequest size = ReqOptionUpdateDtoV1.OptionRequest.builder().name("Size").values(List.of(s)).build();
		List<ResOptionDtoV1> options = optionService.updateOptions(productId, ReqOptionUpdateDtoV1.builder().options(List.of(size)).build());
		UUID optionValueId = options.get(0).getValues().get(0).getOptionValueId();

		// 1. SKU 추가
		ReqVariantCreateDtoV1 createReq = ReqVariantCreateDtoV1.builder()
			.skuCode("SKU-VAR-1")
			.price(15000L)
			.stockQuantity(10)
			.optionValueIds(List.of(optionValueId))
			.build();
		ResVariantDtoV1 createdVariant = variantService.createVariant(productId, createReq);
		assertThat(createdVariant.getSkuCode()).isEqualTo("SKU-VAR-1");
		assertThat(createdVariant.getOptionName()).isEqualTo("S");

		// 2. SKU 수정
		ReqVariantUpdateDtoV1 updateReq = ReqVariantUpdateDtoV1.builder()
			.price(18000L)
			.stockQuantity(20)
			.build();
		ResVariantDtoV1 updatedVariant = variantService.updateVariant(productId, createdVariant.getVariantId(), updateReq);
		assertThat(updatedVariant.getPrice()).isEqualTo(18000L);
		assertThat(updatedVariant.getStockQuantity()).isEqualTo(20);

		// 3. SKU 삭제
		variantService.deleteVariant(productId, createdVariant.getVariantId());
		assertThat(variantService.getVariants(productId)).isEmpty();
	}

	private ResProductCreateDtoV1 createProduct(String title) {
		ReqProductCreateDtoV1 request = ReqProductCreateDtoV1.builder()
			.categoryId(categoryId)
			.title(title)
			.price(10000L)
			.stockQuantity(100)
			.hasOptions(false)
			.build();
		return productService.createProduct(request);
	}
}
