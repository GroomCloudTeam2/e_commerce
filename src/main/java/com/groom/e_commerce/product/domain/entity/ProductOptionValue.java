package com.groom.e_commerce.product.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

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
@Table(name = "p_product_option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionValue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "option_value_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "option_id", nullable = false)
	private ProductOption option;

	@Column(name = "value", nullable = false, length = 100)
	private String value;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Builder
	public ProductOptionValue(ProductOption option, String value, Integer sortOrder) {
		this.option = option;
		this.value = value;
		this.sortOrder = sortOrder != null ? sortOrder : 0;
	}
}
