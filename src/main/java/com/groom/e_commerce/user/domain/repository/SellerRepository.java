package com.groom.e_commerce.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.user.domain.entity.seller.SellerEntity;
import com.groom.e_commerce.user.domain.entity.seller.SellerStatus;

@Repository
public interface SellerRepository extends JpaRepository<SellerEntity, UUID> {
	Optional<SellerEntity> findByUserUserIdAndDeletedAtIsNull(UUID userId);

	// 승인 상태별 조회
	Page<SellerEntity> findBySellerStatusAndDeletedAtIsNull(SellerStatus sellerStatus, Pageable pageable);

	// sellerId로 조회 (삭제되지 않은 것)
	Optional<SellerEntity> findBySellerIdAndDeletedAtIsNull(UUID sellerId);

}
