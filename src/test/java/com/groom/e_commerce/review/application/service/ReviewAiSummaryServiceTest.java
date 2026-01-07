package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.groom.e_commerce.global.infrastructure.client.OpenAi.OpenAiRestClient;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.review.application.support.AiReviewPromptBuilder;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewAiSummaryServiceTest {

	@InjectMocks
	private ReviewAiSummaryService reviewAiSummaryService;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private ProductRatingRepository productRatingRepository;

	@Mock
	private AiReviewPromptBuilder promptBuilder;

	@Mock
	private OpenAiRestClient openAiRestClient;

	@Mock
	private ProductRepository productRepository;

	private UUID productId;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
	}

	@Test
	void AI_리뷰_요약_생성_성공() {
		// given
		doReturn(List.of(mock(ReviewEntity.class)))
			.when(reviewRepository)
			.findTopReviews(
				eq(productId),
				any(ReviewCategory.class),
				any(PageRequest.class)
			);

		when(productRepository.findTitleById(productId))
			.thenReturn(Optional.of("상품 제목"));

		when(promptBuilder.build(eq("상품 제목"), any()))
			.thenReturn("AI PROMPT");

		when(openAiRestClient.summarizeReviews("AI PROMPT"))
			.thenReturn("AI 요약 결과");

		ProductRatingEntity rating = new ProductRatingEntity(productId);
		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.of(rating));

		// when
		reviewAiSummaryService.generate(productId);

		// then
		assertThat(rating.getAiReview()).isEqualTo("AI 요약 결과");
		verify(productRatingRepository).save(rating);
	}

	@Test
	void ProductRating이_없으면_예외() {
		for (ReviewCategory category : ReviewCategory.values()) {
			doReturn(List.of(mock(ReviewEntity.class)))
				.when(reviewRepository)
				.findTopReviews(
					eq(productId),
					eq(category),
					any(PageRequest.class)
				);
		}

		when(productRepository.findTitleById(productId))
			.thenReturn(Optional.of("상품 제목"));
		when(promptBuilder.build(anyString(), any()))
			.thenReturn("PROMPT");
		when(openAiRestClient.summarizeReviews(anyString()))
			.thenReturn("AI 요약");
		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() ->
			reviewAiSummaryService.generate(productId)
		).isInstanceOf(NoSuchElementException.class);
	}
}
