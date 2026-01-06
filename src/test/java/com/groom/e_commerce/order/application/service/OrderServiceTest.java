package com.groom.e_commerce.order.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.status.OrderStatus;
import com.groom.e_commerce.order.presentation.dto.request.OrderStatusChangeRequest;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Test
	@DisplayName("배송 완료 처리: 서비스 계층에서 상태 변경 및 동기화가 정상 작동한다.")
	void completeDelivery_Service_Test() {
		// given
		UUID itemId = UUID.randomUUID();
		OrderStatusChangeRequest request = new OrderStatusChangeRequest(List.of(itemId));

		// 1. 헬퍼 메서드로 빈 객체 생성 (protected 무시)
		Order order = createEmptyOrder();
		OrderItem item = createEmptyOrderItem();

		// 2. 테스트에 필요한 상태값 강제 주입 (ReflectionTestUtils)
		ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPING);      // 주문: 배송 중

		ReflectionTestUtils.setField(item, "itemStatus", OrderStatus.SHIPPING);   // 상품: 배송 중
		ReflectionTestUtils.setField(item, "order", order);                       // 연관관계 설정

		// 3. 주문 동기화 로직을 위해 Order에 Item 리스트 주입
		ReflectionTestUtils.setField(order, "item", List.of(item));

		// Mocking
		given(orderItemRepository.findAllByOrderItemIdIn(request.orderItemIds()))
			.willReturn(List.of(item));

		// when
		orderService.completeDelivery(request);

		// then
		assertThat(item.getItemStatus()).isEqualTo(OrderStatus.DELIVERED); // 상품 상태 변경 확인
		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);    // 주문 상태 동기화 확인
	}

	// ======================================================
	//  테스트용 헬퍼 메서드 (protected 생성자 우회)
	// ======================================================

	private Order createEmptyOrder() {
		try {
			java.lang.reflect.Constructor<Order> constructor = Order.class.getDeclaredConstructor();
			constructor.setAccessible(true); // private/protected 무시
			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Order 생성 실패", e);
		}
	}

	private OrderItem createEmptyOrderItem() {
		try {
			java.lang.reflect.Constructor<OrderItem> constructor = OrderItem.class.getDeclaredConstructor();
			constructor.setAccessible(true); // private/protected 무시
			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("OrderItem 생성 실패", e);
		}
	}
}
