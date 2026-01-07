// src/main/java/com/groom/e_commerce/payment/infrastructure/stub/StubOrderQueryAdapter.java
package com.groom.e_commerce.payment.infrastructure.stub;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;

@Component
public class StubOrderQueryAdapter implements OrderQueryPort {

	@Override
	public OrderSummary getOrderSummary(UUID orderId) {
		// TODO: order 도메인 붙으면 진짜 조회로 교체
		return new OrderSummary(
			orderId,
			55000L,              // 임시 금액
			"TEST-ORDER-001",    // 임시 주문번호
			"TEST_CUSTOMER"      // 임시 고객명
		);
	}

	@Override
	public List<OrderItemSnapshot> getOrderItems(UUID orderId) {
		// TODO: order 도메인 붙으면 진짜 조회로 교체
		// 임시로 주문상품 2개(멀티셀러) 반환
		return List.of(
			new OrderItemSnapshot(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				30000L
			),
			new OrderItemSnapshot(
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				25000L
			)
		);
	}
}
