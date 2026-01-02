package com.groom.e_commerce.product.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Product", description = "상품 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class ProductControllerV1 {

	private final ProductServiceV1 productService;

	@Operation(summary = "상품 등록", description = "판매자가 새 상품을 등록합니다.")
	@PostMapping
	public ResponseEntity<ResProductDtoV1> createProduct(
		@Valid @RequestBody ReqProductCreateDtoV1 request
	) {
		ResProductDtoV1 response = productService.createProduct(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "내 상품 목록 조회", description = "판매자가 자신의 상품 목록을 조회합니다.")
	@GetMapping("/owner")
	public ResponseEntity<Page<ResProductListDtoV1>> getSellerProducts(
		@RequestParam(required = false) ProductStatus status,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<ResProductListDtoV1> response = productService.getSellerProducts(status, pageable);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "상품 수정", description = "판매자가 자신의 상품을 수정합니다.")
	@PatchMapping("/{productId}")
	public ResponseEntity<ResProductDtoV1> updateProduct(
		@PathVariable UUID productId,
		@Valid @RequestBody ReqProductUpdateDtoV1 request
	) {
		ResProductDtoV1 response = productService.updateProduct(productId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "상품 삭제", description = "판매자가 자신의 상품을 삭제합니다. (Soft Delete)")
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> deleteProduct(
		@PathVariable UUID productId
	) {
		productService.deleteProduct(productId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Void> handleAccessDeniedException(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}
}
