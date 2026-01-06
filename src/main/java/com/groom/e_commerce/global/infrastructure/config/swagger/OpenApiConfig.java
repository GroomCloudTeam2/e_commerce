package com.groom.e_commerce.global.infrastructure.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("E-Commerce 결제 API")
				.description("토스페이먼츠 결제 승인(ready/confirm) 테스트용 API")
				.version("v1")
			)
			.servers(List.of(
				new Server().url("http://localhost:8080").description("Local")
			));
	}
}
