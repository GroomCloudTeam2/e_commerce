package com.groom.e_commerce.review.presentation.controller;

import com.groom.e_commerce.global.security.AuthenticatedUser;
import com.groom.e_commerce.global.security.SecurityUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AuthTestController {

	@GetMapping("/test/me")
	public ResponseEntity<String> testAuth(@AuthenticationPrincipal AuthenticatedUser user) {
		// SecurityUtil을 통해서도 userId 가져오기
		UUID userIdFromUtil = SecurityUtil.getCurrentUserId();
		return ResponseEntity.ok(
			"Authenticated userId from principal: " + user.getUserId() +
				", from SecurityUtil: " + userIdFromUtil
		);
	}
}
