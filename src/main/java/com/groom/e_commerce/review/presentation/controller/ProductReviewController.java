package com.groom.e_commerce.review.presentation.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.response.ProductReviewResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ProductReviewController {

	private final ReviewService reviewService;

	@Operation(summary = "상품 리뷰 목록 + ai 분석 리뷰")
	@GetMapping("/product/{productId}/")
	public ProductReviewResponse getProductReviews(
		@PathVariable UUID productId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "latest") String sort
	) {

		return reviewService.getProductReviews(productId, page, size, sort);
	}

}
