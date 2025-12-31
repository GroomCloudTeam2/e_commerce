package com.groom.e_commerce.review.presentation.controller;

import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.response.ProductReviewResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductReviewController {

	private final ReviewService reviewService;

	// 상품별 리뷰 목록 + AI 요약 조회 (페이징 적용)
	@GetMapping("/{productId}/reviews")
	public ProductReviewResponse getProductReviews(
		@PathVariable UUID productId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {

		return reviewService.getProductReviews(productId, page, size);
	}
}
