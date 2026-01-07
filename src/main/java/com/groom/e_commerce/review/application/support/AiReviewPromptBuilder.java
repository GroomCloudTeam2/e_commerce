package com.groom.e_commerce.review.application.support;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;

@Component
public class AiReviewPromptBuilder {

	public String build(String productTitle, Map<ReviewCategory, List<ReviewEntity>> reviews) {
		StringBuilder sb = new StringBuilder();

		sb.append("""
			다음은 상품 "%s" 대한 사용자 리뷰입니다.
			카테고리별 핵심 의견을 요약하세요.
			과장하지 말고, 공통된 의견 위주로 작성하세요.
			결과는 JSON 형식으로 반환하세요.
			
			{
			  "DELIVERY": "",
			  "QUALITY": "",
			  "PRICE": "",
			  "DESIGN": "",
			  "ETC": ""
			}
			
			리뷰 목록:
			""".formatted(productTitle));

		reviews.forEach((category, list) -> {
			sb.append("\n[").append(category.name()).append("]\n");
			list.forEach(r -> sb.append("- ").append(r.getContent()).append("\n"));
		});

		return sb.toString();
	}
}
