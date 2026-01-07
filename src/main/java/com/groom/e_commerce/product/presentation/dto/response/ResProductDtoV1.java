package com.groom.e_commerce.product.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResProductDtoV1 {

	private UUID id;
	private UUID ownerId;
	private UUID categoryId;
	private String categoryName;
	private String title;
	private String description;
	private String thumbnailUrl;
	private ProductStatus status;
	private Boolean hasOptions;
	private Long price;
	private Integer stockQuantity;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static ResProductDtoV1 from(Product product) {
		return ResProductDtoV1.builder()
			.id(product.getId())
			.ownerId(product.getOwnerId())
			.categoryId(product.getCategory().getId())
			.categoryName(product.getCategory().getName())
			.title(product.getTitle())
			.description(product.getDescription())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.hasOptions(product.getHasOptions())
			.price(product.getPrice())
			.stockQuantity(product.getStockQuantity())
			.createdAt(product.getCreatedAt())
			.updatedAt(product.getUpdatedAt())
			.build();
	}
}
