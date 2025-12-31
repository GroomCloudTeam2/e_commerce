package com.groom.e_commerce.user.presentation.controller;

import com.groom.e_commerce.global.infrastructure.config.security.CustomUserDetails;
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.request.ReqAddressDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResAddressDtoV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "User", description = "배송지 API")
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
public class AddressControllerV1 {

    private final AddressServiceV1 addressService;

    @Operation(summary = "배송지 목록 조회")
    @GetMapping
    public ResponseEntity<List<ResAddressDtoV1>> getAddresses(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(addressService.getAddresses(user.getUserId()));
    }

    @Operation(summary = "배송지 등록")
    @PostMapping
    public ResponseEntity<Void> createAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReqAddressDtoV1 request) {
        addressService.createAddress(user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "배송지 수정")
    @PutMapping("/{addressId}")
    public ResponseEntity<Void> updateAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID addressId,
            @Valid @RequestBody ReqAddressDtoV1 request) {
        addressService.updateAddress(user.getUserId(), addressId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "배송지 삭제")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID addressId) {
        addressService.deleteAddress(user.getUserId(), addressId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "기본 배송지 설정")
    @PostMapping("/{addressId}/set-default")
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID addressId) {
        addressService.setDefaultAddress(user.getUserId(), addressId);
        return ResponseEntity.ok().build();
    }
}
