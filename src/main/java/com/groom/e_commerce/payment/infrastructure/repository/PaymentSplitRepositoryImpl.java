// src/main/java/com/groom/e_commerce/payment/infrastructure/repository/PaymentSplitRepositoryImpl.java
package com.groom.e_commerce.payment.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;
import com.groom.e_commerce.payment.domain.repository.PaymentSplitRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PaymentSplitRepositoryImpl implements PaymentSplitRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public PaymentSplit save(PaymentSplit split) {
		if (split.getSplitId() == null) {
			entityManager.persist(split);
			return split;
		}
		return entityManager.merge(split);
	}

	@Override
	public List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits) {
		for (PaymentSplit split : splits) {
			if (split.getSplitId() == null) {
				entityManager.persist(split);
			} else {
				entityManager.merge(split);
			}
		}
		return java.util.stream.StreamSupport.stream(splits.spliterator(), false).toList();
	}

	@Override
	public List<PaymentSplit> findByOrderId(UUID orderId) {
		return entityManager.createQuery(
				"SELECT ps FROM PaymentSplit ps WHERE ps.orderId = :orderId",
				PaymentSplit.class
			)
			.setParameter("orderId", orderId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByOwnerId(UUID ownerId) {
		return entityManager.createQuery(
				"SELECT ps FROM PaymentSplit ps WHERE ps.ownerId = :ownerId",
				PaymentSplit.class
			)
			.setParameter("ownerId", ownerId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByPaymentId(UUID paymentId) {
		return entityManager.createQuery(
				"SELECT ps FROM PaymentSplit ps WHERE ps.payment.paymentId = :paymentId",
				PaymentSplit.class
			)
			.setParameter("paymentId", paymentId)
			.getResultList();
	}

	@Override
	public boolean existsByOrderItemId(UUID orderItemId) {
		Long count = entityManager.createQuery(
				"SELECT COUNT(ps) FROM PaymentSplit ps WHERE ps.orderItemId = :orderItemId",
				Long.class
			)
			.setParameter("orderItemId", orderItemId)
			.getSingleResult();
		return count != null && count > 0;
	}
}
