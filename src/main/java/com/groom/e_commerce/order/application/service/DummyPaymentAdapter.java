package com.groom.e_commerce.order.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.application.port.out.PaymentPort;

// 👇 @Component를 붙여야 스프링이 "아, 이게 PaymentPort 구현체구나!" 하고 인식합니다.
@Component
public class DummyPaymentAdapter implements PaymentPort {

	@Override
	public void cancelPayment(UUID orderId, Long cancelAmount, List<UUID> orderItemIds) {
		System.out.println("결제 취소 요청 - Order ID: " + orderId);
		System.out.println("Cancel Amount: " + cancelAmount);
		System.out.println("Canceled Items: " + orderItemIds);
	}
}
