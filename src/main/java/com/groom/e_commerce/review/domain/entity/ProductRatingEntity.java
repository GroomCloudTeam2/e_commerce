package com.groom.e_commerce.review.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_product_rating")
@Getter
@NoArgsConstructor
public class ProductRatingEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID productRatingId;

	@Column(nullable = false, unique = true)
	private UUID productId;

	@Column(nullable = false)
	private double avgRating = 0.0;

	@Column(nullable = false)
	private Integer reviewCount = 0;

	@Column(name = "AI_review", columnDefinition = "TEXT")
	private String aiReview; // 추후 LLM을 통해 생성될 리뷰 요약. 현재는 NULL값 입력

	@Version
	private Long version;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

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
}
