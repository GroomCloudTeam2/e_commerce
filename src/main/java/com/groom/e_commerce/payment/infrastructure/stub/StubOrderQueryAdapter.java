package com.groom.e_commerce.payment.infrastructure.stub;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;

@Component
public class StubOrderQueryAdapter implements OrderQueryPort {

	@Override
	public OrderSummary getOrderSummary(UUID orderId) {
		// TODO: order 도메인 붙으면 진짜 조회로 교체
		return new OrderSummary(
			orderId,
			45000L,              // 임시 금액
			"TEST-ORDER-001",    // 임시 주문번호
			"TEST_CUSTOMER"      // 임시 고객명
		);
	}
}
