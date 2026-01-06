package com.groom.e_commerce.payment.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;

import jakarta.persistence.LockModeType;

public interface PaymentSplitJpaRepository extends JpaRepository<PaymentSplit, UUID> {

	Optional<PaymentSplit> findByOrderItemId(UUID orderItemId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select ps from PaymentSplit ps where ps.orderItemId = :orderItemId")
	Optional<PaymentSplit> findByOrderItemIdWithLock(@Param("orderItemId") UUID orderItemId);
}
