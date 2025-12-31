package com.groom.e_commerce.review;

import com.groom.e_commerce.global.security.AuthenticatedUser;
import com.groom.e_commerce.global.security.SecurityUtil;
import com.groom.e_commerce.global.security.mock.MockAuthenticatedUser;
import com.groom.e_commerce.review.presentation.controller.AuthTestController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthTestController.class)
public class ReviewAuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		AuthenticatedUser mockUser = new MockAuthenticatedUser(
			UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
			"mockuser"
		);

		Authentication auth = new UsernamePasswordAuthenticationToken(
			mockUser, null, List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	void testSecurityUtilAndController() throws Exception {
		// SecurityUtil 동작 확인
		UUID userId = SecurityUtil.getCurrentUserId();
		assertThat(userId).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

		// 컨트롤러 호출 테스트
		mockMvc.perform(get("/test/me"))
			.andExpect(status().isOk());
	}
}
