package com.groom.e_commerce.product.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.product.domain.entity.ProductOptionValue;

@Repository
public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, UUID> {

	// 특정 옵션에 속한 값들 조회
	List<ProductOptionValue> findByOptionIdOrderBySortOrder(UUID optionId);

	// 특정 상품에 포함된 모든 옵션 값을 조회
	@Query("SELECT v FROM ProductOptionValue v WHERE v.option.product.id = :productId")
	List<ProductOptionValue> findByProductId(@Param("productId") UUID productId);

	// 옵션 값 ID 목록으로 한 번에 조회
	List<ProductOptionValue> findByIdIn(List<UUID> ids);

	// 옵션 삭제 시 딸려있는 값들 삭제
	void deleteByOptionId(UUID optionId);
}
