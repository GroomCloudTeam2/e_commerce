package com.groom.e_commerce.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// CreateReviewRequest.java
@Getter
public class CreateReviewRequest {
	@Min(1)
	@Max(5)
	@NotNull(message = "평점은 필수입니다.")
	private Integer rating;

	@NotBlank(message = "리뷰 내용을 입력해주세요.")
	private String content;

}
