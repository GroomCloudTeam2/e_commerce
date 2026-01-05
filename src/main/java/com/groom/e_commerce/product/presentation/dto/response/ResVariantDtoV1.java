package com.groom.e_commerce.product.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.VariantStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * SKU(Variant) 응답 DTO
 */
@Getter
@Builder
public class ResVariantDtoV1 {

	private UUID variantId;
	private String skuCode;
	private List<UUID> optionValueIds;
	private String optionName;
	private BigDecimal price;
	private Integer stockQuantity;
	private VariantStatus status;

	public static ResVariantDtoV1 from(ProductVariant variant) {
		return ResVariantDtoV1.builder()
			.variantId(variant.getId())
			.skuCode(variant.getSkuCode())
			.optionValueIds(variant.getOptionValueIds())
			.optionName(variant.getOptionName())
			.price(variant.getPrice())
			.stockQuantity(variant.getStockQuantity())
			.status(variant.getStatus())
			.build();
	}
}
