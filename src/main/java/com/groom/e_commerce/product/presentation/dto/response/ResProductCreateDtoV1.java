package com.groom.e_commerce.product.presentation.dto.response;

import java.util.UUID;

import com.groom.e_commerce.product.domain.entity.Product;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResProductCreateDtoV1 {

	private UUID productId;
	private String message;

	public static ResProductCreateDtoV1 from(Product product) {
		return ResProductCreateDtoV1.builder()
			.productId(product.getId())
			.message("상품이 등록되었습니다.")
			.build();
	}
}
