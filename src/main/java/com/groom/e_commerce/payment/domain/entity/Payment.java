package com.groom.e_commerce.payment.domain.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.model.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
@Entity
@Table(
	name = "p_payment",
	indexes = {
		@Index(name = "idx_payment_order_id", columnList = "order_id"),
		@Index(name = "idx_payment_payment_key", columnList = "payment_key")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id"),
		@UniqueConstraint(name = "uk_payment_payment_key", columnNames = "payment_key")
	}
)
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "pg_provider", nullable = false, length = 50)
	private String pgProvider;

	@Column(name = "payment_key", nullable = true, length = 255)
	private String paymentKey;

	@Column(name = "approved_at", nullable = true)
	private OffsetDateTime approvedAt;

	@Builder.Default
	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PaymentCancel> cancels = new ArrayList<>();

	protected Payment() {
	}

	public Payment(UUID orderId, Long amount, String pgProvider) {
		this.orderId = orderId;
		this.amount = amount;
		this.pgProvider = pgProvider;
		this.status = PaymentStatus.READY;
		this.paymentKey = null;
		this.approvedAt = null;
	}

	public void markPaid(String paymentKey, OffsetDateTime approvedAt) {
		this.paymentKey = paymentKey;
		this.approvedAt = approvedAt;
		this.status = PaymentStatus.PAID;
	}

	public void markFailed() {
		this.status = PaymentStatus.FAILED;
	}

	/**
	 * 취소 이력 추가 + 전액 취소면 CANCELLED로 변경
	 * (취소 합계는 cancels의 cancelAmount 합산으로 계산)
	 */
	public void addCancel(PaymentCancel cancel) {
		this.cancels.add(cancel);
		cancel.setPayment(this);

		if (getCanceledAmount() >= this.amount) {
			this.status = PaymentStatus.CANCELLED;
		}
	}

	public long getCanceledAmount() {
		if (this.cancels == null) return 0L;
		return this.cancels.stream().mapToLong(PaymentCancel::getCancelAmount).sum();
	}

	public boolean isAlreadyPaid() {
		return this.status == PaymentStatus.PAID;
	}

	public boolean isAlreadyCancelled() {
		return this.status == PaymentStatus.CANCELLED;
	}

	// ===== getters =====

	public UUID getPaymentId() {
		return paymentId;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public Long getAmount() {
		return amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public String getPgProvider() {
		return pgProvider;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public List<PaymentCancel> getCancels() {
		return cancels;
	}
}
