package com.groom.e_commerce.review.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProductRatingEntityTest {

	@Test
	void 생성시_평균과_리뷰수는_0이다() {
		// given
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		// then
		assertThat(rating.getAvgRating()).isEqualTo(0.0);
		assertThat(rating.getReviewCount()).isEqualTo(0);
	}

	@Test
	void 첫_리뷰_추가시_평균은_해당_점수이다() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		// when
		rating.updateRating(4);

		// then
		assertThat(rating.getReviewCount()).isEqualTo(1);
		assertThat(rating.getAvgRating()).isEqualTo(4.0);
	}

	@Test
	void 여러_리뷰_추가시_평균이_정확히_계산된다() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		// when
		rating.updateRating(5); // avg 5.0
		rating.updateRating(4); // avg 4.5
		rating.updateRating(3); // avg 4.0

		// then
		assertThat(rating.getReviewCount()).isEqualTo(3);
		assertThat(rating.getAvgRating()).isEqualTo(4.0);
	}

	@Test
	void 평균은_소수점_한자리로_반올림된다() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		// when
		rating.updateRating(5);
		rating.updateRating(4); // 4.5
		rating.updateRating(4); // (5+4+4)/3 = 4.333... → 4.3

		// then
		assertThat(rating.getAvgRating()).isEqualTo(4.3);
	}

	@Test
	void 리뷰_1개일때_삭제하면_초기화된다() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		rating.updateRating(5);

		// when
		rating.removeRating(5);

		// then
		assertThat(rating.getReviewCount()).isEqualTo(0);
		assertThat(rating.getAvgRating()).isEqualTo(0.0);
	}

	@Test
	void 여러_리뷰중_하나_삭제시_평균이_재계산된다() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		rating.updateRating(5);
		rating.updateRating(4);
		rating.updateRating(3); // avg 4.0

		// when
		rating.removeRating(3); // (5+4)/2 = 4.5

		// then
		assertThat(rating.getReviewCount()).isEqualTo(2);
		assertThat(rating.getAvgRating()).isEqualTo(4.5);
	}

	@Test
	void AI_리뷰_업데이트() {
		ProductRatingEntity rating =
			new ProductRatingEntity(UUID.randomUUID());

		// when
		rating.updateAiReview("AI 요약 리뷰");

		// then
		assertThat(rating.getAiReview()).isEqualTo("AI 요약 리뷰");
	}
}
