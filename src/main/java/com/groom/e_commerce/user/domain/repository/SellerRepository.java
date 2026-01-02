package com.groom.e_commerce.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.user.domain.entity.SellerEntity;

@Repository
public interface SellerRepository extends JpaRepository<SellerEntity, UUID> {

	Optional<SellerEntity> findByUserUserId(UUID userId);

	boolean existsByUserUserId(UUID userId);
}
