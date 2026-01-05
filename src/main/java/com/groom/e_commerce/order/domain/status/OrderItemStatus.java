package com.groom.e_commerce.order.domain.status;

public enum OrderItemStatus {
	PENDING,            // 결제 대기 (주문 생성 직후)
	PAID,               // 결제 완료 (주문 접수됨 -> 즉시 취소 가능)
	PREPARING,          // [NEW] 배송 준비 중 (송장 출력/포장 중 -> 취소 시 판매자 확인 필요)
	SHIPPING,           // 배송 중 (취소 불가)
	DELIVERED,          // 배송 완료
	CONFIRMED,          // 구매 확정 (정산 가능)
	CANCELLED;          // 취소됨

	// 즉시 취소 가능 여부 (결제 완료 상태까지만 즉시 취소)
	public boolean canCancelImmediately() {
		return this == PENDING || this == PAID;
	}

	// 취소 자체가 가능한지 여부 (배송 시작 전이면 가능)
	// PREPARING 단계는 '취소 요청'은 가능하므로 true로 둠
	public boolean isCancelable() {
		return this == PENDING || this == PAID || this == PREPARING;
	}
}
