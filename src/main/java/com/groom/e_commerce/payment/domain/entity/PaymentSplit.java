// src/main/java/com/groom/e_commerce/payment/domain/entity/PaymentSplit.java
package com.groom.e_commerce.payment.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.groom.e_commerce.payment.domain.model.PaymentSplitStatus;

import jakarta.persistence.*;

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
	@Column(name = "split_id", columnDefinition = "uuid")
	private UUID splitId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(name = "order_id", nullable = false, columnDefinition = "uuid")
	private UUID orderId;

	@Column(name = "order_item_id", nullable = false, columnDefinition = "uuid")
	private UUID orderItemId;

	@Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
	private UUID ownerId;

	@Column(name = "item_amount", nullable = false)
	private Long itemAmount;

	@Column(name = "canceled_amount", nullable = false)
	private Long canceledAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentSplitStatus status;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected PaymentSplit() {
	}

	private PaymentSplit(Payment payment, UUID orderId, UUID orderItemId, UUID ownerId, Long itemAmount) {
		this.payment = payment;
		this.orderId = orderId;
		this.orderItemId = orderItemId;
		this.ownerId = ownerId;
		this.itemAmount = itemAmount;
		this.canceledAmount = 0L;
		this.status = PaymentSplitStatus.PAID;
		this.createdAt = OffsetDateTime.now();
	}

	public static PaymentSplit of(Payment payment, UUID orderId, UUID orderItemId, UUID ownerId, Long itemAmount) {
		if (payment == null) throw new IllegalArgumentException("payment must not be null");
		if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
		if (orderItemId == null) throw new IllegalArgumentException("orderItemId must not be null");
		if (ownerId == null) throw new IllegalArgumentException("ownerId must not be null");
		if (itemAmount == null || itemAmount <= 0) throw new IllegalArgumentException("itemAmount must be > 0");

		return new PaymentSplit(payment, orderId, orderItemId, ownerId, itemAmount);
	}

	// getters
	public UUID getSplitId() {
		return splitId;
	}

	public Payment getPayment() {
		return payment;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public UUID getOrderItemId() {
		return orderItemId;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public Long getItemAmount() {
		return itemAmount;
	}

	public Long getCanceledAmount() {
		return canceledAmount;
	}

	public PaymentSplitStatus getStatus() {
		return status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
