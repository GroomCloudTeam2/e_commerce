package com.groom.e_commerce.product.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.product.domain.entity.ProductOption;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {

	// 특정 상품에 딸린 옵션 목록 조회
	List<ProductOption> findByProductIdOrderBySortOrder(UUID productId);

	// 옵션과 그 옵션의 값들을 한 번에 조회
	// 상세 페이지 들어갈 때: "색상(빨,파) + 사이즈(95,100)" 정보를 쿼리 한 방에 가져옴.
	@Query("SELECT o FROM ProductOption o LEFT JOIN FETCH o.optionValues "
		+ "WHERE o.product.id = :productId ORDER BY o.sortOrder")
	List<ProductOption> findByProductIdWithValues(@Param("productId") UUID productId);

	// 상품 삭제 시 옵션들도 싹 지우기 위해 사용
	void deleteByProductId(UUID productId);

	// 상품에 옵션이 등록되어 있는지 확인
	boolean existsByProductId(UUID productId);
}
