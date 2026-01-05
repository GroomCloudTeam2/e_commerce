// src/main/java/com/groom/e_commerce/payment/domain/repository/PaymentSplitRepository.java
package com.groom.e_commerce.payment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;

public interface PaymentSplitRepository {

	PaymentSplit save(PaymentSplit split);

	List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits);

	Optional<PaymentSplit> findByOrderItemId(UUID orderItemId);

	Optional<PaymentSplit> findByOrderItemIdWithLock(UUID orderItemId);

	boolean existsByOrderItemId(UUID orderItemId);

	List<PaymentSplit> findByOrderId(UUID orderId);
	List<PaymentSplit> findByPaymentId(UUID paymentId);
	List<PaymentSplit> findByOwnerId(UUID ownerId);
}
