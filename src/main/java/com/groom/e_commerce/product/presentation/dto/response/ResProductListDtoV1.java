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

	private UUID productId;
	private String title;
	private String thumbnailUrl;
	private ProductStatus status;
	private BigDecimal price;
	private Integer stockQuantity;
	private Integer salesCount;
	private String categoryName;
	private LocalDateTime createdAt;

	public static ResProductListDtoV1 from(Product product) {
		// stockQuantity: 옵션이 있으면 variant 재고 합계, 없으면 기본 재고
		Integer stockQuantity = product.getStockQuantity();
		if (Boolean.TRUE.equals(product.getHasOptions())
			&& product.getVariants() != null
			&& !product.getVariants().isEmpty()) {
			stockQuantity = product.getVariants().stream()
				.mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
				.sum();
		}

		return ResProductListDtoV1.builder()
			.productId(product.getId())
			.title(product.getTitle())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.price(product.getPrice())
			.stockQuantity(stockQuantity)
			.salesCount(null) // TODO: Order 도메인 연동 후 구현
			.categoryName(product.getCategory().getName())
			.createdAt(product.getCreatedAt())
			.build();
	}
}
