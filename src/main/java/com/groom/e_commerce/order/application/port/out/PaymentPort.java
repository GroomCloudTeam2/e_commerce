package com.groom.e_commerce.order.application.port.out;

import java.util.List;
import java.util.UUID;

public interface PaymentPort {
	/**
	 * 결제 서비스에 부분 취소를 요청합니다.
	 * @param orderId     결제 정보(PaymentKey)를 찾기 위한 주문 식별자
	 * @param amount      환불할 총 금액
	 * @param orderItemIds 취소 대상 상품들의 ID 목록 (PaymentSplit 갱신용)
	 */
	void cancelPayment(UUID orderId, Long amount, List<UUID> orderItemIds);
}
