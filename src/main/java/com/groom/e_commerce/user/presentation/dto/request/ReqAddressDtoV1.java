package com.groom.e_commerce.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqAddressDtoV1 {

    @NotBlank(message = "우편번호는 필수입니다.")
    private String zipCode;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @NotBlank(message = "상세주소는 필수입니다.")
    private String detailAddress;

    private Boolean isDefault;
}
