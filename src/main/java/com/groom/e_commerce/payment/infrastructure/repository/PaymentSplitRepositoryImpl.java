// src/main/java/com/groom/e_commerce/payment/infrastructure/repository/PaymentSplitRepositoryImpl.java
package com.groom.e_commerce.payment.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;
import com.groom.e_commerce.payment.domain.repository.PaymentSplitRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Repository
public class PaymentSplitRepositoryImpl implements PaymentSplitRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	public PaymentSplit save(PaymentSplit split) {
		if (split.getSplitId() == null) {
			em.persist(split);
			return split;
		}
		return em.merge(split);
	}

	@Override
	public List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits) {
		// 간단 구현: persist/merge 반복
		// (대량 처리 최적화는 나중에 배치/flush로 개선 가능)
		return ((splits instanceof List<PaymentSplit> list) ? list : toList(splits))
			.stream().map(this::save).toList();
	}

	private List<PaymentSplit> toList(Iterable<PaymentSplit> it) {
		java.util.ArrayList<PaymentSplit> list = new java.util.ArrayList<>();
		for (PaymentSplit s : it) list.add(s);
		return list;
	}

	@Override
	public Optional<PaymentSplit> findByOrderItemId(UUID orderItemId) {
		return em.createQuery(
				"select s from PaymentSplit s where s.orderItemId = :orderItemId",
				PaymentSplit.class
			)
			.setParameter("orderItemId", orderItemId)
			.getResultStream()
			.findFirst();
	}

	@Override
	public Optional<PaymentSplit> findByOrderItemIdWithLock(UUID orderItemId) {
		return em.createQuery(
				"select s from PaymentSplit s where s.orderItemId = :orderItemId",
				PaymentSplit.class
			)
			.setParameter("orderItemId", orderItemId)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.getResultStream()
			.findFirst();
	}

	@Override
	public boolean existsByOrderItemId(UUID orderItemId) {
		Long count = em.createQuery(
				"select count(s) from PaymentSplit s where s.orderItemId = :orderItemId",
				Long.class
			)
			.setParameter("orderItemId", orderItemId)
			.getSingleResult();
		return count != null && count > 0;
	}

	@Override
	public List<PaymentSplit> findByOrderId(UUID orderId) {
		return em.createQuery(
				"select s from PaymentSplit s where s.orderId = :orderId",
				PaymentSplit.class
			)
			.setParameter("orderId", orderId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByPaymentId(UUID paymentId) {
		return em.createQuery(
				"select s from PaymentSplit s where s.payment.paymentId = :paymentId",
				PaymentSplit.class
			)
			.setParameter("paymentId", paymentId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByOwnerId(UUID ownerId) {
		return em.createQuery(
				"select s from PaymentSplit s where s.ownerId = :ownerId",
				PaymentSplit.class
			)
			.setParameter("ownerId", ownerId)
			.getResultList();
	}
}
