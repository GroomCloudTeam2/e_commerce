package com.groom.e_commerce.product.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "p_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "category_id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@OneToMany(mappedBy = "parent")
	private List<Category> children = new ArrayList<>();

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "depth", nullable = false)
	private Integer depth;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Category(Category parent, String name, Integer depth, Integer sortOrder,
		Boolean isActive) {
		this.parent = parent;
		this.name = name;
		this.depth = depth;
		this.sortOrder = sortOrder;
		this.isActive = isActive != null ? isActive : true;
	}

	public void update(String name, Integer sortOrder, Boolean isActive) {
		if (name != null) {
			this.name = name;
		}
		if (sortOrder != null) {
			this.sortOrder = sortOrder;
		}
		if (isActive != null) {
			this.isActive = isActive;
		}
	}

	// 최상위 카테고리인지 확인
	public boolean isRoot() {
		return this.parent == null;
	}
}
