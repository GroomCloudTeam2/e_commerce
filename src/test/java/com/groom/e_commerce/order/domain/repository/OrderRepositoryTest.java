package com.groom.e_commerce.order.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.support.IntegrationTestSupport; // 👈 작성하신 파일 import
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;

// 1. @DataJpaTest 제거 -> IntegrationTestSupport 상속으로 변경
// 2. @Transactional 추가 (테스트 후 데이터 롤백을 위해 필수)
@Transactional
class OrderRepositoryTest extends IntegrationTestSupport {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Test
	@DisplayName("특정 상품 ID가 포함된 주문 목록을 조회한다.")
	void findAllByProductId() {
		// given
		UUID targetProductId = UUID.randomUUID();
		UUID otherProductId = UUID.randomUUID();

		// 주문 1: 타겟 상품 포함
		Order order1 = createOrder();
		createOrderItem(order1, targetProductId);

		// 주문 2: 타겟 상품 포함
		Order order2 = createOrder();
		createOrderItem(order2, targetProductId);

		// 주문 3: 타겟 상품 미포함 (다른 상품만 있음)
		Order order3 = createOrder();
		createOrderItem(order3, otherProductId);

		// when
		List<Order> result = orderRepository.findAllByProductId(targetProductId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("orderId")
			.containsExactlyInAnyOrder(order1.getOrderId(), order2.getOrderId());
	}

	// 테스트 헬퍼 메서드
	private Order createOrder() {
		// 👇 [중요 수정] 중복 방지를 위해 UUID 앞부분(8자리) 활용 + "ORD-" 접두사 = 총 12자 (20자 이내)
		String uniqueOrderNo = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

		Order order = Order.builder()
			.buyerId(UUID.randomUUID())
			.orderNumber(uniqueOrderNo)
			.totalPaymentAmount(10000L)
			.recipientName("테스트 수령인")
			.recipientPhone("010-1234-5678")
			.zipCode("12345")
			.shippingAddress("서울시 강남구 테헤란로")
			.shippingMemo("문 앞")
			.build();
		return orderRepository.save(order);
	}

	private OrderItem createOrderItem(Order order, UUID productId) {
		OrderItem item = OrderItem.builder()
			.order(order)
			.productId(productId)
			.ownerId(UUID.randomUUID()) // 필수값 처리
			.productTitle("테스트 상품")
			.quantity(1)
			.unitPrice(1000L)
			.variantId(UUID.randomUUID())
			.build();
		return orderItemRepository.save(item);
	}
}
