package com.groom.e_commerce.product.presentation.dto.response;

import com.groom.e_commerce.product.domain.entity.Category;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

	private UUID id;
	private String name;
	private Integer depth;
	private Integer sortOrder;
	private Boolean isActive;
	private UUID parentId;
	private List<CategoryResponse> children;

	public static CategoryResponse from(Category category) {
		return CategoryResponse.builder()
			.id(category.getId())
			.name(category.getName())
			.depth(category.getDepth())
			.sortOrder(category.getSortOrder())
			.isActive(category.getIsActive())
			.parentId(category.getParent() != null ? category.getParent().getId() : null)
			.build();
	}

	public static CategoryResponse fromWithChildren(Category category) {
		return CategoryResponse.builder()
			.id(category.getId())
			.name(category.getName())
			.depth(category.getDepth())
			.sortOrder(category.getSortOrder())
			.isActive(category.getIsActive())
			.parentId(category.getParent() != null ? category.getParent().getId() : null)
			.children(category.getChildren().stream()
				.filter(Category::getIsActive)
				.map(CategoryResponse::fromWithChildren)
				.toList())
			.build();
	}
}
