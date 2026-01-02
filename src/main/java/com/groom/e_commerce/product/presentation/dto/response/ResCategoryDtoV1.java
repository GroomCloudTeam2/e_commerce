package com.groom.e_commerce.product.presentation.dto.response;

import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.Category;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResCategoryDtoV1 {

	private UUID id;
	private String name;
	private Integer depth;
	private Integer sortOrder;
	private Boolean isActive;
	private UUID parentId;
	private List<ResCategoryDtoV1> children;

	public static ResCategoryDtoV1 from(Category category) {
		return ResCategoryDtoV1.builder()
			.id(category.getId())
			.name(category.getName())
			.depth(category.getDepth())
			.sortOrder(category.getSortOrder())
			.isActive(category.getIsActive())
			.parentId(category.getParent() != null ? category.getParent().getId() : null)
			.build();
	}

	public static ResCategoryDtoV1 fromWithChildren(Category category) {
		return ResCategoryDtoV1.builder()
			.id(category.getId())
			.name(category.getName())
			.depth(category.getDepth())
			.sortOrder(category.getSortOrder())
			.isActive(category.getIsActive())
			.parentId(category.getParent() != null ? category.getParent().getId() : null)
			.children(category.getChildren().stream()
				.filter(Category::getIsActive)
				.map(ResCategoryDtoV1::fromWithChildren)
				.toList())
			.build();
	}
}
