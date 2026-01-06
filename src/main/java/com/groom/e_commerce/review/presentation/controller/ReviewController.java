package com.groom.e_commerce.review.presentation.controller;

import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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
		@RequestBody CreateReviewRequest request
	) {
		UUID userId = SecurityUtil.getCurrentUserId();
		return reviewService.createReview(orderId, productId, userId, request);
	}

	// 내 리뷰 조회로 바꾸자.
	@GetMapping("/{reviewId}")
	public ReviewResponse getReview(
		@PathVariable UUID reviewId
	) {
		UUID userId = SecurityUtil.getCurrentUserId();
		return reviewService.getReview(reviewId, userId);
	}

	// 리뷰 수정
	@PutMapping("/reviewId")
	public ReviewResponse updateReview(
		@PathVariable UUID reviewId,
		@RequestBody UpdateReviewRequest request
	) {
		UUID userId = SecurityUtil.getCurrentUserId();
		return reviewService.updateReview(reviewId, userId, request);
	}

	// 리뷰 삭제
	@DeleteMapping("/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReview(@PathVariable UUID reviewId) {
		UUID userId = SecurityUtil.getCurrentUserId();
		reviewService.deleteReview(reviewId, userId);
	}
	// 리뷰 좋아요
	@PostMapping("/{reviewId}/like")
	public int likeReview(@PathVariable UUID reviewId) {
		UUID userId = SecurityUtil.getCurrentUserId();
		return reviewService.likeReview(reviewId, userId);
	}

	// 리뷰 좋아요 취소
	@DeleteMapping("/{reviewId}/like")
	public int unlikeReview(@PathVariable UUID reviewId) {
		UUID userId = SecurityUtil.getCurrentUserId();
		return reviewService.unlikeReview(reviewId, userId);
	}

}
