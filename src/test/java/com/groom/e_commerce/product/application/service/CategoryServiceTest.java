package com.groom.e_commerce.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryServiceV1 categoryService;

	private Category rootCategory;
	private Category childCategory;
	private UUID rootCategoryId;
	private UUID childCategoryId;

	@BeforeEach
	void setUp() {
		rootCategoryId = UUID.randomUUID();
		childCategoryId = UUID.randomUUID();

		// 루트 카테고리 생성
		rootCategory = Category.builder()
			.name("의류")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();
		ReflectionTestUtils.setField(rootCategory, "id", rootCategoryId);

		// 자식 카테고리 생성
		childCategory = Category.builder()
			.parent(rootCategory)
			.name("상의")
			.depth(2)
			.sortOrder(1)
			.isActive(true)
			.build();
		ReflectionTestUtils.setField(childCategory, "id", childCategoryId);

		// 부모-자식 관계 설정
		rootCategory.getChildren().add(childCategory);
	}

	@Nested
	@DisplayName("getAllCategories")
	class GetAllCategoriesTest {

		@Test
		@DisplayName("전체 카테고리를 계층 구조로 조회한다")
		void getAllCategories_success() {
			// given
			given(categoryRepository.findRootCategoriesWithChildren())
				.willReturn(List.of(rootCategory));

			// when
			List<ResCategoryDtoV1> result = categoryService.getAllCategories();

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getName()).isEqualTo("의류");
			assertThat(result.get(0).getChildren()).hasSize(1);
			assertThat(result.get(0).getChildren().get(0).getName()).isEqualTo("상의");
		}

		@Test
		@DisplayName("카테고리가 없으면 빈 리스트를 반환한다")
		void getAllCategories_empty() {
			// given
			given(categoryRepository.findRootCategoriesWithChildren())
				.willReturn(new ArrayList<>());

			// when
			List<ResCategoryDtoV1> result = categoryService.getAllCategories();

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("getRootCategories")
	class GetRootCategoriesTest {

		@Test
		@DisplayName("루트 카테고리 목록을 조회한다")
		void getRootCategories_success() {
			// given
			given(categoryRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrder())
				.willReturn(List.of(rootCategory));

			// when
			List<ResCategoryDtoV1> result = categoryService.getRootCategories();

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getName()).isEqualTo("의류");
			assertThat(result.get(0).getParentId()).isNull();
		}
	}

	@Nested
	@DisplayName("getChildCategories")
	class GetChildCategoriesTest {

		@Test
		@DisplayName("특정 카테고리의 자식 목록을 조회한다")
		void getChildCategories_success() {
			// given
			given(categoryRepository.existsById(rootCategoryId)).willReturn(true);
			given(categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrder(rootCategoryId))
				.willReturn(List.of(childCategory));

			// when
			List<ResCategoryDtoV1> result = categoryService.getChildCategories(rootCategoryId);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getName()).isEqualTo("상의");
			assertThat(result.get(0).getParentId()).isEqualTo(rootCategoryId);
		}

		@Test
		@DisplayName("존재하지 않는 카테고리의 자식 조회 시 예외가 발생한다")
		void getChildCategories_notFound() {
			// given
			UUID invalidId = UUID.randomUUID();
			given(categoryRepository.existsById(invalidId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> categoryService.getChildCategories(invalidId))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException ce = (CustomException)ex;
					assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
				});
		}
	}

	@Nested
	@DisplayName("getCategory")
	class GetCategoryTest {

		@Test
		@DisplayName("카테고리 상세를 조회한다")
		void getCategory_success() {
			// given
			given(categoryRepository.findByIdAndIsActiveTrue(rootCategoryId))
				.willReturn(Optional.of(rootCategory));

			// when
			ResCategoryDtoV1 result = categoryService.getCategory(rootCategoryId);

			// then
			assertThat(result.getId()).isEqualTo(rootCategoryId);
			assertThat(result.getName()).isEqualTo("의류");
			assertThat(result.getDepth()).isEqualTo(1);
			assertThat(result.getIsActive()).isTrue();
		}

		@Test
		@DisplayName("존재하지 않는 카테고리 조회 시 예외가 발생한다")
		void getCategory_notFound() {
			// given
			UUID invalidId = UUID.randomUUID();
			given(categoryRepository.findByIdAndIsActiveTrue(invalidId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> categoryService.getCategory(invalidId))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException ce = (CustomException)ex;
					assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
				});
		}
	}

	@Nested
	@DisplayName("findActiveCategoryById")
	class FindActiveCategoryByIdTest {

		@Test
		@DisplayName("활성화된 카테고리 엔티티를 조회한다")
		void findActiveCategoryById_success() {
			// given
			given(categoryRepository.findByIdAndIsActiveTrue(rootCategoryId))
				.willReturn(Optional.of(rootCategory));

			// when
			Category result = categoryService.findActiveCategoryById(rootCategoryId);

			// then
			assertThat(result.getId()).isEqualTo(rootCategoryId);
			assertThat(result.getName()).isEqualTo("의류");
		}

		@Test
		@DisplayName("비활성화된 카테고리 조회 시 예외가 발생한다")
		void findActiveCategoryById_inactive() {
			// given
			UUID inactiveId = UUID.randomUUID();
			given(categoryRepository.findByIdAndIsActiveTrue(inactiveId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> categoryService.findActiveCategoryById(inactiveId))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException ce = (CustomException)ex;
					assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
				});
		}
	}
}
