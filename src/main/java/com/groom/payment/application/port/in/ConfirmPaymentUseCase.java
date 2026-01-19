package com.groom.e_commerce.payment.application.port.in;

import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResPaymentV1;

public interface ConfirmPaymentUseCase {
	ResPaymentV1 confirm(ReqConfirmPaymentV1 request);
}
