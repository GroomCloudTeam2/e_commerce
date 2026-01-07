package com.groom.e_commerce.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCategoryCreateDtoV1 {

	@Schema(description = "카테고리명", example = "신규 카테고리")
	@NotBlank(message = "카테고리명은 필수입니다.")
	private String name;

	@Schema(description = "상위 카테고리 ID (최상위일 경우 null)", example = "uuid-parent-id")
	private UUID parentId;

	@Schema(description = "정렬 순서", example = "1")
	@Min(value = 1, message = "정렬 순서는 1 이상이어야 합니다.")
	private Integer sortOrder;

	@Schema(description = "활성화 여부", example = "true")
	private Boolean isActive;
}
