package com.groom.e_commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.support.IntegrationTestSupport;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.application.service.CategoryServiceV1;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqCategoryUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResCategoryDtoV1;
import com.groom.e_commerce.user.application.service.UserServiceV1;

@Transactional
class CategoryMasterIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private CategoryServiceV1 categoryService;

	@Autowired
	private CategoryRepository categoryRepository;

	@MockBean
	private UserServiceV1 userService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID masterId;

	@BeforeEach
	void setUp() {
		masterId = UUID.randomUUID();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(masterId);
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
	}

	@Test
	@DisplayName("1. 카테고리 생성 (최상위)")
	void createCategory_root() {
		// given
		ReqCategoryCreateDtoV1 request = ReqCategoryCreateDtoV1.builder()
			.name("Electronics")
			.sortOrder(1)
			.isActive(true)
			.build();

		// when
		categoryService.createCategory(request);

		// then
		List<Category> all = categoryRepository.findAll();
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getName()).isEqualTo("Electronics");
		assertThat(all.get(0).getParent()).isNull();
	}

	@Test
	@DisplayName("2. 카테고리 생성 (하위) 및 계층 검증")
	void createCategory_child() {
		// given
		Category parent = Category.builder()
			.name("Parent")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();
		categoryRepository.save(parent);

		ReqCategoryCreateDtoV1 request = ReqCategoryCreateDtoV1.builder()
			.parentId(parent.getId())
			.name("Child")
			.sortOrder(1)
			.isActive(true)
			.build();

		// when
		categoryService.createCategory(request);

		// then
		List<Category> all = categoryRepository.findAll();
		assertThat(all).hasSize(2);
		
		Category child = all.stream().filter(c -> c.getName().equals("Child")).findFirst().orElseThrow();
		assertThat(child.getParent().getId()).isEqualTo(parent.getId());
		assertThat(child.getDepth()).isEqualTo(2);
	}

	@Test
	@DisplayName("3. 전체 카테고리 조회 (숨김 포함)")
	void getAllCategoriesForMaster() {
		// given
		createCategory("Active Cat", true);
		createCategory("Inactive Cat", false);

		// when
		List<ResCategoryDtoV1> result = categoryService.getAllCategoriesForMaster();

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("name")
			.containsExactlyInAnyOrder("Active Cat", "Inactive Cat");
	}

	@Test
	@DisplayName("4. 카테고리 수정")
	void updateCategory() {
		// given
		Category category = createCategory("Old Name", true);
		
		ReqCategoryUpdateDtoV1 updateReq = ReqCategoryUpdateDtoV1.builder()
			.name("New Name")
			.sortOrder(5)
			.isActive(false)
			.build();

		// when
		categoryService.updateCategory(category.getId(), updateReq);

		// then
		Category updated = categoryRepository.findById(category.getId()).orElseThrow();
		assertThat(updated.getName()).isEqualTo("New Name");
		assertThat(updated.getSortOrder()).isEqualTo(5);
		assertThat(updated.getIsActive()).isFalse();
	}

	@Test
	@DisplayName("5. 카테고리 삭제 - 성공")
	void deleteCategory_success() {
		// given
		Category category = createCategory("To be deleted", true);

		// when
		categoryService.deleteCategory(category.getId());

		// then
		assertThat(categoryRepository.findById(category.getId())).isEmpty();
	}

	@Test
	@DisplayName("6. 카테고리 삭제 - 실패 (하위 카테고리 존재)")
	void deleteCategory_fail_hasChildren() {
		// given
		Category parent = createCategory("Parent", true);
		Category child = Category.builder()
			.name("Child")
			.parent(parent)
			.depth(2)
			.sortOrder(1)
			.isActive(true)
			.build();
		categoryRepository.save(child);

		// when & then
		assertThatThrownBy(() -> categoryService.deleteCategory(parent.getId()))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.CATEGORY_HAS_CHILDREN));
	}

	private Category createCategory(String name, boolean isActive) {
		Category category = Category.builder()
			.name(name)
			.depth(1)
			.sortOrder(1)
			.isActive(isActive)
			.build();
		return categoryRepository.save(category);
	}
}
