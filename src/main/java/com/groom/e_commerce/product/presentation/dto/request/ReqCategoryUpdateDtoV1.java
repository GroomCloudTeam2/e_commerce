package com.groom.e_commerce.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCategoryUpdateDtoV1 {

	@Schema(description = "변경할 카테고리명", example = "수정할 이름")
	private String name;

	@Schema(description = "변경할 정렬 순서", example = "5")
	@Min(value = 1, message = "정렬 순서는 1 이상이어야 합니다.")
	private Integer sortOrder;

	@Schema(description = "변경할 활성화 여부", example = "false")
	private Boolean isActive;
}
