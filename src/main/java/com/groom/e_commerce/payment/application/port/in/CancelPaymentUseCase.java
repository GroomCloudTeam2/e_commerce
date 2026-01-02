package com.groom.e_commerce.payment.application.port.in;

import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResultV1;

public interface CancelPaymentUseCase {
	ResCancelResultV1 cancel(String paymentKey, ReqCancelPaymentV1 request);
}
