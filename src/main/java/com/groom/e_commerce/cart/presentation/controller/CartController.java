package com.groom.e_commerce.cart.presentation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.groom.e_commerce.cart.presentation.dto.response.CartItemResponse;
import com.groom.e_commerce.global.infrastructure.config.security.CustomUserDetails;
import com.groom.e_commerce.cart.application.CartService;
import com.groom.e_commerce.cart.presentation.dto.request.CartAddRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@Tag(name = "Cart", description = "Cart CRUD")
@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
	private final CartService cartService;

	@PostMapping("/items")
	public ResponseEntity<Void> addItem(
		@RequestBody CartAddRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
		cartService.addItemToCart(userDetails.getUserId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@GetMapping
	public ResponseEntity<List<CartItemResponse>> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ResponseEntity.ok(cartService.getMyCart(userDetails.getUserId()));
	}
}
