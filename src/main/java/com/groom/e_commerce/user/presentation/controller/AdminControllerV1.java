package com.groom.e_commerce.user.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.review.application.service.ReviewAiSummaryService;
import com.groom.e_commerce.user.application.service.AdminServiceV1;
import com.groom.e_commerce.user.presentation.dto.request.ReqCreateManagerDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResUserListDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminControllerV1 {

	private final AdminServiceV1 adminService;

	// ==================== Manager 전용 ====================

	@Operation(summary = "회원 목록 조회 (Manager)")
	// @PreAuthorize : 아래 경로로 들어오는 트래픽은 Spring Security가 검사
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@GetMapping("/users")
	public ResponseEntity<ResUserListDtoV1> getUserList(@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getUserList(pageable));
	}

	@Operation(summary = "회원 제재 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/users/{userId}/ban")
	public ResponseEntity<Void> banUser(@PathVariable UUID userId) {
		adminService.banUser(userId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원 제재 해제 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/users/{userId}/unban")
	public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
		adminService.unbanUser(userId);
		return ResponseEntity.ok().build();
	}

	// ==================== Master 전용 ====================

	@Operation(summary = "Manager 계정 생성 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@PostMapping("/managers")
	public ResponseEntity<ResUserDtoV1> createManager(@Valid @RequestBody ReqCreateManagerDtoV1 request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createManager(request));
	}

	@Operation(summary = "Manager 계정 삭제 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@DeleteMapping("/managers/{managerId}")
	public ResponseEntity<Void> deleteManager(@PathVariable UUID managerId) {
		adminService.deleteManager(managerId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Manager 목록 조회 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@GetMapping("/managers")
	public ResponseEntity<ResUserListDtoV1> getManagerList(@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getManagerList(pageable));
	}

}
