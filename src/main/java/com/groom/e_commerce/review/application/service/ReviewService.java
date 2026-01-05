package com.groom.e_commerce.review.application.service;

import com.groom.e_commerce.global.infrastructure.client.Classification.AiRestClient;
import com.groom.e_commerce.review.application.validator.OrderReviewValidator;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.entity.ReviewLikeEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewLikeRepository;
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
	private final ReviewLikeRepository reviewLikeRepository;
	private final ProductRatingRepository productRatingRepository;
	private final AiRestClient aiRestClient;
	private final OrderReviewValidator orderReviewValidator;

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
		// 0. 주문/상품/유저 검증 (신규)
		orderReviewValidator.validate(orderId, productId, currentUserId);


		// 2. AI 카테고리 분류
		ReviewCategory category = classifyComment(request.getContent());

		// 3. 리뷰 저장
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
		ProductRatingEntity ratingEntity =
			productRatingRepository.findByProductId(productId)
				.orElseGet(() -> new ProductRatingEntity(productId));

		ratingEntity.updateRating(request.getRating());
		productRatingRepository.save(ratingEntity);

		return ReviewResponse.fromEntity(review);
	}


	/**
	 * 리뷰 수정
	 */
	@Transactional
	public ReviewResponse updateReview(UUID reviewId, UUID currentUserId, UpdateReviewRequest request) {
		// 1. 리뷰 존재 여부 확인
		ReviewEntity review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		// 2. 권한 확인 (본인 리뷰인지)
		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("수정 권한이 없습니다.");
		}

		// 3. 평점 변경 시 통계 업데이트
		if (request.getRating() != null && !review.getRating().equals(request.getRating())) {
			ProductRatingEntity ratingEntity = productRatingRepository.findByProductId(review.getProductId())
				.orElseThrow(() -> new IllegalStateException("상품 통계 정보가 없습니다."));

			ratingEntity.removeRating(review.getRating());
			ratingEntity.updateRating(request.getRating());

			review.updateRating(request.getRating());
		}

		// 4. 내용 변경 시 AI 카테고리 재분류
		if (request.getContent() != null && !request.getContent().isBlank()) {
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
		review.softDelete(currentUserId.toString());
	}

	/**
	 * 상품별 리뷰 목록 조회
	 */
	public ProductReviewResponse getProductReviews(UUID productId, int page, int size, String sortParam) {

		Sort sort;
		// 정렬 기준 결정
		if ("like".equalsIgnoreCase(sortParam)) {
			// 좋아요 순, 동률이면 최신순으로
			sort = Sort.by("likeCount").descending().and(Sort.by("createdAt").descending());
		} else {
			// 최신순
			sort = Sort.by("createdAt").descending();
		}

		Pageable pageable = PageRequest.of(page, size, sort);

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
	public ReviewResponse getReview(UUID reviewId, UUID currentUserId) {

		ReviewEntity review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		if (!review.getUserId().equals(currentUserId)) {
			throw new SecurityException("조회 권한이 없습니다.");
		}

		return ReviewResponse.fromEntity(review);
	}

	private ReviewCategory classifyComment(String comment) {
		try {
			AiRestClient.AiResponse response = aiRestClient.classifyComment(comment);

			if (response == null || response.getCategory() == null) {
				log.warn(
					"AI 응답이 비어있습니다. 기본 카테고리(ERR)로 설정합니다. comment: {}",
					comment
				);
				return ReviewCategory.ERR;
			}

			return response.getCategory();

		} catch (Exception e) {

			log.error(
				"AI 서버 통신 중 예외 발생. 기본 카테고리(ERR)로 설정합니다. comment: {}",
				comment,
				e
			);
			return ReviewCategory.ERR;
		}
	}


	@Transactional
	public int likeReview(UUID reviewId, UUID userId) {
		ReviewEntity review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		// 이미 좋아요 했는지 체크
		reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId)
			.ifPresent(like -> {
				throw new IllegalStateException("이미 좋아요를 눌렀습니다.");
			});

		// 좋아요 저장
		ReviewLikeEntity like = new ReviewLikeEntity(reviewId, userId);
		reviewLikeRepository.save(like);

		// 카운트 증가
		review.incrementLikeCount();
		return review.getLikeCount();
	}

	@Transactional
	public int unlikeReview(UUID reviewId, UUID userId) {
		ReviewEntity review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

		ReviewLikeEntity like = reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId)
			.orElseThrow(() -> new IllegalStateException("좋아요를 누르지 않은 리뷰입니다."));

		reviewLikeRepository.delete(like);

		review.decrementLikeCount();
		return review.getLikeCount();
	}

}
