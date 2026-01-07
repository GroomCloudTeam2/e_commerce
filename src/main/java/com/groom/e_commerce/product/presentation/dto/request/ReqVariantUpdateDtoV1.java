package com.groom.e_commerce.product.presentation.dto.request;

import java.math.BigDecimal;

import com.groom.e_commerce.product.domain.enums.VariantStatus;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU(Variant) 수정 요청 DTO
 * PATCH /products/{id}/variants/{variantId}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqVariantUpdateDtoV1 {

	@Size(max = 200, message = "옵션명은 200자 이하여야 합니다.")
	private String optionName;

	@PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
	private Long price;

	@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
	private Integer stockQuantity;

	private VariantStatus status;
}
