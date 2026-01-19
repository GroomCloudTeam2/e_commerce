package com.groom.e_commerce.payment.domain.model;

public enum PaymentStatus {
	READY,      // 결제 준비(주문 생성 직후)
	PAID,       // 결제 완료
	CANCELLED,  // 전액 취소 완료
	FAILED      // 결제 실패
}
