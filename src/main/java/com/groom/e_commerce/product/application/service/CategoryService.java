package com.groom.e_commerce.product.application.service;

import com.groom.e_commerce.global.common.exception.BusinessException;
import com.groom.e_commerce.global.common.exception.ErrorCode;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.presentation.dto.response.CategoryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;

	/**
	 * 전체 카테고리 목록 조회 (계층 구조)
	 */
	public List<CategoryResponse> getAllCategories() {
		List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren();
		return rootCategories.stream()
			.map(CategoryResponse::fromWithChildren)
			.toList();
	}

	/**
	 * 루트 카테고리 목록 조회
	 */
	public List<CategoryResponse> getRootCategories() {
		List<Category> categories = categoryRepository
			.findByParentIsNullAndIsActiveTrueOrderBySortOrder();
		return categories.stream()
			.map(CategoryResponse::from)
			.toList();
	}

	/**
	 * 특정 카테고리의 자식 카테고리 목록 조회
	 */
	public List<CategoryResponse> getChildCategories(UUID parentId) {
		validateCategoryExists(parentId);
		List<Category> categories = categoryRepository
			.findByParentIdAndIsActiveTrueOrderBySortOrder(parentId);
		return categories.stream()
			.map(CategoryResponse::from)
			.toList();
	}

	/**
	 * 카테고리 상세 조회
	 */
	public CategoryResponse getCategory(UUID categoryId) {
		Category category = findActiveCategoryById(categoryId);
		return CategoryResponse.fromWithChildren(category);
	}

	/**
	 * 카테고리 엔티티 조회 (내부용)
	 */
	public Category findActiveCategoryById(UUID categoryId) {
		return categoryRepository.findByIdAndIsActiveTrue(categoryId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
	}

	private void validateCategoryExists(UUID categoryId) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
		}
	}
}
