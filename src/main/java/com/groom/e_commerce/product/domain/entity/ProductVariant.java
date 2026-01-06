package com.groom.e_commerce.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.domain.enums.VariantStatus;

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
@Table(name = "p_product_variant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "variant_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "sku_code", nullable = false, unique = true, length = 50)
	private String skuCode;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "option_value_ids", columnDefinition = "jsonb")
	private List<UUID> optionValueIds;

	@Column(name = "option_name", length = 200)
	private String optionName;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private VariantStatus status;

	@Builder
	public ProductVariant(Product product, String skuCode, List<UUID> optionValueIds,
		String optionName, BigDecimal price, Integer stockQuantity) {
		this.product = product;
		this.skuCode = skuCode;
		this.optionValueIds = optionValueIds;
		this.optionName = optionName;
		this.price = price;
		this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
		this.status = VariantStatus.ON_SALE;
	}

	public void update(String optionName, BigDecimal price, Integer stockQuantity) {
		if (optionName != null) {
			this.optionName = optionName;
		}
		if (price != null) {
			this.price = price;
		}
		if (stockQuantity != null) {
			this.stockQuantity = stockQuantity;
			updateStatusByStock();
		}
	}

	public void updateStatus(VariantStatus status) {
		this.status = status;
	}

	public void discontinue() {
		this.status = VariantStatus.DISCONTINUED;
	}

	public void decreaseStock(int quantity) {
		if (this.stockQuantity < quantity) {
			throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
		}
		this.stockQuantity -= quantity;
		updateStatusByStock();
	}

	public void increaseStock(int quantity) {
		this.stockQuantity += quantity;
		if (this.status == VariantStatus.SOLD_OUT && this.stockQuantity > 0) {
			this.status = VariantStatus.ON_SALE;
		}
	}

	// 재고가 0이되면 자동으로 상태 SOLD_OUT으로 변
	private void updateStatusByStock() {
		if (this.stockQuantity == 0 && this.status == VariantStatus.ON_SALE) {
			this.status = VariantStatus.SOLD_OUT;
		} else if (this.stockQuantity > 0 && this.status == VariantStatus.SOLD_OUT) {
			this.status = VariantStatus.ON_SALE;
		}
	}
}
