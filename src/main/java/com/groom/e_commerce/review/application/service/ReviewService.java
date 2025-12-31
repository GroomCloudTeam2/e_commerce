package com.groom.e_commerce.review.application.service;

import com.groom.e_commerce.global.infrastructure.client.AiWebClient;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.response.ProductReviewResponse;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;
import com.groom.e_commerce.review.presentation.dto.response.PaginationResponse;

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
		UUID currentUserId,
		CreateReviewRequest request
	) {
		// 1. 중복 리뷰 체크
		reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.ifPresent(r -> {
				throw new IllegalStateException("이미 리뷰가 존재합니다.");
			});

		ReviewCategory category = classifyComment(request.getContent());

		// 3. 리뷰 엔티티 생성 및 저장
		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(currentUserId)
			.rating(request.getRating())
			.content(request.getContent())
			.category(category)
			.build();

		reviewRepository.save(review);

		// 4. 상품 평점 업데이트
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
		UUID currentUserId,
		UpdateReviewRequest request
	) {
		// 1. 리뷰 존재 여부 확인
		ReviewEntity review = reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

		// 2. 권한 확인 (본인 리뷰인지)
		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("수정 권한이 없습니다.");
		}

		// 3. 평점 변경 시 통계 업데이트
		if (request.getRating() != null && !review.getRating().equals(request.getRating())) {
			ProductRatingEntity ratingEntity = productRatingRepository.findByProductId(productId)
				.orElseThrow(() -> new IllegalStateException("상품 통계 정보가 없습니다."));

			ratingEntity.removeRating(review.getRating());
			ratingEntity.updateRating(request.getRating());

			review.updateRating(request.getRating());
		}

		// 4. 내용 변경 시 AI 카테고리 재분류 (수정된 부분)
		if (request.getContent() != null && !request.getContent().isBlank()) {
			// String categoryStr 단계를 생략하고 바로 Enum으로 받습니다.
			ReviewCategory category = classifyComment(request.getContent());
			review.updateContentAndCategory(request.getContent(), category);
		}

		return ReviewResponse.fromEntity(review);
	}

	/**
	 * 리뷰 삭제 (소프트 딜리트)
	 */
	@Transactional
	public void deleteReview(UUID reviewId, UUID currentUserId) {

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
	public ReviewResponse getReview(UUID orderId, UUID productId, UUID currentUserId) {

		ReviewEntity review = reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("조회 권한이 없습니다.");
		}

		return ReviewResponse.fromEntity(review);
	}

	private ReviewCategory classifyComment(String comment) {
		try {
			AiWebClient.AiResponse response = aiWebClient.classifyComment(comment);

			if (response != null && response.getCategory() != null) {
				return response.getCategory();
			}

			log.warn("AI 응답이 비어있습니다. 기본 카테고리(ERR)로 설정합니다. comment: {}", comment);
		} catch (Exception e) {
			// WebClient에서 이미 에러 처리를 하고 있지만, 서비스 단에서도 안전하게 한 번 더 감싸줍니다.
			log.error("AI 서버 통신 중 예외 발생. 기본 카테고리(ERR)로 설정합니다.", e);
		}

		// AI 서버가 죽었거나 응답이 이상할 경우 가장 안전한 기본값 반환
		return ReviewCategory.ERR;
	}
}
