package com.groom.e_commerce.user.presentation.dto.response;

import com.groom.e_commerce.user.domain.entity.UserEntity;
import com.groom.e_commerce.user.domain.entity.UserRole;
import com.groom.e_commerce.user.domain.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResUserDtoV1 {

    private UUID id;
    private String email;
    private String nickname;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResUserDtoV1 from(UserEntity user) {
        return ResUserDtoV1.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
