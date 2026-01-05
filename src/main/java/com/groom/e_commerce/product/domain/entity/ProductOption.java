package com.groom.e_commerce.product.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "p_product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "option_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	// 해당 옵션이 가질 수 있는 값들의 목록
	@OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductOptionValue> optionValues = new ArrayList<>();

	@Builder
	public ProductOption(Product product, String name, Integer sortOrder) {
		this.product = product;
		this.name = name;
		this.sortOrder = sortOrder != null ? sortOrder : 0;
	}

	public void addOptionValue(ProductOptionValue optionValue) {
		this.optionValues.add(optionValue);
	}
}
