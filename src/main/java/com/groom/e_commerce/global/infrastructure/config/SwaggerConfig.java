package com.groom.e_commerce.global.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		// 1. 보안 스키마 설정 (JWT Bearer 방식)
		String jwtSchemeName = "JWT Authentication";

		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

		Components components = new Components()
			.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
				.name(jwtSchemeName)
				.type(SecurityScheme.Type.HTTP) // HTTP 방식
				.scheme("bearer")
				.bearerFormat("JWT")); // 토큰 포맷

		// 2. Swagger UI에 적용
		return new OpenAPI()
			.info(new Info()
				.title("이커머스 프로젝트 API")
				.description("주문, 인증, 상품 기능을 제공하는 API 명세서")
				.version("1.0.0"))
			.addSecurityItem(securityRequirement)
			.components(components);
	}
}
