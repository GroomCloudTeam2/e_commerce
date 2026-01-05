package com.groom.e_commerce.payment.application.port.out;

import java.util.UUID;

public interface OrderStatePort {
	/**
	 * 주문 상태를 '결제 완료(PAID)'로 변경
	 */
	void payOrder(UUID orderId);
}
