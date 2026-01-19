package com.groom.e_commerce.payment.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
	name = "p_payment_cancel",
	indexes = {
		@Index(name = "idx_payment_cancel_payment_key", columnList = "payment_key"),
		@Index(name = "idx_payment_cancel_payment_id", columnList = "payment_id")
	}
)
public class PaymentCancel {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "cancel_id", nullable = false)
	private UUID cancelId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(name = "payment_key", nullable = false, length = 255)
	private String paymentKey;

	@Column(name = "cancel_amount", nullable = false)
	private Long cancelAmount;

	@Column(name = "canceled_at", nullable = false)
	private OffsetDateTime canceledAt;

	protected PaymentCancel() {
	}

	public PaymentCancel(String paymentKey, Long cancelAmount, OffsetDateTime canceledAt) {
		this.paymentKey = paymentKey;
		this.cancelAmount = cancelAmount;
		this.canceledAt = canceledAt;
	}

	public UUID getCancelId() {
		return cancelId;
	}

	// ===== getters =====

	public Payment getPayment() {
		return payment;
	}

	void setPayment(Payment payment) {
		this.payment = payment;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public Long getCancelAmount() {
		return cancelAmount;
	}

	public OffsetDateTime getCanceledAt() {
		return canceledAt;
	}
}
