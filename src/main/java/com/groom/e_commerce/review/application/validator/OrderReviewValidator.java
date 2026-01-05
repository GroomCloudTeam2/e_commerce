package com.groom.e_commerce.review.application.validator;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.domain.status.OrderStatus;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderReviewValidator {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ReviewRepository reviewRepository;

	public void validate(UUID orderId, UUID productId, UUID userId) {

		// 1. 주문 존재 여부
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() ->
				new IllegalArgumentException("주문이 존재하지 않습니다.")
			);

		// 리뷰 중복 체크
		reviewRepository.findByOrderIdAndProductId(orderId, productId)
			.ifPresent(r -> {
				throw new IllegalStateException("이미 리뷰가 존재합니다.");
			});

		// 2. 주문 소유자 검증
		if (!order.getBuyerId().equals(userId)) {
			throw new SecurityException("본인의 주문만 리뷰할 수 있습니다.");
		}

		// 3. 주문에 해당 상품이 포함되어 있는지
		boolean containsProduct =
			orderItemRepository.existsByOrderIdAndProductId(orderId, productId);

		if (!containsProduct) {
			throw new IllegalArgumentException("주문한 상품이 아닙니다.");
		}

		if (!order.getStatus().equals(OrderStatus.CONFIRMED)) {
		     throw new IllegalStateException("리뷰 가능한 주문 상태가 아닙니다.");
		 }
	}
}
