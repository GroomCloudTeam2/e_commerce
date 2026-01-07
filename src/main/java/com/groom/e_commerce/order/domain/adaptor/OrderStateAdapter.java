package com.groom.e_commerce.order.domain.adaptor;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.payment.application.port.out.OrderStatePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
public class OrderStateAdapter implements OrderStatePort {

	private final OrderRepository orderRepository;

	// 1. 결제 성공 처리 (주문 상태 변경 + 정산 장부 생성)
	@Override
	@Transactional
	public void payOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		order.markPaid();
	}

	// 2. 결제 취소 처리 (주문 취소 + 정산 장부 무효화)
	@Override
	public void cancelOrderByPayment(UUID orderId, Long canceledAmountThisTime, Long canceledAmountTotal, String paymentStatus, List<UUID> orderItemIds) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

		// 2-1. 주문 상태 변경
		// Case 1: 부분 취소 (취소할 아이템 ID 목록이 넘어옴)
		if (orderItemIds != null && !orderItemIds.isEmpty()) {
			order.cancelItems(orderItemIds); // ★ Order 엔티티에 이 메서드를 만들어야 함
		}
		// Case 2: 전체 취소 (목록이 없으면 싹 다 취소)
		else {
			order.cancel();
		}
	}
}
