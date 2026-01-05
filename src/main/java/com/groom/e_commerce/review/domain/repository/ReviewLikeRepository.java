package com.groom.e_commerce.review.domain.repository;

import com.groom.e_commerce.review.domain.entity.ReviewLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewLikeRepository extends JpaRepository<ReviewLikeEntity, UUID> {
	Optional<ReviewLikeEntity> findByReviewIdAndUserId(UUID reviewId, UUID userId);
}
