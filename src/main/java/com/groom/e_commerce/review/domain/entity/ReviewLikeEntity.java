package com.groom.e_commerce.review.domain.entity;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Where;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

@Entity
@Table(
	name = "review_like",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"review_id", "user_id"})
	}
)
@Getter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class ReviewLikeEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "review_id", nullable = false)
	private UUID reviewId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	public ReviewLikeEntity(UUID reviewId, UUID userId) {
		this.reviewId = reviewId;
		this.userId = userId;
	}
}
