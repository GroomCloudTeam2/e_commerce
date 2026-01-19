package com.groom.e_commerce.order.presentation.dto.response;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 생성 응답")
public record OrderCreateResponse(
	@Schema(description = "생성된 주문의 UUID", example = "2f4fceba-3015-47ee-9014-65b1b172cd46")
	UUID orderId
) {
}
