package com.groom.e_commerce.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqProductSuspendDtoV1 {

	@Schema(description = "정지 사유", example = "부적절한 상품 정보 포함")
	@NotBlank(message = "정지 사유는 필수입니다.")
	private String reason;
}
