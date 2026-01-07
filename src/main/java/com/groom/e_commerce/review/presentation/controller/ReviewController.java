package com.groom.e_commerce.review.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.global.infrastructure.config.security.CustomUserDetails;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;
import com.groom.e_commerce.user.domain.entity.user.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	/*
	 * 인증 사용자 ID 조회
	 * - 프로덕션: SecurityUtil 사용
	 * - 테스트: override
	 */
	protected UUID getCurrentUserId() {
		return SecurityUtil.getCurrentUserId();
	}

	@Operation(summary = "내 주문에 대한 특정 상품 리뷰 작성")
	@PostMapping("/{orderId}/items/{productId}")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse createReview(
		@PathVariable UUID orderId,
		@PathVariable UUID productId,
		@RequestBody CreateReviewRequest request
	) {
		UUID userId = getCurrentUserId();
		return reviewService.createReview(orderId, productId, userId, request);
	}

	@Operation(summary = "특정 상품에 대한 내 리뷰 보기")
	@GetMapping("/me/{reviewId}")
	public ReviewResponse getReview(
		@PathVariable UUID reviewId
	) {
		UUID userId = getCurrentUserId();
		return reviewService.getReview(reviewId, userId);
	}

	@Operation(summary = "내 리뷰 모두 보기")
	@GetMapping("/me")
	public Page<ReviewResponse> getMyReviews(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return reviewService.getMyReviews(
			SecurityUtil.getCurrentUserId(),
			PageRequest.of(page, size)
		);
	}

	@Operation(summary = "내 리뷰 수정")
	@PutMapping("/{reviewId}")
	public ReviewResponse updateReview(
		@PathVariable UUID reviewId,
		@RequestBody UpdateReviewRequest request
	) {
		UUID userId = getCurrentUserId();
		return reviewService.updateReview(reviewId, userId, request);
	}

	@Operation(summary = "권한에 따른 리뷰 삭제")
	@DeleteMapping("/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReview(
		@PathVariable UUID reviewId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		reviewService.deleteReview(
			reviewId,
			userDetails.getUserId(),
			UserRole.valueOf(userDetails.getRole())
		);
	}

	@Operation(summary = "리뷰 좋아요")
	@PostMapping("/{reviewId}/like")
	public int likeReview(@PathVariable UUID reviewId) {
		UUID userId = getCurrentUserId();
		return reviewService.likeReview(reviewId, userId);
	}

	@Operation(summary = "리뷰 좋아요 취소")
	@DeleteMapping("/{reviewId}/like")
	public int unlikeReview(@PathVariable UUID reviewId) {
		UUID userId = getCurrentUserId();
		return reviewService.unlikeReview(reviewId, userId);
	}
}
