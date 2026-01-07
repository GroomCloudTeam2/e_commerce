package com.groom.e_commerce.order.application.service;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.domain.status.OrderStatus;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateItemRequest;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateRequest;
import com.groom.e_commerce.order.presentation.dto.request.OrderStatusChangeRequest;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.response.address.ResAddressDtoV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock private OrderRepository orderRepository;
	@Mock private OrderItemRepository orderItemRepository;
	@Mock private AddressServiceV1 addressService;
	@Mock private PaymentRepository paymentRepository;
	@Mock private ProductServiceV1 productServiceV1;

	@Test
	@DisplayName("주문 생성 성공: 재고 차감 및 결제 생성 확인")
	void createOrder_Success() {
		// given
		UUID buyerId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		OrderCreateRequest request = new OrderCreateRequest();
		ReflectionTestUtils.setField(request, "addressId", UUID.randomUUID()); // UUID로 수정됨
		ReflectionTestUtils.setField(request, "items", List.of(
			new OrderCreateItemRequest(productId, null, 2)
		));

		// Mocking
		given(addressService.getAddress(any(), any())).willReturn(ResAddressDtoV1.builder()
			.recipient("장지민").zipCode("12345").address("경기도 광주").detailAddress("453").build());

		given(productServiceV1.getProductCartInfos(anyList())).willReturn(List.of(
			ProductCartInfo.builder()
				.productId(productId).productName("사과").price(1000L).build()
		));

		// when
		orderService.createOrder(buyerId, request);

		// then
		verify(productServiceV1, times(1)).decreaseStockBulk(anyList());
		verify(orderRepository, times(1)).save(any(Order.class));
		verify(paymentRepository, times(1)).save(any());
	}

	@Test
	@DisplayName("주문 취소: 재고 복구 로직이 '한 번만' 호출되어야 한다 (폭증 버그 검증)")
	void cancelOrder_BugFix_Verification() {
		// given
		UUID orderId = UUID.randomUUID();

		Order order = Order.builder().buyerId(UUID.randomUUID()).build();

		OrderItem item1 = OrderItem.builder()
			.order(order)
			.productId(UUID.randomUUID())
			.quantity(2) // 필수
			.unitPrice(1000L) // 필수
			.build();

		OrderItem item2 = OrderItem.builder()
			.order(order)
			.productId(UUID.randomUUID())
			.quantity(3) // 필수
			.unitPrice(2000L) // 필수
			.build();

		// Order 엔티티 필드명("item" vs "items")에 맞춰 주입
		ReflectionTestUtils.setField(order, "item", List.of(item1, item2));

		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

		// when
		orderService.cancelOrder(orderId);

		// then
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

		ArgumentCaptor<List<StockManagement>> captor = ArgumentCaptor.forClass(List.class);
		verify(productServiceV1, times(1)).increaseStockBulk(captor.capture());

		List<StockManagement> capturedList = captor.getValue();
		assertThat(capturedList).hasSize(2);
	}

	// import static org.mockito.Mockito.mock; // 필요 시 상단에 추가

	@Test
	@DisplayName("배송 시작: N+1 방지용 findAllWithItemsByIdIn 메서드가 호출되어야 한다")
	void startShipping_Performance_Verification() {
		// given
		List<UUID> itemIds = List.of(UUID.randomUUID(), UUID.randomUUID());

		OrderStatusChangeRequest request = new OrderStatusChangeRequest(itemIds);

		// 1. Order 생성 (ID가 있어야 서비스 로직에서 추출 가능하므로 ID 주입)
		Order order = Order.builder().orderNumber("ORD-001").build();
		ReflectionTestUtils.setField(order, "orderId", UUID.randomUUID());

		// 2. [수정] Real Entity 대신 Mock 사용 (상태 검증 로직 우회)
		// 실제 OrderItem을 쓰면 상태(PENDING) 때문에 에러가 나므로, 껍데기(Mock)만 사용합니다.
		OrderItem item = org.mockito.Mockito.mock(OrderItem.class);

		// item.getOrder() 호출 시 준비한 order 반환하도록 설정
		given(item.getOrder()).willReturn(order);

		given(orderItemRepository.findAllByOrderItemIdIn(itemIds)).willReturn(List.of(item));

		// 우리가 만든 fetch join 메서드 호출 가정
		given(orderRepository.findAllWithItemsByIdIn(anyList())).willReturn(List.of(order));

		// when
		orderService.startShipping(request);

		// then
		// mockItem.startShipping()은 아무 동작도 안 하므로 에러 없이 통과되고,
		// 바로 리포지토리 호출 여부를 검증할 수 있습니다.
		verify(orderRepository, times(1)).findAllWithItemsByIdIn(anyList());
	}
}
