package com.groom.e_commerce.review.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaginationResponse {
	private long totalElements;
	private int totalPages;
	private int currentPage;
	private boolean isLast;
}
