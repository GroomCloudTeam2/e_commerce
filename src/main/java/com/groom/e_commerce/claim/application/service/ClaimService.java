package com.groom.e_commerce.claim.application.service;

import com.groom.e_commerce.claim.domain.entity.Claim;
import com.groom.e_commerce.claim.domain.repository.ClaimRepository;
import com.groom.e_commerce.claim.presentation.dto.ClaimDto;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {

	private final ClaimRepository claimRepository;
	private final OrderItemRepository orderItemRepository;

	/**
	 * 사용자: 클레임 요청
	 */
	@Transactional
	public UUID createClaim(UUID userId, ClaimDto.Request request) {
		// 1. 주문 상품 조회 및 검증
		OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 상품입니다."));

		// 2. 본인 주문 확인 로직 (주문의 구매자와 요청자 ID 비교)
		if (!orderItem.getOrder().getBuyerId().equals(userId)) {
			throw new IllegalArgumentException("본인의 주문 상품만 클레임을 요청할 수 있습니다.");
		}

		// 3. 주문 상태 스냅샷
		String currentStatus = orderItem.getItemStatus().name();

		// 4. 클레임 생성
		Claim claim = Claim.builder()
			.userId(userId)
			.orderItemId(request.getOrderItemId())
			.claimType(request.getClaimType())
			.reason(request.getReason())
			.prevStatus(currentStatus)
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
		// 예: if (claim.getClaimType() == ClaimType.CANCEL) orderItem.cancel();
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
