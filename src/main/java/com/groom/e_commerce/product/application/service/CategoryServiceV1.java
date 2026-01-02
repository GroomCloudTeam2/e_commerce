package com.groom.e_commerce.product.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceV1 {

	private final CategoryRepository categoryRepository;

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
}
