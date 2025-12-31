package com.groom.e_commerce.review.domain.repository;

import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRatingEntity, UUID> {

	Optional<ProductRatingEntity> findByProductId(UUID productId);
}

