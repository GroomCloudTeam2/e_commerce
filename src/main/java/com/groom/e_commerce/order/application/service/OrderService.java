package com.groom.e_commerce.order.application.service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.response.ResAddressDtoV1;

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
	// MSA 핵심: Repository가 아니라 Service(또는 Client)를 주입받음
	// private final ProductService productService;
	// private final AddressService addressService;

	/**
	 * 주문 생성 (핵심 비즈니스 로직)
	 */
	@Transactional // 쓰기 트랜잭션 시작
	public UUID createOrder(UUID buyerId, OrderCreateRequest request) {

		ResAddressDtoV1 addressInfo = addressService.getAddress(request.getAddressId(), buyerId);
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
			.totalPaymentAmount(BigInteger.valueOf(0L))
			.build();

		orderRepository.save(order); // 영속화 (ID 생성됨)

		// 4. 주문 상품(OrderItem) 처리
		long totalAmount = 0L;
		List<OrderItem> orderItems = new ArrayList<>();

		for (OrderCreateItemRequest itemReq : request.getItems()) {

			// 상품 서비스에 정보 요청
			// ProductResponse productInfo = productService.getProduct(itemReq.getProductId());

			// 👇 [임시] 상품 서비스 대신 가짜 DTO 생성
			MockProductResponse productInfo = MockProductResponse.builder()
				.productId(itemReq.getProductId())
				.ownerId(UUID.randomUUID())
				.name("테스트 상품 (" + itemReq.getProductId().toString().substring(0, 5) + ")")
				.thumbnail("http://fake-image.com/img.png")
				.optionName("기본 옵션")
				.price(10000L) // 가격 10,000원으로 고정
				.build();

			// [MSA Point 2] 재고 차감 요청
			// productService.decreaseStock(itemReq.getProductId(), itemReq.getQuantity());
			// 👇 [임시] 재고 차감은 그냥 넘어감 (로그만 출력)
			System.out.println("재고 차감 요청됨: ID=" + itemReq.getProductId() + ", 수량=" + itemReq.getQuantity());

			// 5. 상품 스냅샷 생성 (OrderItem)
			OrderItem orderItem = OrderItem.builder()
				.order(order)
				.productId(productInfo.getProductId())
				.variantId(UUID.randomUUID())
				.ownerId(productInfo.getOwnerId())
				.productTitle(productInfo.getName())
				.productThumbnail(productInfo.getThumbnail())
				.optionName(productInfo.getOptionName())
				.unitPrice(productInfo.getPrice())
				.quantity(itemReq.getQuantity())
				.build();

			orderItems.add(orderItem);

			// 총액 합산
			totalAmount += (productInfo.getPrice() * itemReq.getQuantity());
		}

		// 6. OrderItem 일괄 저장
		orderItemRepository.saveAll(orderItems);

		order.updatePaymentAmount(totalAmount);
		System.out.println("최종 결제 금액: " + totalAmount);
		Payment payment = Payment.builder()
			.orderId(order.getOrderId())
			.amount(totalAmount)
			.status(PaymentStatus.READY) // 중요: 초기 상태
			// .paymentKey(null) // 빌더에 따라 생략 가능
			.build();

		paymentRepository.save(payment);

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
		// OrderItem 리스트를 순회하며 각 상품의 수량만큼 재고를 다시 늘려줍니다.
		for (OrderItem item : order.getItem()) {

			// 상품 서비스에 재고 증가(복구) 요청
			// productService.increaseStock(item.getProductId(), item.getQuantity());

			// 👇 [임시] 상품 서비스 대신 로그 출력 (Mocking)(나중에 지우고 위 코드로 대체)
			System.out.println("=========================================");
			System.out.println("[재고 복구 요청]");
			System.out.println("상품 ID: " + item.getProductId());
			System.out.println("복구 수량: " + item.getQuantity());
			System.out.println("=========================================");
		}

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

		// 2. 주문 상태 동기화 (이 부분이 훨씬 깔끔해집니다!)
		Set<Order> orders = items.stream()
			.map(OrderItem::getOrder)
			.collect(Collectors.toSet());

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

	// 👇 [임시] 파일 하나로 해결하기 위해 내부에 만든 가짜 DTO 클래스
	@Getter
	@Builder
	static class MockProductResponse {
		private UUID productId;
		private UUID ownerId;
		private String name;
		private String thumbnail;
		private String optionName;
		private Long price;
	}

}
