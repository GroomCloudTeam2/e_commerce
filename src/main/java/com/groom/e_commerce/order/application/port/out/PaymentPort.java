package com.groom.e_commerce.order.application.port.out;

import java.util.List;
import java.util.UUID;

public interface PaymentPort {
	/**
	 * 결제 취소 요청
	 *
	 * @param orderId     주문 ID
	 * @param cancelAmount      환불할 총 금액
	 * @param orderItemIds 취소된 주문 상품 ID 목록
	 */
	void cancelPayment(UUID orderId, Long cancelAmount, List<UUID> orderItemIds);
}
