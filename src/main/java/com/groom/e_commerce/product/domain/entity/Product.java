package com.groom.e_commerce.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.groom.e_commerce.product.domain.enums.ProductStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "product_id")
	private UUID id;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ProductStatus status;

	@Column(name = "has_options", nullable = false)
	private Boolean hasOptions;

	@Column(name = "price", precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_quantity")
	private Integer stockQuantity;

	@Column(name = "suspend_reason", length = 500)
	private String suspendReason;

	@Column(name = "suspended_at")
	private LocalDateTime suspendedAt;

	// 상품이 삭제되면 옵션들도 같이 삭제(Cascade)
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductOption> options = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductVariant> variants = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by")
	private UUID deletedBy;

	@Builder
	public Product(UUID ownerId, Category category, String title, String description,
		String thumbnailUrl, Boolean hasOptions, BigDecimal price, Integer stockQuantity) {
		this.ownerId = ownerId;
		this.category = category;
		this.title = title;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.hasOptions = hasOptions != null ? hasOptions : false;
		this.price = price;
		this.stockQuantity = stockQuantity;
		this.status = ProductStatus.ON_SALE;
	}

	public void update(Category category, String title, String description, String thumbnailUrl,
		BigDecimal price, Integer stockQuantity, ProductStatus status) {
		if (category != null) {
			this.category = category;
		}
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (thumbnailUrl != null) {
			this.thumbnailUrl = thumbnailUrl;
		}
		if (price != null) {
			this.price = price;
		}
		if (stockQuantity != null) {
			this.stockQuantity = stockQuantity;
			syncStatusWithStock();
		}
		// Owner가 변경 가능한 상태: ON_SALE, SOLD_OUT, HIDDEN
		if (status != null && isOwnerAllowedStatus(status)) {
			this.status = status;
		}
	}

	private boolean isOwnerAllowedStatus(ProductStatus status) {
		return status == ProductStatus.ON_SALE
			|| status == ProductStatus.SOLD_OUT
			|| status == ProductStatus.HIDDEN;
	}

	private void syncStatusWithStock() {
		if (this.stockQuantity != null && this.stockQuantity == 0 && this.status == ProductStatus.ON_SALE) {
			this.status = ProductStatus.SOLD_OUT;
		} else if (this.stockQuantity != null && this.stockQuantity > 0 && this.status == ProductStatus.SOLD_OUT) {
			this.status = ProductStatus.ON_SALE;
		}
	}

	public void updateStatus(ProductStatus status) {
		this.status = status;
	}

	// manager에 의한 강제 판매 중지
	public void suspend(String reason) {
		this.status = ProductStatus.SUSPENDED;
		this.suspendReason = reason;
		this.suspendedAt = LocalDateTime.now();
	}

	// 판매 중지 해제
	public void restore() {
		this.status = ProductStatus.ON_SALE;
		this.suspendReason = null;
		this.suspendedAt = null;
	}

	// owner에 의한 상품 삭제
	public void softDelete(UUID deletedBy) {
		this.status = ProductStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deletedBy;
	}

	public boolean isOwnedBy(UUID ownerId) {
		return this.ownerId.equals(ownerId);
	}

	public void addOption(ProductOption option) {
		this.options.add(option);
		this.hasOptions = true;
	}

	public void clearOptions() {
		this.options.clear();
	}

	public void addVariant(ProductVariant variant) {
		this.variants.add(variant);
	}

	// 옵션이 없는 상품의 재고 감소 로직
	public void decreaseStock(int quantity) {
		if (this.stockQuantity == null || this.stockQuantity < quantity) {
			throw new IllegalStateException("재고가 부족합니다.");
		}
		this.stockQuantity -= quantity;
		if (this.stockQuantity == 0) {
			this.status = ProductStatus.SOLD_OUT;
		}
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}
}
