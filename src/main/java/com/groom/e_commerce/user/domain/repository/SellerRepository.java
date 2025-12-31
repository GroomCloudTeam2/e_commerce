package com.groom.e_commerce.user.domain.repository;

import com.groom.e_commerce.user.domain.entity.SellerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<SellerEntity, UUID> {

    Optional<SellerEntity> findByUserUserId(UUID userId);

    boolean existsByUserUserId(UUID userId);
}
