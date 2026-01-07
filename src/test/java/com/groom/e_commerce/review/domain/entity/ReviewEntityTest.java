package com.groom.e_commerce.review.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReviewEntityTest {

	@Test
	void 리뷰_생성자_정상_동작() {
		// given
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		// when
		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(userId)
			.rating(5)
			.content("색이 아주 이뻐요")
			.category(ReviewCategory.DESIGN)
			.build();

		// then
		assertThat(review.getOrderId()).isEqualTo(orderId);
		assertThat(review.getProductId()).isEqualTo(productId);
		assertThat(review.getUserId()).isEqualTo(userId);
		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getContent()).isEqualTo("색이 아주 이뻐요");
		assertThat(review.getCategory()).isEqualTo(ReviewCategory.DESIGN);
		assertThat(review.getLikeCount()).isZero();
	}

	@Test
	void 평점_수정() {
		// given
		ReviewEntity review = createReview(3);

		// when
		review.updateRating(5);

		// then
		assertThat(review.getRating()).isEqualTo(5);
	}

	@Test
	void 내용_및_카테고리_수정() {
		// given
		ReviewEntity review = createReview(4);

		// when
		review.updateContentAndCategory("수정된 리뷰는 퀄리티에 대한 내용이다.", ReviewCategory.QUALITY);

		// then
		assertThat(review.getContent()).isEqualTo("수정된 리뷰는 퀄리티에 대한 내용이다.");
		assertThat(review.getCategory()).isEqualTo(ReviewCategory.QUALITY);
	}

	@Test
	void 좋아요_증가() {
		// given
		ReviewEntity review = createReview(5);

		// when
		review.incrementLikeCount();
		review.incrementLikeCount();

		// then
		assertThat(review.getLikeCount()).isEqualTo(2);
	}

	@Test
	void 좋아요_감소() {
		// given
		ReviewEntity review = createReview(5);
		review.incrementLikeCount();
		review.incrementLikeCount();

		// when
		review.decrementLikeCount();

		// then
		assertThat(review.getLikeCount()).isEqualTo(1);
	}

	@Test
	void 좋아요가_0일때_감소해도_0_유지() {
		// given
		ReviewEntity review = createReview(5);

		// when
		review.decrementLikeCount();

		// then
		assertThat(review.getLikeCount()).isZero();
	}

	/* ================= 헬퍼 ================= */

	private ReviewEntity createReview(int rating) {
		return ReviewEntity.builder()
			.orderId(UUID.randomUUID())
			.productId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.rating(rating)
			.content("색이 정말 이쁘다")
			.category(ReviewCategory.DESIGN)
			.build();
	}
}
