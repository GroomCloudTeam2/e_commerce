package com.groom.ecommerce.review.application.service;

import com.groom.ecommerce.global.infrastructure.client.AiWebClient;
import com.groom.ecommerce.global.infrastructure.security.SecurityUtil;
import com.groom.ecommerce.review.domain.entity.ProductRatingEntity;
import com.groom.ecommerce.review.domain.entity.ReviewCategory;
import com.groom.ecommerce.review.domain.entity.ReviewEntity;
import com.groom.ecommerce.review.domain.repository.ProductRatingRepository;
import com.groom.ecommerce.review.domain.repository.ReviewRepository;
import com.groom.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.ecommerce.review.presentation.dto.response.ProductReviewResponse;
import com.groom.ecommerce.review.presentation.dto.response.ReviewResponse;
import com.groom.ecommerce.review.presentation.dto.response.PaginationResponse;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final ProductRatingRepository productRatingRepository;
	private final AiWebClient aiWebClient;

	/**
	 * 리뷰 작성
	 */
	@Transactional
	public ReviewResponse createReview(
		UUID orderId,
		UUID productId,
		CreateReviewRequest request
	) {
		UUID currentUserId = SecurityUtil.getCurrentUserId();

		reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.ifPresent(r -> { throw new IllegalStateException("이미 리뷰가 존재합니다."); });

		String aiCategoryStr = classifyComment(request.getContent());
		ReviewCategory category = ReviewCategory.fromAiCategory(aiCategoryStr);

		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(currentUserId)
			.rating(request.getRating())
			.content(request.getContent())
			.category(category)
			.build();

		reviewRepository.save(review);

		ProductRatingEntity ratingEntity = productRatingRepository.findByProductId(productId)
			.orElseGet(() -> new ProductRatingEntity(productId));

		ratingEntity.updateRating(request.getRating());
		productRatingRepository.save(ratingEntity);

		return ReviewResponse.fromEntity(review);
	}

	/**
	 * 리뷰 수정
	 */
	@Transactional
	public ReviewResponse updateReview(
		UUID orderId,
		UUID productId,
		UpdateReviewRequest request
	) {
		UUID currentUserId = SecurityUtil.getCurrentUserId();

		ReviewEntity review = reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("수정 권한이 없습니다.");
		}

		if (request.getRating() != null &&
			!review.getRating().equals(request.getRating())) {

			ProductRatingEntity ratingEntity =
				productRatingRepository.findByProductId(productId)
					.orElseThrow(() -> new IllegalStateException("상품 통계 정보가 없습니다."));

			ratingEntity.removeRating(review.getRating());
			ratingEntity.updateRating(request.getRating());

			review.updateRating(request.getRating());
		}

		if (request.getContent() != null && !request.getContent().isBlank()) {
			String categoryStr = classifyComment(request.getContent());
			ReviewCategory category = ReviewCategory.fromAiCategory(categoryStr);
			review.updateContentAndCategory(request.getContent(), category);
		}

		return ReviewResponse.fromEntity(review);
	}

	/**
	 * 리뷰 삭제 (소프트 딜리트)
	 */
	@Transactional
	public void deleteReview(UUID reviewId) {
		UUID currentUserId = SecurityUtil.getCurrentUserId();

		ReviewEntity review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("삭제 권한이 없습니다.");
		}

		ProductRatingEntity ratingEntity =
			productRatingRepository.findByProductId(review.getProductId())
				.orElseThrow(() -> new IllegalStateException("상품 통계 정보가 없습니다."));

		ratingEntity.removeRating(review.getRating());
		review.softDelete();
	}

	/**
	 * 상품별 리뷰 목록 조회
	 */
	public ProductReviewResponse getProductReviews(UUID productId, int page, int size) {
		Pageable pageable =
			PageRequest.of(page, size, Sort.by("reviewId").descending());

		Page<ReviewEntity> reviewPage =
			reviewRepository.findAllByProductId(productId, pageable);

		ProductRatingEntity ratingEntity =
			productRatingRepository.findByProductId(productId)
				.orElseGet(() -> new ProductRatingEntity(productId));

		return ProductReviewResponse.builder()
			.avgRating(ratingEntity.getAvgRating())
			.reviewCount(ratingEntity.getReviewCount())
			.aiReview(ratingEntity.getAiReview())
			.reviews(reviewPage.getContent().stream()
				.map(ReviewResponse::fromEntity)
				.collect(Collectors.toList()))
			.pagination(PaginationResponse.builder()
				.totalElements(reviewPage.getTotalElements())
				.totalPages(reviewPage.getTotalPages())
				.currentPage(reviewPage.getNumber())
				.isLast(reviewPage.isLast())
				.build())
			.build();
	}

	/**
	 * 단건 리뷰 조회
	 */
	public ReviewResponse getReview(UUID orderId, UUID productId) {

		ReviewEntity review = reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

		return ReviewResponse.fromEntity(review);
	}

	private String classifyComment(String comment) {
		try {
			AiWebClient.AiResponse response = aiWebClient.classifyComment(comment);
			return (response != null) ? response.getCategory() : null;
		} catch (Exception e) {
			log.error("AI 서버 통신 실패", e);
			return null;
		}
	}
}
