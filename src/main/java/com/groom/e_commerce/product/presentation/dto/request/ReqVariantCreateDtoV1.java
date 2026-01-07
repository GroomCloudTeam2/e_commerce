package com.groom.e_commerce.product.presentation.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU(Variant) 추가 요청 DTO
 * POST /products/{id}/variants
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqVariantCreateDtoV1 {

	@Size(max = 50, message = "SKU 코드는 50자 이하여야 합니다.")
	private String skuCode;

	private List<UUID> optionValueIds;

	private String optionName;

	@NotNull(message = "가격은 필수입니다.")
	@PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
	private Long price;

	@NotNull(message = "재고는 필수입니다.")
	@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
	private Integer stockQuantity;
}
