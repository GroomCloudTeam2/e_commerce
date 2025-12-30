package com.groom.e_commerce.product.presentation.controller;

import com.groom.e_commerce.global.common.response.ApiResponse;
import com.groom.e_commerce.product.application.service.CategoryService;
import com.groom.e_commerce.product.presentation.dto.response.CategoryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	/**
	 * 카테고리 목록 조회
	 * - parentId가 없으면 전체 계층 구조 반환
	 * - parentId가 있으면 해당 카테고리의 자식 목록 반환
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
		@RequestParam(required = false) UUID parentId
	) {
		List<CategoryResponse> categories;
		if (parentId == null) {
			categories = categoryService.getAllCategories();
		} else {
			categories = categoryService.getChildCategories(parentId);
		}
		return ResponseEntity.ok(ApiResponse.success(categories));
	}

	/**
	 * 카테고리 상세 조회
	 */
	@GetMapping("/{categoryId}")
	public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
		@PathVariable UUID categoryId
	) {
		CategoryResponse category = categoryService.getCategory(categoryId);
		return ResponseEntity.ok(ApiResponse.success(category));
	}
}
