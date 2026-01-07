package com.groom.e_commerce.product.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.product.domain.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

	// 최상위 카테고리 조회
	List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrder();

	// 특정 부모 밑에 있는 자식 카테고리 조회
	List<Category> findByParentIdAndIsActiveTrueOrderBySortOrder(UUID parentId);

	// 전체 카테고리 목록 조회
	@Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.depth, c.sortOrder")
	List<Category> findAllActiveOrderByDepthAndSortOrder();

	// 대분류 조회 시 자식들까지 한 번에 로딩
	@Query("SELECT c FROM Category c LEFT JOIN FETCH c.children "
		+ "WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.sortOrder")
	List<Category> findRootCategoriesWithChildren();

	// (Master) 대분류 조회 시 자식들까지 한 번에 로딩 (비활성 포함)
	@Query("SELECT c FROM Category c LEFT JOIN FETCH c.children "
		+ "WHERE c.parent IS NULL ORDER BY c.sortOrder")
	List<Category> findRootCategoriesWithChildrenIncludingInactive();

	// 카테고리 삭제 전 자식이 있는지 검사
	boolean existsByParentId(UUID parentId);

	// 활성화된 카테고리인지 확인하면서 단건 조회
	@Query("SELECT c FROM Category c WHERE c.id = :id AND c.isActive = true")
	java.util.Optional<Category> findByIdAndIsActiveTrue(@Param("id") UUID id);
}
