package com.groom.e_commerce.order.domain.entity;

import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.order.domain.status.OrderStatus;
import com.groom.e_commerce.product.application.dto.StockManagement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_order_item")
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "order_item_id")
	private UUID orderItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "variant_id", nullable = false)
	private UUID variantId;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	// --- 스냅샷 ---
	@Column(name = "product_title", nullable = false, length = 200)
	private String productTitle;

	@Column(name = "product_thumbnail", length = 500)
	private String productThumbnail;

	@Column(name = "option_name", length = 200)
	private String optionName;

	@Column(name = "unit_price", nullable = false)
	private Long unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "subtotal", nullable = false)
	private Long subtotal;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_status", nullable = false, length = 20)
	private OrderStatus itemStatus;

	@Builder
	public OrderItem(Order order, UUID productId, UUID variantId, UUID ownerId,
		String productTitle, String productThumbnail, String optionName,
		Long unitPrice, Integer quantity) {
		this.order = order;
		this.productId = productId;
		this.variantId = variantId;
		this.ownerId = ownerId;
		this.productTitle = productTitle;
		this.productThumbnail = productThumbnail;
		this.optionName = optionName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotal = unitPrice*quantity;
		this.itemStatus = OrderStatus.PENDING;
	}
	public StockManagement toStockManagement() {
		return StockManagement.of(this.productId, this.variantId, this.quantity);
	}


	public void cancel() {
		if (this.itemStatus == OrderStatus.SHIPPING || this.itemStatus == OrderStatus.DELIVERED) {
			throw new IllegalStateException("이미 배송된 상품은 취소할 수 없습니다.");
		}
		this.itemStatus = OrderStatus.CANCELLED;
	}

	// 배송 시작 처리
	public void startShipping() {
		if (this.itemStatus != OrderStatus.PAID) {
			throw new IllegalStateException("배송 시작은 '결제 완료' 또는 '상품 준비 중' 상태에서만 가능합니다. 현재 상태: " + this.itemStatus);
		}
		this.itemStatus = OrderStatus.SHIPPING;
	}

	// 배송 완료 처리
	public void completeDelivery() {
		if (this.itemStatus != OrderStatus.SHIPPING) {
			throw new IllegalStateException("배송 완료는 '배송 중' 상태에서만 가능합니다. 현재 상태: " + this.itemStatus);
		}
		this.itemStatus = OrderStatus.DELIVERED;
	}

	// 1. 취소 가능 검증
	public void validateCancelable() {
		// 배송 중, 배송 완료, 구매 확정 상태면 에러
		if (this.itemStatus == OrderStatus.SHIPPING ||
			this.itemStatus == OrderStatus.DELIVERED ||
			this.itemStatus == OrderStatus.CONFIRMED) {
			throw new IllegalStateException("이미 배송이 시작되었거나 완료된 상품은 취소할 수 없습니다.");
		}
		if (this.itemStatus == OrderStatus.CANCELLED) {
			throw new IllegalStateException("이미 취소된 상품입니다.");
		}
	}

	// 2. 환불 금액 반환
	public Long getRefundAmount() {
		return this.subtotal;
	}

}
