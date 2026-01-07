package com.groom.e_commerce.payment.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;
import com.groom.e_commerce.payment.application.port.out.OrderStatePort;
import com.groom.e_commerce.payment.application.port.out.TossPaymentPort;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.infrastructure.api.toss.config.TossPaymentsProperties;

@ExtendWith(MockitoExtension.class)
abstract class PaymentCommandServiceTestBase {

	@Mock protected PaymentRepository paymentRepository;
	@Mock protected TossPaymentPort tossPaymentPort;
	@Mock protected TossPaymentsProperties tossPaymentsProperties;
	@Mock protected OrderQueryPort orderQueryPort;
	@Mock protected OrderStatePort orderStatePort;

	protected PaymentCommandService service;

	@BeforeEach
	void setUpBase() {
		service = new PaymentCommandService(
			paymentRepository,
			tossPaymentPort,
			tossPaymentsProperties,
			orderQueryPort,
			orderStatePort
		);
	}
}
