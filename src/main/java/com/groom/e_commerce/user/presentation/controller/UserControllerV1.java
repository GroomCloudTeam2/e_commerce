package com.groom.e_commerce.user.presentation.controller;

import com.groom.e_commerce.global.infrastructure.config.security.CustomUserDetails;
import com.groom.e_commerce.user.application.service.UserServiceV1;
import com.groom.e_commerce.user.domain.entity.PeriodType;
import com.groom.e_commerce.user.presentation.dto.request.ReqUpdateUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResSalesStatDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResUserDtoV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserControllerV1 {

    private final UserServiceV1 userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping
    public ResponseEntity<ResUserDtoV1> getMe(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getMe(user.getUserId()));
    }

    @Operation(summary = "내 정보 수정")
    @PatchMapping
    public ResponseEntity<Void> updateMe(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReqUpdateUserDtoV1 request) {
        userService.updateMe(user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴 (Soft Delete)")
    @DeleteMapping
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal CustomUserDetails user) {
        userService.deleteMe(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "매출 통계 조회 (OWNER only)")
    @GetMapping("/sales")
    public ResponseEntity<List<ResSalesStatDtoV1>> getSalesStats(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam PeriodType periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(userService.getSalesStats(user.getUserId(), periodType, date));
    }
}
