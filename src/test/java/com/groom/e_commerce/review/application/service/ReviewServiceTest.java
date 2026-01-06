package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@InjectMocks
	private ReviewService reviewService;

	@Mock
	private ReviewRepository reviewRepository;
	@Mock
	private ReviewLikeRepository reviewLikeRepository;
	@Mock
	private ProductRatingRepository productRatingRepository;
	@Mock
	private AiRestClient aiRestClient;
	@Mock
	private OrderReviewValidator orderReviewValidator;

	private UUID userId;
	private UUID productId;
	private UUID orderId;
	private UUID reviewId;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		productId = UUID.randomUUID();
		orderId = UUID.randomUUID();
		reviewId = UUID.randomUUID();
	}

	/* ================= 리뷰 생성 ================= */

	@Test
	void 리뷰_생성_정상() {
		// given
		CreateReviewRequest request =
			new CreateReviewRequest(5, "디자인 좋음");

		AiRestClient.AiResponse aiResponse =
			new AiRestClient.AiResponse(ReviewCategory.DESIGN, 0.95);

		when(aiRestClient.classifyComment(anyString()))
			.thenReturn(aiResponse);

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.empty());

		// when
		var response = reviewService.createReview(
			orderId, productId, userId, request
		);

		// then
		verify(orderReviewValidator)
			.validate(orderId, productId, userId);

		verify(reviewRepository).save(
			argThat(review ->
				review.getCategory() == ReviewCategory.DESIGN &&
					review.getRating() == 5
			)
		);

		assertThat(response).isNotNull();
	}

	@Test
	void AI_카테고리가_null이면_NPE_발생() {
		assertThatThrownBy(() ->
			ReviewCategory.fromAiCategory(null)
		).isInstanceOf(NullPointerException.class);
	}

	@Test
	void AI_ERR_카테고리여도_리뷰는_저장됨() {
		// given
		CreateReviewRequest request =
			new CreateReviewRequest(3, "의미불명");

		AiRestClient.AiResponse aiResponse =
			new AiRestClient.AiResponse(ReviewCategory.ERR, 0.0);

		when(aiRestClient.classifyComment(anyString()))
			.thenReturn(aiResponse);

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.empty());

		// when
		var response = reviewService.createReview(
			orderId, productId, userId, request
		);

		// then
		verify(reviewRepository).save(
			argThat(review ->
				review.getCategory() == ReviewCategory.ERR
			)
		);

		assertThat(response).isNotNull();
	}

	/* ================= 리뷰 수정 ================= */

	@Test
	void 리뷰_수정_본인_성공() {
		// given
		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(userId)
			.rating(3)
			.content("기존")
			.category(ReviewCategory.PRICE)
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.of(new ProductRatingEntity(productId)));

		AiRestClient.AiResponse aiResponse =
			new AiRestClient.AiResponse(ReviewCategory.QUALITY, 0.8);

		when(aiRestClient.classifyComment(anyString()))
			.thenReturn(aiResponse);

		// when
		var response = reviewService.updateReview(
			reviewId, userId,
			new UpdateReviewRequest("수정", 5)
		);

		// then
		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getCategory()).isEqualTo(ReviewCategory.QUALITY);
		assertThat(response).isNotNull();
	}

	@Test
	void 리뷰_수정_권한없음() {
		// given
		ReviewEntity review = ReviewEntity.builder()
			.userId(UUID.randomUUID())
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		// then
		assertThatThrownBy(() ->
			reviewService.updateReview(
				reviewId, userId,
				new UpdateReviewRequest("x", 1)
			)
		).isInstanceOf(SecurityException.class);
	}

	/* ================= 좋아요 ================= */

	@Test
	void 좋아요_성공() {
		// given
		ReviewEntity review = ReviewEntity.builder().build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId))
			.thenReturn(Optional.empty());

		// when
		int count = reviewService.likeReview(reviewId, userId);

		// then
		assertThat(count).isEqualTo(1);
		verify(reviewLikeRepository).save(any());
	}

	@Test
	void 좋아요_중복_실패() {
		// given
		ReviewEntity review = ReviewEntity.builder().build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId))
			.thenReturn(Optional.of(new ReviewLikeEntity(reviewId, userId)));

		// then
		assertThatThrownBy(() ->
			reviewService.likeReview(reviewId, userId)
		).isInstanceOf(IllegalStateException.class);
	}
}
