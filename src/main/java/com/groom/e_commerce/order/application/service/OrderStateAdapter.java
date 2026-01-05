package com.groom.e_commerce.order.application.service;

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
}
