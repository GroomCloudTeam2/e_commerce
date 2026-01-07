package com.groom.e_commerce.order.domain.entity; // ✅ 패키지 변경!

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.groom.e_commerce.order.domain.status.OrderStatus;

class OrderDomainTest {

	@Test
	@DisplayName("상품 배송 시작: PAID 상태면 성공하고 시간/상태가 기록되어야 한다.")
	void startShipping_Success() {
		// given
		// 같은 패키지이므로 protected 생성자(new OrderItem()) 호출 가능
		OrderItem item = new OrderItem();
		ReflectionTestUtils.setField(item, "itemStatus", OrderStatus.PAID);

		// when
		item.startShipping();

		// then
		assertThat(item.getItemStatus()).isEqualTo(OrderStatus.SHIPPING);

	}

	@Test
	@DisplayName("상품 배송 시작 실패: PAID가 아니면 예외가 발생해야 한다.")
	void startShipping_Fail() {
		// given
		OrderItem item = new OrderItem();
		ReflectionTestUtils.setField(item, "itemStatus", OrderStatus.PENDING);

		// when & then
		assertThatThrownBy(item::startShipping)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("결제 완료");
	}

	@Test
	@DisplayName("주문 상태 동기화: 상품 중 하나라도 배송 중이면 주문도 배송 중이어야 한다.")
	void syncStatus_Shipping() {
		// given
		Order order = new Order();

		OrderItem item1 = new OrderItem();
		ReflectionTestUtils.setField(item1, "itemStatus", OrderStatus.SHIPPING);

		OrderItem item2 = new OrderItem();
		ReflectionTestUtils.setField(item2, "itemStatus", OrderStatus.PAID);

		// 연관관계 설정
		ReflectionTestUtils.setField(order, "item", List.of(item1, item2));

		// when
		order.syncStatus();

		// then
		assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
	}

	@Test
	@DisplayName("주문 상태 동기화: 모든 상품이 배송 완료되어야 주문도 배송 완료된다.")
	void syncStatus_Delivered() {
		// given
		Order order = new Order();

		OrderItem item1 = new OrderItem();
		ReflectionTestUtils.setField(item1, "itemStatus", OrderStatus.DELIVERED);

		OrderItem item2 = new OrderItem();
		ReflectionTestUtils.setField(item2, "itemStatus", OrderStatus.DELIVERED);

		ReflectionTestUtils.setField(order, "item", List.of(item1, item2));

		// when
		order.syncStatus();

		// then
		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
	}
}
