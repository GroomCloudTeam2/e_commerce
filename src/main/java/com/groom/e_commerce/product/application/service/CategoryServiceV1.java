package com.groom.e_commerce.product.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceV1 {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	/**
	 * 전체 카테고리 목록 조회 (계층 구조)
	 */
	public List<ResCategoryDtoV1> getAllCategories() {
		List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren();
		return rootCategories.stream()
			.map(ResCategoryDtoV1::fromWithChildren)
			.toList();
	}

	/**
	 * 루트 카테고리 목록 조회
	 */
	public List<ResCategoryDtoV1> getRootCategories() {
		List<Category> categories = categoryRepository
			.findByParentIsNullAndIsActiveTrueOrderBySortOrder();
		return categories.stream()
			.map(ResCategoryDtoV1::from)
			.toList();
	}

	/**
	 * 특정 카테고리의 자식 카테고리 목록 조회
	 */
	public List<ResCategoryDtoV1> getChildCategories(UUID parentId) {
		validateCategoryExists(parentId);
		List<Category> categories = categoryRepository
			.findByParentIdAndIsActiveTrueOrderBySortOrder(parentId);
		return categories.stream()
			.map(ResCategoryDtoV1::from)
			.toList();
	}

	/**
	 * 카테고리 상세 조회
	 */
	public ResCategoryDtoV1 getCategory(UUID categoryId) {
		Category category = findActiveCategoryById(categoryId);
		return ResCategoryDtoV1.fromWithChildren(category);
	}

	/**
	 * 카테고리 엔티티 조회 (내부용)
	 */
	public Category findActiveCategoryById(UUID categoryId) {
		return categoryRepository.findByIdAndIsActiveTrue(categoryId)
			.orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
	}

	private void validateCategoryExists(UUID categoryId) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
		}
	}

	// ==================== Master Methods ====================

	/**
	 * 전체 카테고리 목록 조회 (Master) - 비활성 포함
	 */
	public List<ResCategoryDtoV1> getAllCategoriesForMaster() {
		List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildrenIncludingInactive();
		return rootCategories.stream()
			.map(ResCategoryDtoV1::fromWithChildrenIncludingInactive)
			.toList();
	}

	/**
	 * 카테고리 생성 (Master)
	 */
	@Transactional
	public void createCategory(ReqCategoryCreateDtoV1 request) {
		Category parent = null;
		int depth = 1;

		if (request.getParentId() != null) {
			parent = categoryRepository.findById(request.getParentId())
				.orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
			depth = parent.getDepth() + 1;
		}

		Category category = Category.builder()
			.name(request.getName())
			.depth(depth)
			.sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 1)
			.isActive(request.getIsActive() != null ? request.getIsActive() : true)
			.parent(parent)
			.build();

		categoryRepository.save(category);
	}

	/**
	 * 카테고리 수정 (Master)
	 */
	@Transactional
	public void updateCategory(UUID categoryId, ReqCategoryUpdateDtoV1 request) {
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

		category.update(
			request.getName(),
			request.getSortOrder(),
			request.getIsActive()
		);
	}

	/**
	 * 카테고리 삭제 (Master)
	 */
	@Transactional
	public void deleteCategory(UUID categoryId) {
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

		// 하위 카테고리 존재 여부 확인
		if (categoryRepository.existsByParentId(categoryId)) {
			throw new CustomException(ErrorCode.CATEGORY_HAS_CHILDREN);
		}

		// 상품 존재 여부 확인
		if (productRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)) {
			throw new CustomException(ErrorCode.CATEGORY_HAS_PRODUCTS);
		}

		categoryRepository.delete(category);
	}
}
