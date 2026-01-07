package com.groom.e_commerce.order.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

	// 사용자 배송지 ID (주문 시점의 주소 정보를 스냅샷으로 저장)
	private UUID addressId;

	// 주문 상품 목록
	private List<OrderCreateItemRequest> items;
	private List<UUID> fromCartItemsIds;
}
