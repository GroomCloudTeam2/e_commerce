// src/main/java/com/groom/e_commerce/payment/domain/entity/PaymentSplit.java
package com.groom.e_commerce.payment.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
	name = "p_payment_split",
	indexes = {
		@Index(name = "idx_split_payment_id", columnList = "payment_id"),
		@Index(name = "idx_split_order_id", columnList = "order_id"),
		@Index(name = "idx_split_owner_id", columnList = "owner_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_split_order_item_id", columnNames = "order_item_id")
	}
)
public class PaymentSplit {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "split_id", length = 36)
	private String splitId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(name = "order_id", nullable = false, length = 100)
	private String orderId;

	@Column(name = "order_item_id", nullable = false, length = 100)
	private String orderItemId;

	@Column(name = "owner_id", nullable = false, length = 100)
	private String ownerId;

	@Column(name = "item_amount", nullable = false)
	private Long itemAmount;

	/**
	 * 지금 단계(승인 시 split 생성)에서는 필요 없으면 지워도 됨.
	 * 나중에 부분취소/환불을 split 기준으로 잡을 때 확장용.
	 */
	@Column(name = "canceled_amount", nullable = false)
	private Long canceledAmount;

	/**
	 * created_at 성격의 스냅샷 타임스탬프(선택)
	 * Audit BaseEntity가 있으면 그걸로 대체 가능.
	 */
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected PaymentSplit() {
	}

	private PaymentSplit(Payment payment, String orderId, String orderItemId, String ownerId, Long itemAmount) {
		this.payment = payment;
		this.orderId = orderId;
		this.orderItemId = orderItemId;
		this.ownerId = ownerId;
		this.itemAmount = itemAmount;
		this.canceledAmount = 0L;
		this.createdAt = OffsetDateTime.now();
	}

	public static PaymentSplit of(Payment payment, String orderId, String orderItemId, String ownerId, Long itemAmount) {
		if (payment == null) throw new IllegalArgumentException("payment must not be null");
		if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId must not be blank");
		if (orderItemId == null || orderItemId.isBlank()) throw new IllegalArgumentException("orderItemId must not be blank");
		if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("ownerId must not be blank");
		if (itemAmount == null || itemAmount <= 0) throw new IllegalArgumentException("itemAmount must be > 0");

		return new PaymentSplit(payment, orderId, orderItemId, ownerId, itemAmount);
	}

	// getters
	public String getSplitId() {
		return splitId;
	}

	public Payment getPayment() {
		return payment;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getOrderItemId() {
		return orderItemId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public Long getItemAmount() {
		return itemAmount;
	}

	public Long getCanceledAmount() {
		return canceledAmount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
