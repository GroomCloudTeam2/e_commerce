package com.groom.e_commerce.product.presentation.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.domain.enums.ProductSortType;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductSearchDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Product (Public)", description = "상품 공개 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductPublicControllerV1 {

	private final ProductServiceV1 productService;

	@Operation(summary = "상품 목록 조회", description = "구매자가 상품 목록을 조회합니다. (검색, 필터, 정렬 지원)")
	@GetMapping
	public ResponseEntity<Page<ResProductSearchDtoV1>> searchProducts(
		@Parameter(description = "카테고리 ID") @RequestParam(required = false) UUID categoryId,
		@Parameter(description = "검색어 (상품명)") @RequestParam(required = false) String keyword,
		@Parameter(description = "최소 가격") @RequestParam(required = false) BigDecimal minPrice,
		@Parameter(description = "최대 가격") @RequestParam(required = false) BigDecimal maxPrice,
		@Parameter(description = "정렬 (price_asc, price_desc, newest, rating)") @RequestParam(required = false, defaultValue = "newest") String sort,
		@Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(required = false, defaultValue = "1") Integer page,
		@Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") Integer size
	) {
		ProductSortType sortType = ProductSortType.fromValue(sort);
		// 명세에서는 page가 1부터 시작하므로, 0-based로 변환
		Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);

		Page<ResProductSearchDtoV1> response = productService.searchProducts(
			categoryId, keyword, minPrice, maxPrice, sortType, pageable
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "상품 상세 조회", description = "구매자가 상품 상세 정보를 조회합니다.")
	@GetMapping("/{productId}")
	public ResponseEntity<ResProductDetailDtoV1> getProductDetail(
		@Parameter(description = "상품 ID") @PathVariable UUID productId
	) {
		ResProductDetailDtoV1 response = productService.getProductDetail(productId);
		return ResponseEntity.ok(response);
	}
}
