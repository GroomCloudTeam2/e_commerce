package com.groom.e_commerce.review.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.e_commerce.review.domain.entity.ReviewLikeEntity;

public interface ReviewLikeRepository extends JpaRepository<ReviewLikeEntity, UUID> {
	Optional<ReviewLikeEntity> findByReviewIdAndUserId(UUID reviewId, UUID userId);
}
