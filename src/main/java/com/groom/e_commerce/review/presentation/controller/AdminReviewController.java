package com.groom.e_commerce.review.presentation.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.review.application.service.ReviewAiSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

	private final ReviewAiSummaryService reviewAiSummaryService;

	@Operation(summary = "관리자가 실행 시키는 ai 리뷰 생성")
	@PostMapping("/{productId}/ai-summary")
	public void regenerateAiReview(@PathVariable UUID productId) {
		reviewAiSummaryService.generate(productId);
	}

}
