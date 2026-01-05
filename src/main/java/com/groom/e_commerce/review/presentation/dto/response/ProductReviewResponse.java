package com.groom.e_commerce.review.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductReviewResponse {
	private Double avgRating;      // 추가: 평균 별점
	private Integer reviewCount;   // 추가: 전체 리뷰 수
	private String aiReview;       // 기존 요약
	private List<ReviewResponse> reviews;
	private PaginationResponse pagination; // 추가: 페이징 메타데이터
}
