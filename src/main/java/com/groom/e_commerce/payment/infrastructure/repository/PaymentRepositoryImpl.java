package com.groom.e_commerce.payment.infrastructure.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Payment save(Payment payment) {
		if (payment.getPaymentId() == null) {
			entityManager.persist(payment);
			return payment;
		}
		return entityManager.merge(payment);
	}

	@Override
	public Optional<Payment> findById(Long paymentId) {
		return Optional.ofNullable(
			entityManager.find(Payment.class, paymentId)
		);
	}

	@Override
	public Optional<Payment> findByOrderId(String orderId) {
		return entityManager.createQuery(
				"SELECT p FROM Payment p WHERE p.orderId = :orderId",
				Payment.class
			)
			.setParameter("orderId", orderId)
			.getResultStream()
			.findFirst();
	}

	@Override
	public Optional<Payment> findByPaymentKey(String paymentKey) {
		return entityManager.createQuery(
				"SELECT p FROM Payment p WHERE p.paymentKey = :paymentKey",
				Payment.class
			)
			.setParameter("paymentKey", paymentKey)
			.getResultStream()
			.findFirst();
	}

	/**
	 * 결제 승인/취소 시 동시성 제어를 위한 비관적 락 조회
	 */
	@Override
	public Optional<Payment> findByIdWithLock(Long paymentId) {
		return Optional.ofNullable(
			entityManager.find(
				Payment.class,
				paymentId,
				LockModeType.PESSIMISTIC_WRITE
			)
		);
	}

	/**
	 * paymentKey 기준 락 조회 (결제 승인 멱등 처리용)
	 */
	@Override
	public Optional<Payment> findByPaymentKeyWithLock(String paymentKey) {
		return entityManager.createQuery(
				"SELECT p FROM Payment p WHERE p.paymentKey = :paymentKey",
				Payment.class
			)
			.setParameter("paymentKey", paymentKey)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.getResultStream()
			.findFirst();
	}
}
