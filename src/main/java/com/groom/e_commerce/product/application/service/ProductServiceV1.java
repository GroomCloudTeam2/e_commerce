package com.groom.e_commerce.product.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceV1 {

	private final ProductRepository productRepository;
	private final ProductQueryRepository productQueryRepository;
	private final CategoryServiceV1 categoryService;

	/**
	 * 상품 등록 (Owner)
	 */
	@Transactional
	public ResProductDtoV1 createProduct(ReqProductCreateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Category category = categoryService.findActiveCategoryById(request.getCategoryId());

		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title(request.getTitle())
			.description(request.getDescription())
			.thumbnailUrl(request.getThumbnailUrl())
			.hasOptions(request.getHasOptions())
			.price(request.getPrice())
			.stockQuantity(request.getStockQuantity())
			.build();

		Product savedProduct = productRepository.save(product);
		return ResProductDtoV1.from(savedProduct);
	}

	/**
	 * 내 상품 목록 조회 (Owner)
	 */
	public Page<ResProductListDtoV1> getSellerProducts(ProductStatus status, Pageable pageable) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Page<Product> products = productQueryRepository.findSellerProducts(ownerId, status, pageable);
		return products.map(ResProductListDtoV1::from);
	}

	/**
	 * 상품 수정 (Owner)
	 */
	@Transactional
	public ResProductDtoV1 updateProduct(UUID productId, ReqProductUpdateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		Category category = null;
		if (request.getCategoryId() != null) {
			category = categoryService.findActiveCategoryById(request.getCategoryId());
		}

		product.update(
			category,
			request.getTitle(),
			request.getDescription(),
			request.getThumbnailUrl(),
			request.getPrice(),
			request.getStockQuantity()
		);

		return ResProductDtoV1.from(product);
	}

	/**
	 * 상품 삭제 - Soft Delete (Owner)
	 */
	@Transactional
	public void deleteProduct(UUID productId) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		product.softDelete(ownerId);
	}

	/**
	 * 상품 조회 (삭제되지 않은 상품)
	 */
	public Product findProductById(UUID productId) {
		return productRepository.findByIdAndNotDeleted(productId)
			.orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	/**
	 * 상품 소유권 검증
	 */
	private void validateProductOwnership(Product product, UUID ownerId) {
		if (!product.isOwnedBy(ownerId)) {
			throw new CustomException(ErrorCode.PRODUCT_ACCESS_DENIED);
		}
	}
}
