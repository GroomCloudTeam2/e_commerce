package com.groom.e_commerce.review.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReviewCategoryTest {

	@Test
	void AI_카테고리_디자인_매핑() {
		assertThat(ReviewCategory.fromAiCategory("디자인/외형"))
			.isEqualTo(ReviewCategory.DESIGN);
	}

	@Test
	void AI_카테고리_성능_매핑() {
		assertThat(ReviewCategory.fromAiCategory("성능/기능"))
			.isEqualTo(ReviewCategory.PERFORMANCE);
	}

	@Test
	void AI_카테고리_편의성_매핑() {
		assertThat(ReviewCategory.fromAiCategory("편의성/사용감"))
			.isEqualTo(ReviewCategory.CONVENIENCE);
	}

	@Test
	void AI_카테고리_가격_매핑() {
		assertThat(ReviewCategory.fromAiCategory("가격/구성"))
			.isEqualTo(ReviewCategory.PRICE);
	}

	@Test
	void AI_카테고리_품질_매핑() {
		assertThat(ReviewCategory.fromAiCategory("품질/내구성"))
			.isEqualTo(ReviewCategory.QUALITY);
	}

	@Test
	void 알수없는_AI_카테고리는_null_반환() {
		assertThat(ReviewCategory.fromAiCategory("기타"))
			.isNull();
	}

	@Test
	void AI_카테고리가_null이면_NPE_발생() {
		assertThatThrownBy(() ->
			ReviewCategory.fromAiCategory(null)
		).isInstanceOf(NullPointerException.class);
	}
}
