package com.groom.e_commerce.cart.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.e_commerce.cart.domain.entity.Cart;
import com.groom.e_commerce.cart.domain.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

	// 1. 옵션 상품 중복 체크 (variantId가 있을 때)
	Optional<CartItem> findByCartAndVariantId(Cart cart, UUID variantId);

	// 2. 단일 상품 중복 체크 (variantId가 없을 때 - productId가 같고 variantId가 null인 것)
	Optional<CartItem> findByCartAndProductIdAndVariantIdIsNull(Cart cart, UUID productId);
}
