package com.groom.e_commerce.product.presentation.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
	private Long price;

	@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
	private Integer stockQuantity;

	@Valid
	private List<OptionRequest> options;

	@Valid
	private List<VariantRequest> variants;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OptionRequest {

		@NotBlank(message = "옵션명은 필수입니다.")
		@Size(max = 50, message = "옵션명은 50자 이하여야 합니다.")
		private String name;

		private Integer sortOrder;

		@Valid
		@NotNull(message = "옵션값 목록은 필수입니다.")
		@Size(min = 1, message = "옵션값은 최소 1개 이상이어야 합니다.")
		private List<OptionValueRequest> values;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OptionValueRequest {

		@NotBlank(message = "옵션값은 필수입니다.")
		@Size(max = 100, message = "옵션값은 100자 이하여야 합니다.")
		private String value;

		private Integer sortOrder;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VariantRequest {

		private List<Integer> optionValueIndexes;

		@Size(max = 50, message = "SKU 코드는 50자 이하여야 합니다.")
		private String skuCode;

		@NotNull(message = "가격은 필수입니다.")
		@PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
		private Long price;

		@NotNull(message = "재고는 필수입니다.")
		@PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
		private Integer stockQuantity;
	}
}
