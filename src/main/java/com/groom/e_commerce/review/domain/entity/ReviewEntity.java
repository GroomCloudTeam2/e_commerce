package com.groom.e_commerce.review.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted = false")
public class ReviewEntity {

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

	@Column(columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReviewCategory category;

	@Column(nullable = false)
	private int likeCount = 0;

	/* ================= 감사(Auditing) ================= */

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private UUID createdBy;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by")
	private UUID updatedBy;

	/* ================= 소프트 삭제 ================= */

	@Column(nullable = false)
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@LastModifiedBy
	@Column(name = "deleted_by")
	private UUID deletedBy;

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

	/* ================= 소프트 딜리트 ================= */

	public void softDelete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}
}
