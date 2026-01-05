// src/main/java/com/groom/e_commerce/payment/application/port/in/CancelOrderItemPaymentUseCase.java
package com.groom.e_commerce.payment.application.port.in;

import java.util.UUID;

import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

public interface CancelOrderItemPaymentUseCase {
	ResCancelResultV1 cancelOrderItem(UUID orderId, UUID orderItemId, long cancelAmount);
}
