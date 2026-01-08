package com.groom.e_commerce.review.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.groom.e_commerce.global.infrastructure.client.OpenAi.OpenAiRestClient;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.review.application.support.AiReviewPromptBuilder;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewAiSummaryService {

	private final ReviewRepository reviewRepository;
	private final ProductRatingRepository productRatingRepository;
	private final AiReviewPromptBuilder promptBuilder;
	private final OpenAiRestClient openAiRestClient;
	private final ProductRepository productRepository;

	public void generate(UUID productId) {

		Map<ReviewCategory, List<ReviewEntity>> reviews =
			Arrays.stream(ReviewCategory.values())
				.collect(
					Collectors.toMap(
						c -> c,
						c -> reviewRepository.findTopReviews(
							productId,
							c,
							PageRequest.of(0, 10)
						)
					));
		String productTitle = productRepository.findByIdAndNotDeleted(productId)
			.map(Product::getTitle)
			.orElseThrow(() -> new IllegalStateException("상품 제목이 없습니다."));
		String prompt = promptBuilder.build(productTitle, reviews);
		String aiReview = openAiRestClient.summarizeReviews(prompt);

		ProductRatingEntity rating = productRatingRepository
			.findByProductId(productId)
			.orElseThrow();

		rating.updateAiReview(aiReview);
		productRatingRepository.save(rating);

	}
}
