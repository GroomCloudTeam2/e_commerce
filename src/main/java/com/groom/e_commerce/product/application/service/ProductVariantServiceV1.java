package com.groom.e_commerce.product.application.service;

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
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.domain.repository.ProductVariantRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqVariantUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResVariantDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductVariantServiceV1 {

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;

	/**
	 * SKU(Variant) 추가
	 */
	@Transactional
	public ResVariantDtoV1 createVariant(UUID productId, ReqVariantCreateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		// SKU 코드 중복 검사
		if (request.getSkuCode() != null && productVariantRepository.existsBySkuCode(request.getSkuCode())) {
			throw new CustomException(ErrorCode.DUPLICATE_SKU_CODE);
		}

		// optionName 생성 (optionValueIds가 있는 경우)
		String optionName = request.getOptionName();
		if (optionName == null && request.getOptionValueIds() != null && !request.getOptionValueIds().isEmpty()) {
			optionName = buildOptionNameFromValueIds(product, request.getOptionValueIds());
		}

		ProductVariant variant = ProductVariant.builder()
			.product(product)
			.skuCode(request.getSkuCode())
			.optionValueIds(request.getOptionValueIds())
			.optionName(optionName)
			.price(request.getPrice())
			.stockQuantity(request.getStockQuantity())
			.build();

		product.addVariant(variant);
		Product savedProduct = productRepository.saveAndFlush(product);

		// 저장된 상품의 variants 목록에서 방금 추가한 variant를 찾아 ID가 포함된 상태로 반환
		ProductVariant savedVariant = savedProduct.getVariants().stream()
			.filter(v -> v.getSkuCode().equals(request.getSkuCode()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

		return ResVariantDtoV1.from(savedVariant);
	}

	/**
	 * SKU(Variant) 수정
	 */
	@Transactional
	public ResVariantDtoV1 updateVariant(UUID productId, UUID variantId, ReqVariantUpdateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
			.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

		variant.update(request.getOptionName(), request.getPrice(), request.getStockQuantity());

		if (request.getStatus() != null) {
			variant.updateStatus(request.getStatus());
		}

		return ResVariantDtoV1.from(variant);
	}

	/**
	 * SKU(Variant) 삭제
	 * - 주문이 있으면 DISCONTINUED로 상태 변경 (Soft Delete)
	 * - 주문이 없으면 Hard Delete
	 */
	@Transactional
	public void deleteVariant(UUID productId, UUID variantId) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
			.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

		// TODO: 주문 도메인과 연동 후 주문 여부 확인
		// 현재는 Hard Delete로 처리
		boolean hasOrders = false; // orderService.hasOrdersForVariant(variantId);

		if (hasOrders) {
			variant.discontinue();
		} else {
			product.getVariants().remove(variant);
			productVariantRepository.delete(variant);
		}
	}

	/**
	 * 상품의 SKU 목록 조회
	 */
	public List<ResVariantDtoV1> getVariants(UUID productId) {
		List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
		return variants.stream()
			.map(ResVariantDtoV1::from)
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

	/**
	 * optionValueIds로부터 optionName 생성
	 */
	private String buildOptionNameFromValueIds(Product product, List<UUID> optionValueIds) {
		StringBuilder nameBuilder = new StringBuilder();

		for (ProductOption option : product.getOptions()) {
			for (ProductOptionValue value : option.getOptionValues()) {
				if (optionValueIds.contains(value.getId())) {
					if (nameBuilder.length() > 0) {
						nameBuilder.append(" / ");
					}
					nameBuilder.append(value.getValue());
				}
			}
		}

		return nameBuilder.length() > 0 ? nameBuilder.toString() : null;
	}
}
