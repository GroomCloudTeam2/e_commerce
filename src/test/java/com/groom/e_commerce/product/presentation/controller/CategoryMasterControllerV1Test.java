package com.groom.e_commerce.product.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.global.infrastructure.config.security.JwtUtil;
import com.groom.e_commerce.product.application.service.CategoryServiceV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;

@WebMvcTest(CategoryMasterControllerV1.class)
class CategoryMasterControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private JwtUtil jwtUtil;

	@MockBean
	private CategoryServiceV1 categoryService;
	private UUID categoryId;

	@BeforeEach
	void setUp() {
		categoryId = UUID.randomUUID();
	}

	@Test
	@DisplayName("전체 카테고리 조회 (Master)")
	@WithMockUser(roles = "MASTER")
	void getAllCategories() throws Exception {
		ResCategoryDtoV1 response = ResCategoryDtoV1.builder()
			.id(categoryId)
			.name("Master Category")
			.isActive(false)
			.build();

		given(categoryService.getAllCategoriesForMaster())
			.willReturn(Collections.singletonList(response));

		mockMvc.perform(get("/api/v1/master/categories")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("카테고리 생성")
	@WithMockUser(roles = "MASTER")
	void createCategory() throws Exception {
		doNothing().when(categoryService).createCategory(any(ReqCategoryCreateDtoV1.class));

		String jsonContent = """
			{
				"name": "New Category",
				"sortOrder": 1,
				"isActive": true
			}
			""";

		mockMvc.perform(post("/api/v1/master/categories")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("카테고리 수정")
	@WithMockUser(roles = "MASTER")
	void updateCategory() throws Exception {
		doNothing().when(categoryService).updateCategory(eq(categoryId), any(ReqCategoryUpdateDtoV1.class));

		String jsonContent = """
			{
				"name": "Updated Category"
			}
			""";

		mockMvc.perform(patch("/api/v1/master/categories/{categoryId}", categoryId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("카테고리 삭제")
	@WithMockUser(roles = "MASTER")
	void deleteCategory() throws Exception {
		doNothing().when(categoryService).deleteCategory(categoryId);

		mockMvc.perform(delete("/api/v1/master/categories/{categoryId}", categoryId)
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
