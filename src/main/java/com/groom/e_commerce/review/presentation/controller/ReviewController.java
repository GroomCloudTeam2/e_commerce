package com.groom.e_commerce.review.presentation.controller;

import com.groom.e_commerce.global.security.AuthenticatedUser;
import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	// 리뷰 작성
	@PostMapping("/{orderId}/items/{productId}")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse createReview(
		@PathVariable UUID orderId,
		@PathVariable UUID productId,
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestBody CreateReviewRequest request
	) {
		return reviewService.createReview(orderId, productId, user.getUserId(), request);
	}

	// 개별 리뷰 상세 조회
	@GetMapping("/{orderId}/items/{productId}/review")
	public ReviewResponse getReview(
		@PathVariable UUID orderId,
		@PathVariable UUID productId,
		@AuthenticationPrincipal AuthenticatedUser user
	) {

		return reviewService.getReview(orderId, productId, user.getUserId());
	}

	// 리뷰 수정
	@PutMapping("/{orderId}/items/{productId}/review")
	public ReviewResponse updateReview(
		@PathVariable UUID orderId,
		@PathVariable UUID productId,
		@AuthenticationPrincipal AuthenticatedUser user, // 인터페이스로 변경
		@RequestBody UpdateReviewRequest request
	) {
		return reviewService.updateReview(orderId, productId, user.getUserId(), request);
	}

	// 리뷰 삭제
	@DeleteMapping("/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReview(
		@PathVariable UUID reviewId,
		@AuthenticationPrincipal AuthenticatedUser user // 인터페이스로 변경
	) {
		reviewService.deleteReview(reviewId, user.getUserId());
	}
}
