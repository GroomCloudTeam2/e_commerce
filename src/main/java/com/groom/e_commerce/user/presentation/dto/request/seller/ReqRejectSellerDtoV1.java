package com.groom.e_commerce.user.presentation.dto.request.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class ReqRejectSellerDtoV1 {

	@NotBlank(message = "거절 사유는 필수입니다.")
	private String rejectedReason;
}
