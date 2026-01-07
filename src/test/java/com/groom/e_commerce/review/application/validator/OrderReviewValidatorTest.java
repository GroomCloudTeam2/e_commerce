package com.groom.e_commerce.review.application.validator;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.domain.status.OrderStatus;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class OrderReviewValidatorTest {

	@InjectMocks
	private OrderReviewValidator validator;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private ReviewRepository reviewRepository;

	private UUID orderId;
	private UUID productId;
	private UUID userId;

	@BeforeEach
	void setUp() {
		orderId = UUID.randomUUID();
		productId = UUID.randomUUID();
		userId = UUID.randomUUID();
	}

	@Test
	void 정상적인_주문이면_검증_통과() {
		Order order = mock(Order.class);

		when(orderRepository.findById(orderId))
			.thenReturn(Optional.of(order));
		when(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.thenReturn(Optional.empty());
		when(order.getBuyerId()).thenReturn(userId);
		when(orderItemRepository.existsByOrderIdAndProductId(orderId, productId))
			.thenReturn(true);
		when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);

		assertThatCode(() ->
			validator.validate(orderId, productId, userId)
		).doesNotThrowAnyException();
	}

	@Test
	void 주문이_없으면_예외() {
		when(orderRepository.findById(orderId))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() ->
			validator.validate(orderId, productId, userId)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 이미_리뷰가_있으면_예외() {
		Order order = mock(Order.class);

		when(orderRepository.findById(orderId))
			.thenReturn(Optional.of(order));
		when(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.thenReturn(Optional.of(mock()));

		assertThatThrownBy(() ->
			validator.validate(orderId, productId, userId)
		).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 본인_주문이_아니면_예외() {
		Order order = mock(Order.class);

		when(orderRepository.findById(orderId))
			.thenReturn(Optional.of(order));
		when(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.thenReturn(Optional.empty());
		when(order.getBuyerId()).thenReturn(UUID.randomUUID());

		assertThatThrownBy(() ->
			validator.validate(orderId, productId, userId)
		).isInstanceOf(SecurityException.class);
	}

	@Test
	void 주문에_상품이_없으면_예외() {
		Order order = mock(Order.class);

		when(orderRepository.findById(orderId))
			.thenReturn(Optional.of(order));
		when(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.thenReturn(Optional.empty());
		when(order.getBuyerId()).thenReturn(userId);
		when(orderItemRepository.existsByOrderIdAndProductId(orderId, productId))
			.thenReturn(false);

		assertThatThrownBy(() ->
			validator.validate(orderId, productId, userId)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 주문_상태가_CONFIRMED가_아니면_예외() {
		Order order = mock(Order.class);

		when(orderRepository.findById(orderId))
			.thenReturn(Optional.of(order));
		when(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.thenReturn(Optional.empty());
		when(order.getBuyerId()).thenReturn(userId);
		when(orderItemRepository.existsByOrderIdAndProductId(orderId, productId))
			.thenReturn(true);
		when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);

		assertThatThrownBy(() ->
			validator.validate(orderId, productId, userId)
		).isInstanceOf(IllegalStateException.class);
	}
}
