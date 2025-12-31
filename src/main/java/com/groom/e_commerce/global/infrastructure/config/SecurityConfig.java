package com.groom.e_commerce.global.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable) // CSRF 보안 비활성화 (API 서버는 보통 끔)
			.formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
			.httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/**").permitAll() // ⭐ 핵심: 모든 요청 허용 (나중에 여기만 수정하면 됨)
			);

		return http.build();
	}
}
