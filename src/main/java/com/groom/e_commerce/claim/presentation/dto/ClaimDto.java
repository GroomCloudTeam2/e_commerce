package com.groom.e_commerce.claim.presentation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.claim.domain.entity.Claim;
import com.groom.e_commerce.claim.domain.status.ClaimStatus;
import com.groom.e_commerce.claim.domain.status.ClaimType;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ClaimDto {

	// 1. 사용자 요청 DTO
	@Getter
	@NoArgsConstructor
	public static class Request {
		private UUID orderItemId;
		private ClaimType claimType;
		private String reason;
	}

	// 2. 관리자 거절 DTO
	@Getter
	@NoArgsConstructor
	public static class RejectRequest {
		private String rejectReason;
	}

	// 3. 응답 DTO
	@Getter
	@Builder
	public static class Response {
		private UUID claimId;
		private UUID userId;
		private UUID orderItemId;
		private ClaimType claimType;
		private ClaimStatus claimStatus;
		private String reason;
		private String rejectReason; // 거절 사유 포함
		private LocalDateTime createdAt;

		// Entity -> DTO 변환 메서드
		public static Response from(Claim claim) {
			return Response.builder()
				.claimId(claim.getClaimId())
				.userId(claim.getUserId())
				.orderItemId(claim.getOrderItemId())
				.claimType(claim.getClaimType())
				.claimStatus(claim.getClaimStatus())
				.reason(claim.getReason())
				.rejectReason(claim.getRejectReason()) // Getter 필요
				.createdAt(claim.getCreatedAt()) // BaseEntity Getter 필요
				.build();
		}
	}
}
