package com.groom.e_commerce.product.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.product.application.service.CategoryServiceV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Category (Master)", description = "카테고리 관리 API (Master)")
@RestController
@RequestMapping("/api/v1/master/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MASTER')")
public class CategoryMasterControllerV1 {

	private final CategoryServiceV1 categoryService;

	@Operation(summary = "전체 카테고리 조회 (Master)", description = "숨김 처리된 카테고리를 포함하여 전체 계층 구조를 조회합니다.")
	@GetMapping
	public ResponseEntity<List<ResCategoryDtoV1>> getAllCategories() {
		List<ResCategoryDtoV1> categories = categoryService.getAllCategoriesForMaster();
		return ResponseEntity.ok(categories);
	}

	@Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
	@PostMapping
	public ResponseEntity<Void> createCategory(
		@Valid @RequestBody ReqCategoryCreateDtoV1 request
	) {
		categoryService.createCategory(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "카테고리 수정", description = "카테고리 정보를 수정합니다.")
	@PatchMapping("/{categoryId}")
	public ResponseEntity<Void> updateCategory(
		@PathVariable UUID categoryId,
		@Valid @RequestBody ReqCategoryUpdateDtoV1 request
	) {
		categoryService.updateCategory(categoryId, request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다. (하위 카테고리나 상품이 없어야 함)")
	@DeleteMapping("/{categoryId}")
	public ResponseEntity<Void> deleteCategory(
		@PathVariable UUID categoryId
	) {
		categoryService.deleteCategory(categoryId);
		return ResponseEntity.noContent().build();
	}
}
