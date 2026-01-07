package com.groom.e_commerce.claim.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.claim.application.service.ClaimService;
import com.groom.e_commerce.claim.presentation.dto.ClaimDto;
import com.groom.e_commerce.global.infrastructure.config.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Claim", description = "Claim 관련 API")
@RestController
@RequiredArgsConstructor
public class ClaimController {

	private final ClaimService claimService;

	// 사용자: 클레임 요청
	@Operation(summary = "클레임 요청", description = "사용자가 주문 상품에 대해 취소/반품/교환을 요청합니다.")
	@PostMapping("/claims")
	public ResponseEntity<UUID> requestClaim(
		@AuthenticationPrincipal CustomUserDetails user, // 인증된 유저 정보
		@RequestBody ClaimDto.Request request) {

		UUID claimId = claimService.createClaim(user.getUserId(), request);

		return ResponseEntity.ok(claimId);
	}

	@Operation(summary = "내 클레임 목록 조회", description = "로그인한 사용자의 클레임 신청 내역을 조회합니다.")
	@GetMapping("/claims")
	public ResponseEntity<List<ClaimDto>> getMyClaims(@AuthenticationPrincipal CustomUserDetails user) {
		// return ResponseEntity.ok(service.getMyClaims(user.getUserId()));
		return ResponseEntity.ok(List.of()); // 임시 리턴
	}

	@Operation(summary = "관리자용 클레임 목록 조회", description = "모든 사용자의 클레임 요청을 페이징하여 조회합니다.")
	@GetMapping("/admin/claims")
	public ResponseEntity<Page<ClaimDto>> getAllClaims(Pageable pageable) {
		// return ResponseEntity.ok(service.getAllClaims(pageable));
		return ResponseEntity.ok(null);
	}

	// 관리자: 승인
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/admin/claims/{claimId}/approve")
	public ResponseEntity<Void> approveClaim(
		@AuthenticationPrincipal CustomUserDetails user,
		@PathVariable UUID claimId) {

		claimService.approveClaim(user.getUserId(), claimId);
		return ResponseEntity.ok().build();
	}

	// 관리자: 거절
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/admin/claims/{claimId}/reject")
	public ResponseEntity<Void> rejectClaim(
		@AuthenticationPrincipal CustomUserDetails user,
		@PathVariable UUID claimId,
		@RequestBody ClaimDto.RejectRequest request) {

		claimService.rejectClaim(user.getUserId(), claimId, request.getRejectReason());
		return ResponseEntity.ok().build();
	}
}
