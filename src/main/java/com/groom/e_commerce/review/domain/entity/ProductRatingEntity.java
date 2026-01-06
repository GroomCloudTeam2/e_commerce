package com.groom.e_commerce.review.domain.entity;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

@Entity
@Table(name = "p_product_rating")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductRatingEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID productRatingId;

	@Column(nullable = false, unique = true)
	private UUID productId;

	@Column(nullable = false)
	private double avgRating = 0.0;

	@Column(nullable = false)
	private Integer reviewCount = 0;

	@Column(name = "ai_review", columnDefinition = "TEXT")
	private String aiReview;

	@Version
	private Long version;

	public ProductRatingEntity(UUID productId) {
		this.productId = productId;
		this.avgRating = 0.0;
		this.reviewCount = 0;
	}

	public void updateRating(Integer newRating) {
		double currentTotal = this.avgRating * this.reviewCount;
		this.reviewCount += 1;
		double updatedAvg = (currentTotal + newRating) / this.reviewCount;
		this.avgRating = Math.round(updatedAvg * 10.0) / 10.0;
	}

	public void removeRating(Integer oldRating) {
		if (this.reviewCount <= 1) {
			this.reviewCount = 0;
			this.avgRating = 0.0;
			return;
		}

		double currentTotal = this.avgRating * this.reviewCount;
		this.reviewCount -= 1;

		double updatedAvg = (currentTotal - oldRating) / this.reviewCount;
		this.avgRating = Math.round(updatedAvg * 10.0) / 10.0;
	}

	public void updateAiReview(String aiReview) {
		this.aiReview = aiReview;
	}
}
