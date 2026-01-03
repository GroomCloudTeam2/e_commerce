package com.groom.e_commerce.global.infrastructure.config.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
// 서비스 이용 시(매 요청) 토큰 검증 + 인증정보 세팅
// OncePerRequestFilter : 서블릿 필터의 한 종류, 하나의 HTTP 요청에 대해 1번만 실행되도록 보장
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

    // API 요청 시 호출
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		// Authorization Header -> Token 추출
        String token = resolveToken(request);

        // Token에서 사용자 정보 추출 -> CustomUserDetails 생성
		if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
			CustomUserDetails userDetails = new CustomUserDetails(
				jwtUtil.getUserIdFromToken(token),
				jwtUtil.getEmailFromToken(token),
				jwtUtil.getRoleFromToken(token)
			);

            // Authentication 객체 생성 (권한 정보 포함)
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_" + userDetails.getRole()))
			);

            // SecurityContext에 인증 정보 저장
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

        // 다음 필터로 진행
		filterChain.doFilter(request, response);
	}

    // Token 추출 로직
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
