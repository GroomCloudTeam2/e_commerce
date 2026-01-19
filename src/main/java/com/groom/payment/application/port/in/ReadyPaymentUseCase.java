package com.groom.e_commerce.payment.application.port.in;

import com.groom.e_commerce.payment.presentation.dto.request.ReqReadyPaymentV1;
import com.groom.e_commerce.payment.presentation.dto.response.ResReadyPaymentV1;

public interface ReadyPaymentUseCase {

	ResReadyPaymentV1 ready(ReqReadyPaymentV1 request);
}
