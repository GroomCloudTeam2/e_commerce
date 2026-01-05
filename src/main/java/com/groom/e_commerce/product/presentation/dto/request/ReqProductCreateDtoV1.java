package com.groom.e_commerce.product.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReqProductCreateDtoV1 {

	@NotNull(message = "카테고리 ID는 필수입니다.")
	private UUID categoryId;

	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
	private String title;

	private String description;

	@Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
	private String thumbnailUrl;

	private Boolean hasOptions;

	@PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
	private BigDecimal price;

	@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
	private Integer stockQuantity;
}
