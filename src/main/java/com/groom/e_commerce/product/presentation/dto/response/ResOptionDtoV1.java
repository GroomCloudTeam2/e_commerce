package com.groom.e_commerce.product.presentation.dto.response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.groom.e_commerce.product.domain.entity.ProductOption;
import com.groom.e_commerce.product.domain.entity.ProductOptionValue;

import lombok.Builder;
import lombok.Getter;

/**
 * 옵션 응답 DTO
 */
@Getter
@Builder
public class ResOptionDtoV1 {

	private UUID optionId;
	private String name;
	private Integer sortOrder;
	private List<OptionValueDto> values;

	@Getter
	@Builder
	public static class OptionValueDto {
		private UUID optionValueId;
		private String value;
		private Integer sortOrder;

		public static OptionValueDto from(ProductOptionValue optionValue) {
			return OptionValueDto.builder()
				.optionValueId(optionValue.getId())
				.value(optionValue.getValue())
				.sortOrder(optionValue.getSortOrder())
				.build();
		}
	}

	public static ResOptionDtoV1 from(ProductOption option) {
		return ResOptionDtoV1.builder()
			.optionId(option.getId())
			.name(option.getName())
			.sortOrder(option.getSortOrder())
			.values(option.getOptionValues().stream()
				.map(OptionValueDto::from)
				.collect(Collectors.toList()))
			.build();
	}
}
