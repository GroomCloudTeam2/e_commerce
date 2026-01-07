package com.groom.e_commerce.global.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
	"spring.datasource.driver-class-name=org.postgresql.Driver",
	"spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
	"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.url=jdbc:postgresql://localhost:5432/test_db" // DynamicPropertySource가 덮어쓸 더미 값
})
@Testcontainers
public abstract class IntegrationTestSupport {

	static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

	static {
		POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("test_db")
			.withUsername("test")
			.withPassword("test");
		POSTGRESQL_CONTAINER.start();
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
	}
}
