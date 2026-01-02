package com.groom.e_commerce.product.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.VariantStatus;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

	// 상품의 모든 Variant 조회 (manager용)
	List<ProductVariant> findByProductId(UUID productId);

	// 상태별 조회
	List<ProductVariant> findByProductIdAndStatus(UUID productId, VariantStatus status);

	// SKU 코드로 조회
	Optional<ProductVariant> findBySkuCode(String skuCode);

	// SKU 코드 중복 검사
	boolean existsBySkuCode(String skuCode);

	// 구매 가능한 Variant만 조회 (사용자용)
	@Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.status != 'DISCONTINUED'")
	List<ProductVariant> findActiveByProductId(@Param("productId") UUID productId);

	// 내 상품의 Variant가 맞는지 검증하며 조회
	@Query("SELECT v FROM ProductVariant v WHERE v.id = :id AND v.product.id = :productId")
	Optional<ProductVariant> findByIdAndProductId(@Param("id") UUID id, @Param("productId") UUID productId);

	// 상품 삭제 시 Variant 일괄 삭제
	void deleteByProductId(UUID productId);

	// 활성화된 Variant가 하나라도 있는지 확인
	@Query("SELECT COUNT(v) > 0 FROM ProductVariant v WHERE v.product.id = :productId AND v.status != 'DISCONTINUED'")
	boolean hasActiveVariants(@Param("productId") UUID productId);

	// [설계 의도] 장바구니에 담긴 여러 SKU를 한 번에 조회
	List<ProductVariant> findByIdIn(List<UUID> ids);
}
