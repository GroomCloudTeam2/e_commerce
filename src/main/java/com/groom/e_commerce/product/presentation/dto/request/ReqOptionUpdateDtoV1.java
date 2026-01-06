package com.groom.e_commerce.product.presentation.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 옵션 전체 수정 요청 DTO
 * PUT /products/{id}/options
 * 기존 옵션을 모두 삭제하고 새로 생성
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqOptionUpdateDtoV1 {

	@Valid
	@NotNull(message = "옵션 목록은 필수입니다.")
	private List<OptionRequest> options;

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
}
