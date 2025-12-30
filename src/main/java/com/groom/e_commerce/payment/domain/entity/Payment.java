package com.groom.e_commerce.payment.domain.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.groom.e_commerce.payment.domain.model.PaymentMethod;
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

@Entity
@Table(
	name = "p_payment",
	indexes = {
		@Index(name = "idx_payment_order_id", columnList = "orderId"),
		@Index(name = "idx_payment_payment_key", columnList = "paymentKey")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_payment_order_id", columnNames = "orderId"),
		@UniqueConstraint(name = "uk_payment_payment_key", columnNames = "paymentKey")
	}
)
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String paymentId;

	@Column(nullable = false, length = 100)
	private String orderId;

	@Column(nullable = false, length = 200)
	private String paymentKey;

	@Column(nullable = false)
	private Long totalAmount;

	@Column(nullable = false)
	private Long approvedAmount;

	@Column(nullable = false)
	private Long canceledAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentMethod method;

	@Column(nullable = true, length = 50)
	private String currency;

	@Column(nullable = true, length = 50)
	private String orderName;

	@Column(nullable = true, length = 50)
	private String customerName;

	private OffsetDateTime requestedAt;
	private OffsetDateTime approvedAt;

	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PaymentCancel> cancels = new ArrayList<>();

	protected Payment() {
	}

	public Payment(String orderId, String paymentKey, Long totalAmount) {
		this.orderId = orderId;
		this.paymentKey = paymentKey;
		this.totalAmount = totalAmount;
		this.approvedAmount = 0L;
		this.canceledAmount = 0L;
		this.status = PaymentStatus.READY;
		this.method = PaymentMethod.UNKNOWN;
	}

	public void markApproved(Long approvedAmount, PaymentMethod method, String currency, String orderName,
		String customerName, OffsetDateTime requestedAt, OffsetDateTime approvedAt) {
		this.approvedAmount = approvedAmount;
		this.status = PaymentStatus.DONE;
		this.method = method == null ? PaymentMethod.UNKNOWN : method;
		this.currency = currency;
		this.orderName = orderName;
		this.customerName = customerName;
		this.requestedAt = requestedAt;
		this.approvedAt = approvedAt;
	}

	public void addCancel(PaymentCancel cancel) {
		cancels.add(cancel);
		cancel.setPayment(this);
		this.canceledAmount += cancel.getCancelAmount();

		if (this.canceledAmount >= this.approvedAmount) {
			this.status = PaymentStatus.CANCELED;
		}
	}

	public boolean isAlreadyDone() {
		return this.status == PaymentStatus.DONE || this.status == PaymentStatus.CANCELED;
	}

	// getters
	public String getPaymentId() {
		return paymentId;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public Long getTotalAmount() {
		return totalAmount;
	}

	public Long getApprovedAmount() {
		return approvedAmount;
	}

	public Long getCanceledAmount() {
		return canceledAmount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public PaymentMethod getMethod() {
		return method;
	}

	public String getCurrency() {
		return currency;
	}

	public String getOrderName() {
		return orderName;
	}

	public String getCustomerName() {
		return customerName;
	}

	public OffsetDateTime getRequestedAt() {
		return requestedAt;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public List<PaymentCancel> getCancels() {
		return cancels;
	}
}
