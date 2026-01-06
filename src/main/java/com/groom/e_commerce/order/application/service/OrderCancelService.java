package com.groom.e_commerce.order.application.service;

import com.groom.e_commerce.order.application.port.out.PaymentPort;
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.presentation.dto.request.OrderCancelRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

	private final OrderItemRepository orderItemRepository;
	private final PaymentPort paymentPort;// 결제 담당자가 구현할 인터페이스
	// private final ProductService productService; // 재고 담당 서비스

	@Transactional
	public void cancelOrderItems(Long userId, OrderCancelRequest request) {
		// 1. 주문 상품 조회
		List<OrderItem> items = orderItemRepository.findAllByOrderItemIdIn(request.orderItemIds());
		if (items.isEmpty()) {
			throw new IllegalArgumentException("취소할 주문 상품 정보가 존재하지 않습니다.");
		}
		// 2. 검증 (내 주문 맞는지, 취소 가능한 상태인지)
		// (여기서 UserId 비교 로직 추가 필요)
		// validateOwner(userId, items);
		items.forEach(OrderItem::validateCancelable);

		// 3. 결제 취소 요청을 위한 데이터 준비
		// 취소할 상품들의 총 금액 계산
		long totalCancelAmount = items.stream()
			.mapToLong(OrderItem::getRefundAmount)
			.sum();

		// order id 가져옴
		UUID orderId = items.get(0).getOrder().getOrderId();

		// =================================================================
		// 4. [핵심] 결제 서비스 호출 (여기가 결제팀과 연결되는 부분!)
		// =================================================================
		paymentPort.cancelPayment(orderId, totalCancelAmount, request.orderItemIds());

		// // 5. 후처리 (결제 취소 성공 시 실행됨)
		items.forEach(item -> {
			item.cancel();
		// 상태 변경 (CANCELLED)
		// 	productService.increaseStock(item.getProductId(), item.getQuantity()); // 재고 복구
		});

		// (선택) 주문 전체 취소 동기화 로직은 여기에 추가


	}
}
