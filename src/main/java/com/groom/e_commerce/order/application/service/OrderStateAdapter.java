package com.groom.e_commerce.order.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.payment.application.port.out.OrderStatePort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderStateAdapter implements OrderStatePort {

	private final OrderRepository orderRepository;

	@Override
	@Transactional
	public void payOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		order.markPaid();
	}

	@Override
	@Transactional
	public void cancelOrderByPayment(
		UUID orderId,
		Long canceledAmountThisTime,
		Long canceledAmountTotal,
		String paymentStatus,
		List<UUID> orderItemIds
	) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		// ✅ 전액 취소일 때만 주문을 CANCELLED로 전이 (Order.cancel() 재사용)
		if ("CANCELLED".equals(paymentStatus)) {
			order.cancel(); // 내부에서 canCancel() 검증 + 주문아이템 cancel까지 처리
		}

		// ✅ 부분 취소는 현재 Order 모델에 반영할 필드/상태가 없으므로 상태 변경하지 않음
		// 필요하면 여기에 로그만 남겨도 됨.
	}
}
