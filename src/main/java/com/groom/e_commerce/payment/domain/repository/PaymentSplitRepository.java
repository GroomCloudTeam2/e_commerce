// src/main/java/com/groom/e_commerce/payment/domain/repository/PaymentSplitRepository.java
package com.groom.e_commerce.payment.domain.repository;

import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;

public interface PaymentSplitRepository {

	PaymentSplit save(PaymentSplit split);

	List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits);

	List<PaymentSplit> findByOrderId(UUID orderId);

	List<PaymentSplit> findByOwnerId(UUID ownerId);

	List<PaymentSplit> findByPaymentId(UUID paymentId);

	boolean existsByOrderItemId(UUID orderItemId);
}
