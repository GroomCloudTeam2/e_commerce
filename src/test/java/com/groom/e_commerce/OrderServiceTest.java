package com.groom.e_commerce;

import com.groom.e_commerce.order.application.service.OrderService;
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateItemRequest;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateRequest;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.response.ResAddressDtoV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock private OrderRepository orderRepository;
	@Mock private OrderItemRepository orderItemRepository;
	@Mock private AddressServiceV1 addressService;
	@Mock private PaymentRepository paymentRepository;

	@Test
	@DisplayName("주문 생성 시, 총 주문 금액과 OrderId가 포함된 Payment(READY) 데이터가 저장되어야 한다.")
	void shouldSavePayment_withCorrectAmount_whenCreateOrder() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		int quantity = 2;
		long productPrice = 10000L; // OrderService 내부 MockProductResponse 가격이 10,000원 고정

		// 요청 DTO 생성
		OrderCreateItemRequest itemReq = new OrderCreateItemRequest(productId, quantity);
		OrderCreateRequest request = new OrderCreateRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(itemReq));

		// Address Mocking
		given(addressService.getAddress(any(), any()))
			.willReturn(ResAddressDtoV1.builder()
				.recipient("테스터")
				.recipientPhone("010-0000-0000")
				.zipCode("12345")
				.address("서울시 강남구")
				.detailAddress("101호")
				.build());

		// OrderRepository Mocking: save 호출 시 orderId 부여
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			ReflectionTestUtils.setField(order, "orderId", UUID.randomUUID());
			return order;
		});

		// when
		UUID resultOrderId = orderService.createOrder(userId, request);

		// then
		assertThat(resultOrderId).isNotNull();

		// ★ 핵심 검증: PaymentRepository.save()가 호출될 때 넘겨진 파라미터를 낚아챔(Capture)
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		// 1. 주문 ID가 일치하는가?
		assertThat(savedPayment.getOrderId()).isEqualTo(resultOrderId);

		// 2. 총 금액이 맞는가? (10,000원 * 2개 = 20,000원)
		assertThat(savedPayment.getAmount()).isEqualTo(productPrice * quantity);

		// 3. 상태가 READY인가?
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.READY);

		System.out.println("검증 완료: 저장된 금액 = " + savedPayment.getAmount());
	}
}
