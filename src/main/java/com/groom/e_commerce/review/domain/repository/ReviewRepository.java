package com.groom.e_commerce.review.domain.repository;

import com.groom.e_commerce.review.domain.entity.ReviewEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

	Optional<ReviewEntity> findByOrderIdAndProductId(UUID orderId, UUID productId);


	Page<ReviewEntity> findAllByProductId(UUID productId, Pageable pageable);
}
