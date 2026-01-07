package com.groom.e_commerce.product.application.dto;

import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductVariant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductCartInfo {

	private UUID productId;
	private UUID ownerId;
	private UUID variantId;      // 옵션이 없는 경우 null
	private String productName;
	private String optionName;   // 옵션이 없는 경우 null (예: "Red / L")
	private String thumbnailUrl;
	private Long price;    // 옵션 유무에 따른 최종 단가
	private Integer stockQuantity; // 현재 시점의 실시간 재고
	private boolean isAvailable;

	public static ProductCartInfo from(Product product, ProductVariant variant, boolean isAvailable) {
		return ProductCartInfo.builder()
			.productId(product.getId())
			.ownerId(product.getOwnerId())
			.variantId(variant != null ? variant.getId() : null)
			.productName(product.getTitle())
			.optionName(variant != null ? variant.getOptionName() : null)
			.thumbnailUrl(product.getThumbnailUrl())
			.price(variant != null ? variant.getPrice() : product.getPrice())
			.stockQuantity(variant != null ? variant.getStockQuantity() : product.getStockQuantity())
			.isAvailable(isAvailable)
			.build();
	}
}
