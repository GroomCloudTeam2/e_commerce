package com.groom.e_commerce.order.application.service;

import com.groom.e_commerce.order.application.port.out.PaymentPort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

// 👇 @Component를 붙여야 스프링이 "아, 이게 PaymentPort 구현체구나!" 하고 인식합니다.
@Component
public class DummyPaymentAdapter implements PaymentPort {

	@Override
	public void cancelPayment(UUID orderId, Long amount, List<UUID> orderItemIds) {
		// 실제 로직 없음. 로그만 찍고 성공한 척함.
		System.out.println("====== [TEST] 가짜 결제 취소 요청됨 ======");
		System.out.println("OrderID: " + orderId);
		System.out.println("Amount: " + amount);
		System.out.println("========================================");
	}
}
