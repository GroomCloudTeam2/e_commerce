package com.groom.e_commerce.product.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 상품 목록 조회 응답 DTO (구매자용)
 */
@Getter
@Builder
public class ResProductSearchDtoV1 {

	private UUID productId;
	private String title;
	private String thumbnailUrl;
	private ProductStatus status;
	private BigDecimal minPrice;
	private BigDecimal maxPrice;
	private Double avgRating;
	private Integer reviewCount;
	private String categoryName;
	private String ownerStoreName;

	public static ResProductSearchDtoV1 from(Product product) {
		// 옵션이 있는 경우 variant 가격 범위 계산
		BigDecimal minPrice = product.getPrice();
		BigDecimal maxPrice = product.getPrice();

		if (product.getHasOptions() && product.getVariants() != null && !product.getVariants().isEmpty()) {
			minPrice = product.getVariants().stream()
				.map(v -> v.getPrice())
				.min(BigDecimal::compareTo)
				.orElse(product.getPrice());
			maxPrice = product.getVariants().stream()
				.map(v -> v.getPrice())
				.max(BigDecimal::compareTo)
				.orElse(product.getPrice());
		}

		return ResProductSearchDtoV1.builder()
			.productId(product.getId())
			.title(product.getTitle())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.avgRating(null) // TODO: Review 도메인 연동 후 구현
			.reviewCount(null) // TODO: Review 도메인 연동 후 구현
			.categoryName(product.getCategory().getName())
			.ownerStoreName(null) // TODO: User 도메인 연동 후 구현
			.build();
	}

	public static ResProductSearchDtoV1 from(Product product, Double avgRating, Integer reviewCount, String ownerStoreName) {
		BigDecimal minPrice = product.getPrice();
		BigDecimal maxPrice = product.getPrice();

		if (product.getHasOptions() && product.getVariants() != null && !product.getVariants().isEmpty()) {
			minPrice = product.getVariants().stream()
				.map(v -> v.getPrice())
				.min(BigDecimal::compareTo)
				.orElse(product.getPrice());
			maxPrice = product.getVariants().stream()
				.map(v -> v.getPrice())
				.max(BigDecimal::compareTo)
				.orElse(product.getPrice());
		}

		return ResProductSearchDtoV1.builder()
			.productId(product.getId())
			.title(product.getTitle())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.avgRating(avgRating)
			.reviewCount(reviewCount)
			.categoryName(product.getCategory().getName())
			.ownerStoreName(ownerStoreName)
			.build();
	}
}
