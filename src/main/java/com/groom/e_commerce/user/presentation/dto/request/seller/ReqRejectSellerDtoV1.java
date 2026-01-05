package com.groom.e_commerce.user.presentation.dto.request.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqRejectSellerDtoV1 {

	@NotBlank(message = "거절 사유는 필수입니다.")
	private String rejectedReason;
}
