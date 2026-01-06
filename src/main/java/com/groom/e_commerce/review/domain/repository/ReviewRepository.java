package com.groom.e_commerce.review.domain.repository;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

	Optional<ReviewEntity> findByOrderIdAndProductId(UUID orderId, UUID productId);


	Page<ReviewEntity> findAllByProductId(UUID productId, Pageable pageable);

	@Query("""
        SELECT r FROM ReviewEntity r
        WHERE r.productId = :productId
          AND r.category = :category
        ORDER BY r.likeCount DESC, r.createdAt DESC
    """)
	List<ReviewEntity> findTopReviews(
		UUID productId,
		ReviewCategory category,
		Pageable pageable
	);

}
