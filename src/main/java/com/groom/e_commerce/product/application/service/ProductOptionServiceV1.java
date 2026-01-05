package com.groom.e_commerce.product.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductOption;
import com.groom.e_commerce.product.domain.entity.ProductOptionValue;
import com.groom.e_commerce.product.domain.repository.ProductOptionRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqOptionUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResOptionDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductOptionServiceV1 {

	private final ProductRepository productRepository;
	private final ProductOptionRepository productOptionRepository;

	/**
	 * 옵션 전체 수정 (PUT) - 기존 옵션 삭제 후 새로 생성
	 */
	@Transactional
	public List<ResOptionDtoV1> updateOptions(UUID productId, ReqOptionUpdateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		// 기존 옵션 삭제 (Cascade로 옵션값도 삭제)
		product.clearOptions();

		// 새 옵션 생성
		List<ProductOption> newOptions = new ArrayList<>();
		int optionSortOrder = 1;

		for (ReqOptionUpdateDtoV1.OptionRequest optionReq : request.getOptions()) {
			ProductOption option = ProductOption.builder()
				.product(product)
				.name(optionReq.getName())
				.sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : optionSortOrder++)
				.build();

			int valueSortOrder = 1;
			for (ReqOptionUpdateDtoV1.OptionValueRequest valueReq : optionReq.getValues()) {
				ProductOptionValue optionValue = ProductOptionValue.builder()
					.option(option)
					.value(valueReq.getValue())
					.sortOrder(valueReq.getSortOrder() != null ? valueReq.getSortOrder() : valueSortOrder++)
					.build();
				option.addOptionValue(optionValue);
			}

			product.addOption(option);
		}

		// 저장 및 ID 생성을 위해 Flush
		productRepository.saveAndFlush(product);

		// 저장된 옵션 목록 반환 (ID 포함)
		return product.getOptions().stream()
			.map(ResOptionDtoV1::from)
			.collect(Collectors.toList());
	}

	/**
	 * 상품의 옵션 목록 조회
	 */
	public List<ResOptionDtoV1> getOptions(UUID productId) {
		List<ProductOption> options = productOptionRepository.findByProductIdWithValues(productId);
		return options.stream()
			.map(ResOptionDtoV1::from)
			.collect(Collectors.toList());
	}

	private Product findProductById(UUID productId) {
		return productRepository.findByIdAndNotDeleted(productId)
			.orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private void validateProductOwnership(Product product, UUID ownerId) {
		if (!product.isOwnedBy(ownerId)) {
			throw new CustomException(ErrorCode.PRODUCT_ACCESS_DENIED);
		}
	}
}
