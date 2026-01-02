package com.groom.e_commerce.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@TestConfiguration
public class TestQueryDslConfig {

	@PersistenceContext
	private EntityManager entityManager;

	@Bean
	public JPAQueryFactory jpaQueryFactory() {
		return new JPAQueryFactory(entityManager);
	}

	@Bean
	public ProductQueryRepository productQueryRepository() {
		// 우리가 만든 QueryDSL 전용 리포지토리를 빈으로 등록합니다.
		// 이걸 등록 안 하면 테스트 코드에서 @Autowired로 주입받을 수 없습니다.
		return new ProductQueryRepository(jpaQueryFactory());
	}
}
