package com.groom.e_commerce.user.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqUpdateUserDtoV1 {

    private String nickname;
    private String phoneNumber;
    private String password;
    private String address;
}
