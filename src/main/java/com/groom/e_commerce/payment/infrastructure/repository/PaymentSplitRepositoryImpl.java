// src/main/java/com/groom/e_commerce/payment/infrastructure/repository/PaymentSplitRepositoryImpl.java
package com.groom.e_commerce.payment.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.groom.e_commerce.payment.domain.entity.PaymentSplit;
import com.groom.e_commerce.payment.domain.repository.PaymentSplitRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class PaymentSplitRepositoryImpl implements PaymentSplitRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	public PaymentSplit save(PaymentSplit split) {
		if (split == null) {
			throw new IllegalArgumentException("split must not be null");
		}

		if (split.getSplitId() == null) {
			em.persist(split);
			return split;
		}
		return em.merge(split);
	}

	@Override
	public List<PaymentSplit> saveAll(Iterable<PaymentSplit> splits) {
		if (splits == null) {
			throw new IllegalArgumentException("splits must not be null");
		}

		List<PaymentSplit> list = toList(splits);
		return list.stream().map(this::save).toList();
	}

	private List<PaymentSplit> toList(Iterable<PaymentSplit> it) {
		if (it instanceof List<?> l) {
			@SuppressWarnings("unchecked")
			List<PaymentSplit> casted = (List<PaymentSplit>) l;
			return casted;
		}

		ArrayList<PaymentSplit> list = new ArrayList<>();
		for (PaymentSplit s : it) {
			list.add(s);
		}
		return list;
	}

	@Override
	public Optional<PaymentSplit> findByOrderItemId(UUID orderItemId) {
		if (orderItemId == null) {
			throw new IllegalArgumentException("orderItemId must not be null");
		}

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
		if (orderItemId == null) {
			throw new IllegalArgumentException("orderItemId must not be null");
		}

		// ✅ PESSIMISTIC_WRITE 락으로 동시 취소(중복 환불) 방지
		Query q = em.createQuery(
				"select s from PaymentSplit s where s.orderItemId = :orderItemId",
				PaymentSplit.class
			)
			.setParameter("orderItemId", orderItemId)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE);

		// (선택) 락 대기시간 힌트: DB별 지원 여부 다름
		// - Hibernate: "jakarta.persistence.lock.timeout" (ms)
		// - 지원 안 하면 무시될 수 있음
		q.setHint("jakarta.persistence.lock.timeout", 3000); // 3초

		return q.getResultStream().findFirst();
	}

	@Override
	public boolean existsByOrderItemId(UUID orderItemId) {
		if (orderItemId == null) {
			throw new IllegalArgumentException("orderItemId must not be null");
		}

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
		if (orderId == null) {
			throw new IllegalArgumentException("orderId must not be null");
		}

		return em.createQuery(
				"select s from PaymentSplit s where s.orderId = :orderId",
				PaymentSplit.class
			)
			.setParameter("orderId", orderId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByPaymentId(UUID paymentId) {
		if (paymentId == null) {
			throw new IllegalArgumentException("paymentId must not be null");
		}

		return em.createQuery(
				"select s from PaymentSplit s where s.payment.paymentId = :paymentId",
				PaymentSplit.class
			)
			.setParameter("paymentId", paymentId)
			.getResultList();
	}

	@Override
	public List<PaymentSplit> findByOwnerId(UUID ownerId) {
		if (ownerId == null) {
			throw new IllegalArgumentException("ownerId must not be null");
		}

		return em.createQuery(
				"select s from PaymentSplit s where s.ownerId = :ownerId",
				PaymentSplit.class
			)
			.setParameter("ownerId", ownerId)
			.getResultList();
	}
}
