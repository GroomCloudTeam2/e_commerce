package com.groom.e_commerce.review.presentation.dto.response;

import java.util.UUID;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
	private UUID reviewId;
	private UUID orderId;
	private UUID productId;
	private UUID userId;
	private Integer rating;
	private String content;
	private ReviewCategory category; // AI가 분류한 카테고리

	public static ReviewResponse fromEntity(ReviewEntity entity) {
		return ReviewResponse.builder()
			.reviewId(entity.getReviewId())
			.orderId(entity.getOrderId())
			.productId(entity.getProductId())
			.userId(entity.getUserId())
			.rating(entity.getRating())
			.content(entity.getContent())
			.category(entity.getCategory())
			.build();
	}
}
