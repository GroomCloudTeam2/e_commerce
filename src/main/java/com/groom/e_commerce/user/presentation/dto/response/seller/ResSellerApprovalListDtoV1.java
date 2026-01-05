package com.groom.e_commerce.user.presentation.dto.response.seller;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResSellerApprovalListDtoV1 {

	private List<ResSellerApprovalDtoV1> content;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;
	private boolean first;
	private boolean last;

	public static ResSellerApprovalListDtoV1 from(Page<ResSellerApprovalDtoV1> page) {
		return ResSellerApprovalListDtoV1.builder()
			.content(page.getContent())
			.page(page.getNumber())
			.size(page.getSize())
			.totalElements(page.getTotalElements())
			.totalPages(page.getTotalPages())
			.first(page.isFirst())
			.last(page.isLast())
			.build();
	}
}
