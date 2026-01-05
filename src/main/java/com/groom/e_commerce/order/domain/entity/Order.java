package com.groom.e_commerce.order.domain.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.order.domain.status.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수: 기본 생성자
@Table(name = "p_order")
public class Order extends BaseEntity { // Audit(생성일시 등) 적용

	@Id
	@GeneratedValue(strategy = GenerationType.UUID) // ✅ UUID 자동 생성 전략
	@Column(name = "order_id")
	private UUID orderId;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Column(name = "buyer_id", nullable = false)
	private UUID buyerId;

	@Column(name = "total_payment_amt", nullable = false)
	private BigInteger totalPaymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> item = new ArrayList<>();

	/* ================= 배송지 스냅샷 (ERD 제약조건 반영) ================= */

	@Column(name = "recipient_name", nullable = false, length = 50)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	@Column(name = "zip_code", nullable = false, length = 10)
	private String zipCode;

	@Column(name = "shipping_address", nullable = false, length = 300)
	private String shippingAddress;

	@Column(name = "shipping_memo", length = 200)
	private String shippingMemo;

	/* ================= 생성자 (Builder 패턴 권장) ================= */
	@Builder
	public Order(UUID buyerId, String orderNumber, BigInteger totalPaymentAmount,
		String recipientName, String recipientPhone, String zipCode,
		String shippingAddress, String shippingMemo) {
		this.buyerId = buyerId;
		this.orderNumber = orderNumber;
		this.totalPaymentAmount = totalPaymentAmount;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.zipCode = zipCode;
		this.shippingAddress = shippingAddress;
		this.shippingMemo = shippingMemo;
		this.status = OrderStatus.PENDING; // 초기 상태 고정
	}

	/* ================= 상태 전이 메서드 ================= */

	// 결제 금액 업데이트
	public void updatePaymentAmount(long totalAmount) {
		this.totalPaymentAmount = BigInteger.valueOf(totalAmount);
	}

	// 결제 완료 (PENDING → PAID)
	public void markPaid() {
		if (this.status != OrderStatus.PENDING) {
			throw new IllegalStateException("결제 대기 상태에서만 결제 완료 처리할 수 있습니다.");
		}
		this.status = OrderStatus.PAID;
	}

	// 배송 시작 (PAID → SHIPPING)
	public void startShipping() {
		// Enum에 정의한 canShip() 활용 (메서드명 일치시킬 것: canStartShipping -> canShip 등)
		if (!this.status.canShip()) {
			throw new IllegalStateException("결제 완료 상태에서만 배송을 시작할 수 있습니다.");
		}
		this.status = OrderStatus.SHIPPING;
	}

	// 주문 취소 (PENDING, PAID → CANCELLED)
	public void cancel() {
		if (!this.status.canCancel()) {
			throw new IllegalStateException("현재 주문 상태에서는 취소할 수 없습니다.");
		}
		this.status = OrderStatus.CANCELLED;

		for (OrderItem orderItem : this.item) {
			orderItem.cancel();
		}
	}

	// 구매 확정 (DELIVERED → CONFIRMED)
	public void confirm() {
		if (!this.status.canConfirm()) {
			throw new IllegalStateException("배송 완료 상태에서만 구매 확정이 가능합니다.");
		}
		this.status = OrderStatus.CONFIRMED;
	}
}
