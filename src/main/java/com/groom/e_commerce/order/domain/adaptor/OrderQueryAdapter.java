package com.groom.e_commerce.order.domain.adaptor;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryAdapter implements OrderQueryPort {

	private final OrderRepository orderRepository;

	@Override
	public OrderSummary getOrderSummary(UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

		return new OrderSummary(
			order.getOrderId(),
			order.getTotalPaymentAmount(), // 검증용 총액
			order.getOrderNumber(),
			order.getRecipientName()
		);
	}

	@Override
	public List<OrderItemSnapshot> getOrderItems(UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

		// 엔티티 -> 스냅샷 변환
		return order.getItem().stream()
			.map(itm -> new OrderItemSnapshot(
				itm.getOrderItemId(),
				itm.getOwnerId(), // 정산 대상 판매자 ID
				itm.getSubtotal() // 아이템별 금액
			))
			.collect(Collectors.toList());
	}
}
