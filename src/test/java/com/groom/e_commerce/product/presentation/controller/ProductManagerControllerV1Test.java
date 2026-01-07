package com.groom.e_commerce.product.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
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
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductSuspendDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;

@WebMvcTest(ProductManagerControllerV1.class)
class ProductManagerControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private ProductServiceV1 productService;
	private UUID productId;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
	}

	@Test
	@DisplayName("전체 상품 조회 (Manager)")
	@WithMockUser(roles = "MANAGER")
	void getAllProducts() throws Exception {
		ResProductListDtoV1 response = ResProductListDtoV1.builder()
			.productId(productId)
			.title("Manager Product")
			.build();

		given(productService.getAllProductsForManager(any(), any(), any(Pageable.class)))
			.willReturn(new PageImpl<>(Collections.singletonList(response)));

		mockMvc.perform(get("/api/v1/manager/products")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].title").value("Manager Product"));
	}

	@Test
	@DisplayName("상품 정지 (Manager)")
	@WithMockUser(roles = "MANAGER")
	void suspendProduct() throws Exception {
		ResProductDtoV1 response = ResProductDtoV1.builder()
			.id(productId)
			.title("Suspended Product")
			.build();

		given(productService.suspendProduct(eq(productId), any(ReqProductSuspendDtoV1.class)))
			.willReturn(response);

		String jsonContent = """
			{
				"reason": "Policy violation"
			}
			""";

		mockMvc.perform(patch("/api/v1/manager/products/{productId}/suspend", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Suspended Product"));
	}

	@Test
	@DisplayName("상품 정지 해제 (Manager)")
	@WithMockUser(roles = "MANAGER")
	void restoreProduct() throws Exception {
		ResProductDtoV1 response = ResProductDtoV1.builder()
			.id(productId)
			.title("Restored Product")
			.build();

		given(productService.restoreProduct(productId))
			.willReturn(response);

		mockMvc.perform(patch("/api/v1/manager/products/{productId}/restore", productId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Restored Product"));
	}

	@TestConfiguration
	@EnableMethodSecurity(prePostEnabled = true)
	static class SecurityTestConfig {
	}
}
