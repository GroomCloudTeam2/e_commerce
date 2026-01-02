package com.groom.e_commerce.claim.domain.entity;

import java.util.UUID;

import com.groom.e_commerce.claim.domain.status.ClaimStatus;
import com.groom.e_commerce.claim.domain.status.ClaimType;
import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_claim")
public class Claim extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "log_id")
	private UUID claimId;

	/* ================= 식별 정보 ================= */

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "order_item_id", nullable = false)
	private UUID orderItemId;

	@Column(name = "manager_id")
	private UUID managerId;

	/* ================= 클레임 정보 ================= */

	@Enumerated(EnumType.STRING)
	@Column(name = "claim_type", nullable = false, length = 20)
	private ClaimType claimType;

	@Enumerated(EnumType.STRING)
	@Column(name = "claim_status", nullable = false, length = 30)
	private ClaimStatus claimStatus;

	@Column(name = "prev_status", length = 20)
	private String prevStatus;

	@Column(name = "next_status", length = 20)
	private String nextStatus;

	@Column(name = "reason", nullable = false, length = 200)
	private String reason;

	/* ================= 생성 ================= */

	@Builder
	public Claim(UUID userId,
		UUID orderItemId,
		ClaimType claimType,
		String prevStatus,
		String reason) {

		this.userId = userId;
		this.orderItemId = orderItemId;
		this.claimType = claimType;
		this.prevStatus = prevStatus;
		this.reason = reason;
		this.claimStatus = ClaimStatus.REQUESTED;
	}

	/* ================= 상태 전이 ================= */

	// 관리자 승인
	public void approve(UUID managerId, String nextStatus) {
		if (!this.claimStatus.canApprove()) {
			throw new IllegalStateException("승인할 수 없는 클레임 상태입니다.");
		}
		this.managerId = managerId;
		this.claimStatus = ClaimStatus.APPROVED;
		this.nextStatus = nextStatus;
	}

	// 관리자 거절
	public void reject(UUID managerId, String rejectReason) {
		if (!this.claimStatus.canReject()) {
			throw new IllegalStateException("거절할 수 없는 클레임 상태입니다.");
		}
		this.managerId = managerId;
		this.claimStatus = ClaimStatus.REJECTED;
		this.reason = rejectReason;
	}

	// 처리 완료 (환불/재고 복구 끝)
	public void complete() {
		if (!this.claimStatus.canComplete()) {
			throw new IllegalStateException("완료 처리할 수 없는 상태입니다.");
		}
		this.claimStatus = ClaimStatus.COMPLETED;
	}
}
