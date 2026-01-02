package com.groom.e_commerce.user.presentation.dto.response;

import java.util.UUID;

import com.groom.e_commerce.user.domain.entity.AddressEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResAddressDtoV1 {

	private UUID id;
	private String zipCode;
	private String address;
	private String detailAddress;
	private Boolean isDefault;
	private String recipient;
	private String recipientPhone;

	public static ResAddressDtoV1 from(AddressEntity address) {
		return ResAddressDtoV1.builder()
			.id(address.getAddressId())
			.zipCode(address.getZipCode())
			.address(address.getAddress())
			.detailAddress(address.getDetailAddress())
			.recipient(address.getRecipient())
			.recipientPhone(address.getRecipientPhone())
			.isDefault(address.getIsDefault())
			.build();
	}

}
