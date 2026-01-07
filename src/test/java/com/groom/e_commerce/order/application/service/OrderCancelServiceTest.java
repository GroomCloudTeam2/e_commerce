package com.groom.e_commerce.order.application.service;

import com.groom.e_commerce.order.application.port.out.PaymentPort;
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.presentation.dto.request.OrderCancelRequest;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

	@InjectMocks
	private OrderCancelService orderCancelService;

	@Mock private OrderItemRepository orderItemRepository;
	@Mock private PaymentPort paymentPort;
	@Mock private ProductServiceV1 productServiceV1;

	@Test
	@DisplayName("부분 취소: 결제 취소 후 재고 복구(Bulk)가 누락되지 않고 호출되어야 한다")
	void cancelOrderItems_ShouldRestoreStock() {
		// given
		Long userId = 1L; // (서비스 메서드에서 사용 안 하면 제거 가능)

		// [수정 1] Long -> UUID 변경 (시스템 전체 ID 타입 통일)
		List<UUID> cancelItemIds = List.of(UUID.randomUUID(), UUID.randomUUID());

		OrderCancelRequest request = new OrderCancelRequest(cancelItemIds);

		Order order = Order.builder().build(); // Dummy Order

		// 취소할 아이템들 (필수값 unitPrice, quantity 포함)
		OrderItem item1 = OrderItem.builder()
			.order(order)
			.productId(UUID.randomUUID())
			.quantity(1)
			.unitPrice(1000L)
			.build();

		OrderItem item2 = OrderItem.builder()
			.order(order)
			.productId(UUID.randomUUID())
			.quantity(2)
			.unitPrice(2000L)
			.build();

		given(orderItemRepository.findAllByOrderItemIdIn(cancelItemIds)).willReturn(List.of(item1, item2));

		// when
		// [수정 2] 메서드 호출 인자 수정 (2개 -> 1개)
		// 에러 메시지에 따라 userId를 제거하고 request만 넘깁니다.
		orderCancelService.cancelOrderItems(userId, request);
		// 만약 위 코드가 여전히 에러라면 아래처럼 userId를 지우세요:
		// orderCancelService.cancelOrderItems(request);

		// then
		// 1. 외부 결제 취소 포트 호출 확인
		verify(paymentPort, times(1)).cancelPayment(any(), anyLong(), anyList());

		// 2. [중요] 주석 해제한 재고 복구 로직(Bulk) 호출 확인
		verify(productServiceV1, times(1)).increaseStockBulk(anyList());
	}
}
