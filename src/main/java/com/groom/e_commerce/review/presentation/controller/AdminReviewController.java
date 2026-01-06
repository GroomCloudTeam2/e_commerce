package com.groom.e_commerce.review.presentation.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.review.application.service.ReviewAiSummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

	private final ReviewAiSummaryService reviewAiSummaryService;

	@PostMapping("/{productId}/ai-summary")
	public void regenerateAiReview(@PathVariable UUID productId) {
		reviewAiSummaryService.generate(productId);
	}
}
