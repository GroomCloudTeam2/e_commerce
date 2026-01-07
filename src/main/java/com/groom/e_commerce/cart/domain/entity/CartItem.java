package com.groom.e_commerce.cart.domain.entity;

import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.product.application.dto.StockManagement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "p_cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "cart_item_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	// 단일 상품일 경우 null이 허용됩니다.
	@Column(name = "variant_id")
	private UUID variantId;

	@Column(nullable = false)
	private Integer quantity;

	@Builder
	public CartItem(Cart cart, UUID productId, UUID variantId, Integer quantity) {
		this.cart = cart;
		this.productId = productId;
		this.variantId = variantId;
		this.quantity = quantity;
	}
	public StockManagement toStockManagement() {
		return StockManagement.of(this.productId, this.variantId, this.quantity);
	}

	public void updateQuantity(int quantity) {
		this.quantity = quantity;
	}
	public void addQuantity(int count) {
		this.quantity += count;
	}
}
