package com.groom.e_commerce.product.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.global.infrastructure.config.security.JwtUtil;
import com.groom.e_commerce.product.application.service.ProductOptionServiceV1;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.service.ProductVariantServiceV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqOptionUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResOptionDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResVariantDtoV1;

@WebMvcTest(ProductControllerV1.class)
class ProductControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private ProductServiceV1 productService;

	@MockitoBean
	private ProductOptionServiceV1 productOptionService;

	@MockitoBean
	private ProductVariantServiceV1 productVariantService;
	private UUID productId;
	private UUID categoryId;
	private UUID variantId;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
		categoryId = UUID.randomUUID();
		variantId = UUID.randomUUID();
	}

	@Test
	@DisplayName("상품 등록")
	@WithMockUser(roles = "OWNER")
	void createProduct() throws Exception {
		ResProductCreateDtoV1 response = ResProductCreateDtoV1.builder()
			.productId(productId)
			.message("상품이 등록되었습니다.")
			.build();

		given(productService.createProduct(any(ReqProductCreateDtoV1.class)))
			.willReturn(response);

		String jsonContent = """
			{
				"categoryId": "%s",
				"title": "New Product",
				"description": "New Description",
				"price": 30000,
				"stockQuantity": 100,
				"hasOptions": false
			}
			""".formatted(categoryId);

		mockMvc.perform(post("/api/v1/owner/products")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.productId").value(productId.toString()));
	}

	@Test
	@DisplayName("내 상품 목록 조회")
	@WithMockUser(roles = "OWNER")
	void getSellerProducts() throws Exception {
		ResProductListDtoV1 listDto = ResProductListDtoV1.builder()
			.productId(productId)
			.title("My Product")
			.build();

		given(productService.getSellerProducts(any(), any(), any(Pageable.class)))
			.willReturn(new PageImpl<>(Collections.singletonList(listDto)));

		mockMvc.perform(get("/api/v1/owner/products/owner")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].title").value("My Product"));
	}

	@Test
	@DisplayName("상품 수정")
	@WithMockUser(roles = "OWNER")
	void updateProduct() throws Exception {
		ResProductDtoV1 response = ResProductDtoV1.builder()
			.id(productId)
			.title("Updated Product")
			.price(BigDecimal.valueOf(25000))
			.build();

		given(productService.updateProduct(eq(productId), any(ReqProductUpdateDtoV1.class)))
			.willReturn(response);

		String jsonContent = """
			{
				"title": "Updated Product",
				"price": 25000
			}
			""";

		mockMvc.perform(patch("/api/v1/owner/products/{productId}", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Updated Product"))
			.andExpect(jsonPath("$.price").value(25000));
	}

	@Test
	@DisplayName("상품 삭제")
	@WithMockUser(roles = "OWNER")
	void deleteProduct() throws Exception {
		doNothing().when(productService).deleteProduct(productId);

		mockMvc.perform(delete("/api/v1/owner/products/{productId}", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("권한 없는 사용자 접근 시 403")
	@WithMockUser(roles = "USER")
	void createProduct_forbidden() throws Exception {
		String jsonContent = """
			{
				"categoryId": "%s",
				"title": "New Product",
				"description": "New Description",
				"price": 30000,
				"stockQuantity": 100,
				"hasOptions": false
			}
			""".formatted(categoryId);

		mockMvc.perform(post("/api/v1/owner/products")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("옵션 전체 수정")
	@WithMockUser(roles = "OWNER")
	void updateOptions() throws Exception {
		ResOptionDtoV1 optionDto = ResOptionDtoV1.builder()
			.optionId(UUID.randomUUID())
			.name("Color")
			.build();

		given(productOptionService.updateOptions(eq(productId), any(ReqOptionUpdateDtoV1.class)))
			.willReturn(List.of(optionDto));

		String jsonContent = """
			{
				"options": [
					{
						"name": "Color",
						"values": [
							{"value": "Red"},
							{"value": "Blue"}
						]
					}
				]
			}
			""";

		mockMvc.perform(put("/api/v1/owner/products/{productId}/options", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Color"));
	}

	@Test
	@DisplayName("옵션 목록 조회")
	@WithMockUser(roles = "OWNER")
	void getOptions() throws Exception {
		ResOptionDtoV1 optionDto = ResOptionDtoV1.builder()
			.optionId(UUID.randomUUID())
			.name("Color")
			.build();

		given(productOptionService.getOptions(productId))
			.willReturn(List.of(optionDto));

		mockMvc.perform(get("/api/v1/owner/products/{productId}/options", productId)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Color"));
	}

	@Test
	@DisplayName("SKU 추가")
	@WithMockUser(roles = "OWNER")
	void createVariant() throws Exception {
		ResVariantDtoV1 response = ResVariantDtoV1.builder()
			.variantId(variantId)
			.skuCode("TEST-SKU")
			.build();

		given(productVariantService.createVariant(eq(productId), any(ReqVariantCreateDtoV1.class)))
			.willReturn(response);

		String jsonContent = """
			{
				"price": 10000,
				"stockQuantity": 50,
				"skuCode": "TEST-SKU"
			}
			""";

		mockMvc.perform(post("/api/v1/owner/products/{productId}/variants", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.skuCode").value("TEST-SKU"));
	}

	@Test
	@DisplayName("SKU 목록 조회")
	@WithMockUser(roles = "OWNER")
	void getVariants() throws Exception {
		ResVariantDtoV1 response = ResVariantDtoV1.builder()
			.variantId(variantId)
			.skuCode("TEST-SKU")
			.build();

		given(productVariantService.getVariants(productId))
			.willReturn(List.of(response));

		mockMvc.perform(get("/api/v1/owner/products/{productId}/variants", productId)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].skuCode").value("TEST-SKU"));
	}

	@Test
	@DisplayName("SKU 수정")
	@WithMockUser(roles = "OWNER")
	void updateVariant() throws Exception {
		ResVariantDtoV1 response = ResVariantDtoV1.builder()
			.variantId(variantId)
			.skuCode("UPDATED-SKU")
			.build();

		given(productVariantService.updateVariant(eq(productId), eq(variantId), any(ReqVariantUpdateDtoV1.class)))
			.willReturn(response);

		String jsonContent = """
			{
				"skuCode": "UPDATED-SKU"
			}
			""";

		mockMvc.perform(patch("/api/v1/owner/products/{productId}/variants/{variantId}", productId, variantId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.skuCode").value("UPDATED-SKU"));
	}

	@Test
	@DisplayName("SKU 삭제")
	@WithMockUser(roles = "OWNER")
	void deleteVariant() throws Exception {
		doNothing().when(productVariantService).deleteVariant(productId, variantId);

		mockMvc.perform(delete("/api/v1/owner/products/{productId}/variants/{variantId}", productId, variantId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isNoContent());
	}

	@TestConfiguration
	@EnableMethodSecurity(prePostEnabled = true)
	static class SecurityTestConfig {
	}
}
