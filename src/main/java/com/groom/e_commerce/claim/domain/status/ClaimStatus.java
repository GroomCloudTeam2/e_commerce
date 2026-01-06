package com.groom.e_commerce.claim.domain.status;

public enum ClaimStatus {
	REQUESTED,   // 사용자 요청
	APPROVED,    // 관리자 승인
	REJECTED,    // 관리자 거절
	PROCESSING,  // 환불/교환 처리 중
	COMPLETED;   // 처리 완료

	public boolean canApprove() {
		return this == REQUESTED;
	}

	public boolean canReject() {
		return this == REQUESTED;
	}

	public boolean canComplete() {
		return this == APPROVED || this == PROCESSING;
	}
}
