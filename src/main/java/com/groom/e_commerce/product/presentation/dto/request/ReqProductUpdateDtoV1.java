package com.groom.e_commerce.product.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReqProductUpdateDtoV1 {

	private UUID categoryId;

	@Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
	private String title;

	private String description;

	@Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
	private String thumbnailUrl;

	@PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
	private BigDecimal price;

	@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
	private Integer stockQuantity;
}
