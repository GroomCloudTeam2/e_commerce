package com.groom.e_commerce.user.presentation.dto.response.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.user.domain.entity.address.AddressEntity;
import com.groom.e_commerce.user.domain.entity.seller.SellerEntity;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.presentation.dto.response.address.ResAddressDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.seller.ResSellerApprovalDtoV1;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResUserDtoV1 {

	private UUID id;
	private String email;
	private String nickname;
	private String phoneNumber;
	private UserRole role;
	private UserStatus status;
	// 기본 배송지 추가
	private ResAddressDtoV1 defaultAddress;
	// --------------
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	// Seller
	private ResSellerApprovalDtoV1 sellerInfo;
	// ------

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

	public static ResUserDtoV1 from(UserEntity user, AddressEntity defaultAddress) {
		return ResUserDtoV1.builder()
			.id(user.getUserId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phoneNumber(user.getPhoneNumber())
			.role(user.getRole())
			.status(user.getStatus())
			.defaultAddress(defaultAddress != null ? ResAddressDtoV1.from(defaultAddress) : null)
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}

	public static ResUserDtoV1 from(UserEntity user, AddressEntity defaultAddress, SellerEntity seller) {
		return ResUserDtoV1.builder()
			.id(user.getUserId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phoneNumber(user.getPhoneNumber())
			.role(user.getRole())
			.status(user.getStatus())
			.defaultAddress(defaultAddress != null ? ResAddressDtoV1.from(defaultAddress) : null)
			.sellerInfo(seller != null ? ResSellerApprovalDtoV1.from(seller) : null)
			.build();
	}
}
