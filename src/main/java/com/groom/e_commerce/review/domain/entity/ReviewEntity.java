package com.groom.e_commerce.review.domain.entity;

import java.util.UUID;

import org.hibernate.annotations.Where;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Where(clause = "deleted_at IS NULL")
public class ReviewEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "review_id")
	private UUID reviewId;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	@Min(1)
	@Max(5)
	private Integer rating;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReviewCategory category;

	@Column(nullable = false)
	private int likeCount = 0;

	/* ================= 생성자 ================= */

	@Builder
	public ReviewEntity(
		UUID orderId,
		UUID productId,
		UUID userId,
		Integer rating,
		String content,
		ReviewCategory category
	) {
		this.orderId = orderId;
		this.productId = productId;
		this.userId = userId;
		this.rating = rating;
		this.content = content;
		this.category = category;
	}

	/* ================= 비즈니스 메서드 ================= */

	public void updateRating(Integer rating) {
		this.rating = rating;
	}

	public void updateContentAndCategory(String content, ReviewCategory category) {
		this.content = content;
		this.category = category;
	}

	public void incrementLikeCount() {
		this.likeCount++;
	}

	public void decrementLikeCount() {
		if (this.likeCount > 0) {
			this.likeCount--;
		}
	}
}
