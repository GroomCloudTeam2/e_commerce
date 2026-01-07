package com.groom.e_commerce.order.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.domain.repository.OrderItemRepository;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateItemRequest;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateRequest;
import com.groom.e_commerce.order.presentation.dto.request.OrderStatusChangeRequest;
import com.groom.e_commerce.order.presentation.dto.response.OrderResponse;
import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.response.address.ResAddressDtoV1;
import com.groom.e_commerce.cart.application.CartService;

import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;


import jakarta.validation.constraints.Null;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final AddressServiceV1 addressService;

	private final PaymentRepository paymentRepository;
	private final ProductServiceV1 productServiceV1;
	private final CartService cartService;

	/**
	 * 주문 생성 (핵심 비즈니스 로직)
	 */
	@Transactional // 쓰기 트랜잭션 시작
	public UUID createOrder(UUID buyerId, OrderCreateRequest request) {

		ResAddressDtoV1 addressInfo = addressService.getAddress(request.getAddressId(), buyerId);

		List<StockManagement> stockManagements = request.getItems().stream()
			.map(item -> StockManagement.of(
				item.getProductId(),
				item.getVariantId(),
				item.getQuantity()
			))
			.toList();
		productServiceV1.decreaseStockBulk(stockManagements);

		// 2. 주문번호 생성
		String orderNumber = generateOrderNumber();
		// 3. 주문(Order) 엔티티 생성
		Order order = Order.builder()
			.buyerId(buyerId)
			.orderNumber(orderNumber)
			.recipientName(addressInfo.getRecipient())
			.recipientPhone(addressInfo.getRecipientPhone())
			.zipCode(addressInfo.getZipCode())
			.shippingAddress(addressInfo.getAddress() + " " + addressInfo.getDetailAddress())
			.shippingMemo("문 앞에 놔주세요") // (이건 request에 필드가 없어서 일단 고정, 필요하면 request에 추가)
			.totalPaymentAmount(0L)
			.build();

		orderRepository.save(order); // 영속화 (ID 생성됨)

		// 4. 주문 상품(OrderItem) 처리
		long totalAmount = 0L;
		List<OrderItem> orderItems = new ArrayList<>();

		// Bulk 조회
		List<ProductCartInfo> productInfos = productServiceV1.getProductCartInfos(stockManagements);

		// Map으로 변환 (Key: productId + "_" + variantId)
		Map<String, ProductCartInfo> productInfoMap = productInfos.stream()
			.collect(Collectors.toMap(
				info -> info.getProductId() + "_" + (info.getVariantId() != null ? info.getVariantId() : "null"),
				Function.identity()
			));

		for (OrderCreateItemRequest itemReq : request.getItems()) {
			String key = itemReq.getProductId() + "_" + (itemReq.getVariantId() != null ? itemReq.getVariantId() : "null");
			ProductCartInfo productInfo = productInfoMap.get(key);

			if (productInfo == null) {
				throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
			}

			// 5. 상품 스냅샷 생성 (OrderItem)
			OrderItem orderItem = OrderItem.builder()
				.order(order)
				.productId(productInfo.getProductId())
				.variantId(productInfo.getVariantId())
				.ownerId(productInfo.getOwnerId())
				.productTitle(productInfo.getProductName())
				.productThumbnail(productInfo.getThumbnailUrl())
				.optionName(productInfo.getOptionName() != null ? productInfo.getOptionName() : "기본")
				.unitPrice(productInfo.getPrice())
				.quantity(itemReq.getQuantity())
				.build();

			orderItems.add(orderItem);

			// 총액 합산
			totalAmount = totalAmount + (productInfo.getPrice()*(itemReq.getQuantity()));
		}

		// 6. OrderItem 일괄 저장
		orderItemRepository.saveAll(orderItems);

		order.updatePaymentAmount(totalAmount);
		System.out.println("최종 결제 금액: " + totalAmount);
		Payment payment = Payment.builder()
			.orderId(order.getOrderId())
			.amount(totalAmount)
			.status(PaymentStatus.READY)
			.build();

		paymentRepository.save(payment);

		if (request.getFromCartItemsIds() != null && !request.getFromCartItemsIds().isEmpty()) {
			cartService.removeCartItems(buyerId, request.getFromCartItemsIds());
		}
		return order.getOrderId();
	}

	private String generateOrderNumber() {
		String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
		return datePart + "-" + randomPart;
	}

	@Transactional(readOnly = true) // 중요: 조회 전용 트랜잭션 (성능 최적화)
	public OrderResponse getOrder(UUID orderId) {
		Order order = orderRepository.findByIdWithItems(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		return OrderResponse.from(order);
	}
	public List<OrderResponse> getOrdersByProduct(UUID productId) {
		// 1. 리포지토리 호출
		List<Order> orders = orderRepository.findAllByProductId(productId);

		// 2. Entity -> DTO 변환 (Stream 활용)
		return orders.stream()
			.map(OrderResponse::from) // 이미 구현된 from 메서드 재사용
			.collect(Collectors.toList());
	}

	/**
	 * 주문 취소 (핵심 비즈니스 로직)
	 */
	@Transactional // 데이터 변경(상태 변경 + 재고 복구)이므로 필수
	public void cancelOrder(UUID orderId) {

		// 1. 주문 조회
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		// 2. 주문 취소 처리 (Entity 내부 로직 호출)
		// -> Order 상태 변경 & OrderItem 상태 변경 수행됨
		order.cancel();


		// 3. 재고 복구 요청 (Product Service 연동)
		List<StockManagement> stockManagements = order.getItem().stream()
			.map(OrderItem::toStockManagement) // 변환!
			.toList();

		// 한 번만 호출
		productServiceV1.increaseStockBulk(stockManagements);


		// 4. (선택) 결제 취소 로직
		// if (order.getStatus() == OrderStatus.PAID) {
		//     paymentService.cancelPayment(order.getPaymentId());
		// }
	}

	/**
	 * 구매 확정
	 */
	@Transactional
	public void confirmOrder(UUID orderId, UUID currentUserId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		// 권한 검증
		if (!order.getBuyerId().equals(currentUserId)) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인의 주문만 구매 확정할 수 있습니다.");
		}

		// 엔티티 내부에서 상태 검증(DELIVERED 여부) 및 변경 수행
		order.confirm();
	}

	/**
	 * 배송 시작 처리 (관리자/시스템)
	 */
	@Transactional
	public void startShipping(OrderStatusChangeRequest request) {
		// 1. 아이템 상태 변경
		List<OrderItem> items = orderItemRepository.findAllByOrderItemIdIn(request.orderItemIds());
		items.forEach(OrderItem::startShipping);

		List<UUID> orderIds =items.stream()
			.map(item -> item.getOrder().getOrderId())
			.distinct()
			.toList();
		List<Order> orders=orderRepository.findAllWithItemsByIdIn(orderIds);

		for (Order order : orders) {
			order.syncStatus(); // 엔티티가 스스로 상태를 계산하도록 위임
		}
	}

	/**
	 * 배송 완료 처리 (관리자/시스템)
	 */
	@Transactional
	public void completeDelivery(OrderStatusChangeRequest request) {
		List<OrderItem> items = orderItemRepository.findAllByOrderItemIdIn(request.orderItemIds());
		if (items.isEmpty()) {
			throw new IllegalArgumentException("대상 상품을 찾을 수 없습니다.");
		}
		items.forEach(OrderItem::completeDelivery);
		Set<Order> orders = items.stream()
			.map(OrderItem::getOrder)
			.collect(Collectors.toSet());

		for (Order order : orders) {
			order.syncStatus(); // Order 상태 변경.
		}
	}


}
