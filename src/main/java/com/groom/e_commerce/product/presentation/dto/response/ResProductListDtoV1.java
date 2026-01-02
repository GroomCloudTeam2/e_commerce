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
public class ResProductListDtoV1 {

	private UUID id;
	private String title;
	private String thumbnailUrl;
	private ProductStatus status;
	private BigDecimal price;
	private Integer stockQuantity;
	private String categoryName;
	private LocalDateTime createdAt;

	public static ResProductListDtoV1 from(Product product) {
		return ResProductListDtoV1.builder()
			.id(product.getId())
			.title(product.getTitle())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.price(product.getPrice())
			.stockQuantity(product.getStockQuantity())
			.categoryName(product.getCategory().getName())
			.createdAt(product.getCreatedAt())
			.build();
	}
}