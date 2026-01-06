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

@AllArgsConstructor // ✅ Builder를 쓰려면 이게 필요합니다.
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

	/**
	 * ERD: amount (결제 금액)
	 * - READY 단계에서도 주문 금액이 확정되므로 NOT NULL로 둠
	 */
	@Column(name = "amount", nullable = false)
	private Long amount;

	/**
	 * ERD: status (READY / PAID / CANCELLED / FAILED)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	/**
	 * ERD: pg_provider (toss 등) NOT NULL
	 */
	@Column(name = "pg_provider", nullable = false, length = 50)
	private String pgProvider;

	/**
	 * ERD: payment_key (결제 완료 후 생성) => READY에서는 없을 수 있으니 nullable 허용
	 * UQ는 유지 (DB에서 NULL 중복 허용이 일반적)
	 */
	@Column(name = "payment_key", nullable = true, length = 255)
	private String paymentKey;

	/**
	 * ERD: approved_at (결제 승인 시각)
	 * ERD가 TIMESTAMP면 LocalDateTime이 더 딱 맞고,
	 * 너 기존 코드 흐름이 OffsetDateTime이면 이것도 OK.
	 */
	@Column(name = "approved_at", nullable = true)
	private OffsetDateTime approvedAt;

	@Builder.Default
	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PaymentCancel> cancels = new ArrayList<>();

	protected Payment() {
	}

	/**
	 * 주문 생성 시 결제(READY) 레코드 생성용 생성자
	 */
	public Payment(UUID orderId, Long amount, String pgProvider) {
		this.orderId = orderId;
		this.amount = amount;
		this.pgProvider = pgProvider;
		this.status = PaymentStatus.READY;
		this.paymentKey = null;
		this.approvedAt = null;
	}

	/**
	 * 결제 성공 처리 (Confirm 이후)
	 * - paymentKey 세팅
	 * - approvedAt 세팅
	 * - status: PAID
	 */
	public void markPaid(String paymentKey, OffsetDateTime approvedAt) {
		this.paymentKey = paymentKey;
		this.approvedAt = approvedAt;
		this.status = PaymentStatus.PAID;
	}

	/**
	 * 결제 실패 처리
	 */
	public void markFailed() {
		this.status = PaymentStatus.FAILED;
	}

	/**
	 * 취소 이력 추가 + 전액 취소면 CANCELLED로 변경
	 *
	 * ⚠️ ERD에 canceled_amount 컬럼이 없으므로
	 * 취소 합계는 cancels 합산으로 계산한다.
	 */
	public void addCancel(PaymentCancel cancel) {
		this.cancels.add(cancel);
		cancel.setPayment(this);

		if (getCanceledAmount() >= this.amount) {
			this.status = PaymentStatus.CANCELLED;
		}
	}

	public long getCanceledAmount() {
		return this.cancels.stream()
			.mapToLong(PaymentCancel::getCancelAmount)
			.sum();
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
