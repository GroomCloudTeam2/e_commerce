package com.groom.e_commerce.user.presentation.dto.response.seller;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.user.domain.entity.seller.SellerEntity;
import com.groom.e_commerce.user.domain.entity.seller.SellerStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResSellerApprovalDtoV1 {

	private UUID sellerId;
	private UUID userId;
	private String email;
	private String nickname;
	private String storeName;
	private String businessNo;
	private String approvalRequest;
	private SellerStatus sellerStatus;
	private String rejectedReason;
	private LocalDateTime createdAt;
	private LocalDateTime approvedAt;
	private LocalDateTime rejectedAt;

	// 가게 주소 정보
	private String zipCode;
	private String address;
	private String detailAddress;

	public static ResSellerApprovalDtoV1 from(SellerEntity seller) {
		return ResSellerApprovalDtoV1.builder()
			.sellerId(seller.getSellerId())
			.userId(seller.getUser().getUserId())
			.email(seller.getUser().getEmail())
			.nickname(seller.getUser().getNickname())
			.storeName(seller.getStoreName())
			.businessNo(seller.getBusinessNo())
			.approvalRequest(seller.getApprovalRequest())
			.sellerStatus(seller.getSellerStatus())
			.rejectedReason(seller.getRejectedReason())
			.createdAt(seller.getCreatedAt())
			.approvedAt(seller.getApprovedAt())
			.rejectedAt(seller.getRejectedAt())
			.zipCode(seller.getZipCode())
			.address(seller.getAddress())
			.detailAddress(seller.getDetailAddress())
			.build();
	}
}
