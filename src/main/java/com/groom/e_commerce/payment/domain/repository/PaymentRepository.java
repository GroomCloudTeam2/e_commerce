package com.groom.e_commerce.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.Payment;

public interface PaymentRepository {

	Payment save(Payment payment);

	Optional<Payment> findById(UUID paymentId);

	Optional<Payment> findByOrderId(UUID orderId);

	boolean existsByOrderId(UUID orderId);

	Optional<Payment> findByPaymentKey(String paymentKey);

	Optional<Payment> findByIdWithLock(UUID paymentId);

	Optional<Payment> findByPaymentKeyWithLock(String paymentKey);
}
