// src/main/java/com/groom/e_commerce/payment/domain/repository/PaymentSplitRepository.java
package com.groom.e_commerce.payment.domain.repository;

import java.util.List;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;

public interface PaymentSplitRepository {

	PaymentSplit save(PaymentSplit split);

	List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits);

	List<PaymentSplit> findByOrderId(String orderId);

	List<PaymentSplit> findByOwnerId(String ownerId);

	List<PaymentSplit> findByPaymentId(String paymentId);

	boolean existsByOrderItemId(String orderItemId);
}
