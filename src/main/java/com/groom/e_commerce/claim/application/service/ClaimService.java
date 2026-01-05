package com.groom.e_commerce.claim.application.service;

import com.groom.e_commerce.claim.domain.entity.Claim;
import com.groom.e_commerce.claim.domain.repository.ClaimRepository;
import com.groom.e_commerce.claim.presentation.dto.ClaimDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {

	private final ClaimRepository claimRepository;

	// TODO: Order 모듈과 통신하기 위한 Port(Interface) 혹은 FeignClient 필요
	// private final OrderProvider orderProvider;

	/**
	 * 사용자: 클레임 요청
	 */
	@Transactional
	public UUID createClaim(UUID userId, ClaimDto.Request request) {
		// 1. 주문 상품 검증 (실제로는 Order 서비스 조회 필요)
		// OrderItemInfo orderItem = orderProvider.getOrderItem(request.getOrderItemId());

		// 2. 본인 주문 확인 로직
		// if (!orderItem.getUserId().equals(userId)) throw new CustomException(...);

		// 3. 임시 주문 상태 스냅샷 (예: 현재 배송완료 상태)
		String currentOrderStatus = "DELIVERED"; // orderItem.getStatus();

		// 4. 클레임 생성
		Claim claim = Claim.builder()
			.userId(userId)
			.orderItemId(request.getOrderItemId())
			.claimType(request.getClaimType())
			.reason(request.getReason())
			.prevStatus(currentOrderStatus)
			.build();

		return claimRepository.save(claim).getClaimId();
	}

	/**
	 * 관리자: 클레임 승인
	 */
	@Transactional
	public void approveClaim(UUID managerId, UUID claimId) {
		Claim claim = claimRepository.findById(claimId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클레임입니다."));

		// 클레임 타입에 따라 변경될 주문 상태 결정
		String nextStatus = switch (claim.getClaimType()) {
			case CANCEL -> "CANCELLED";
			case RETURN -> "RETURN_REQUESTED";
			case EXCHANGE -> "EXCHANGE_REQUESTED";
		};

		claim.approve(managerId, nextStatus);

		// TODO: Order 서비스에 상태 변경 이벤트 발행 (Kafka or 내부 호출)
	}

	/**
	 * 관리자: 클레임 거절
	 */
	@Transactional
	public void rejectClaim(UUID managerId, UUID claimId, String rejectReason) {
		Claim claim = claimRepository.findById(claimId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클레임입니다."));

		claim.reject(managerId, rejectReason);
	}

	// 조회 로직(getClaim 등) 추가 구현 필요...
}
