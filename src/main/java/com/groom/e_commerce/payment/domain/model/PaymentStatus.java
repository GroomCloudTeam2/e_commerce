package com.groom.e_commerce.payment.domain.model;

public enum PaymentStatus {
	READY,   // 결제 준비(주문 생성 후)
	DONE,    // 결제 승인 완료
	CANCELED // 전액 취소 완료(부분취소는 DONE 유지 + cancelAmount 누적)
}
