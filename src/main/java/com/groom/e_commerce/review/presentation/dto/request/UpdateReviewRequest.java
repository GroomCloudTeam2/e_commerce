package com.groom.e_commerce.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class UpdateReviewRequest {
	@Min(1)
	@Max(5)
	private Integer rating;

	private String content;
}
