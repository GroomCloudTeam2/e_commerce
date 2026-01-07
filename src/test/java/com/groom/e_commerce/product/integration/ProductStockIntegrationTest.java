package com.groom.e_commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.support.IntegrationTestSupport;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.repository.CategoryRepository;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.user.application.service.UserServiceV1;

// @Transactional // 동시성 테스트에서는 트랜잭션이 각각의 스레드에서 돌아야 하므로 클래스 레벨 @Transactional은 제거하거나 주의해야 함.
// 여기서는 각 테스트 메서드에서 제어하거나, 동시성 테스트는 별도 메서드로 분리.
// 하지만 IntegrationTestSupport가 @SpringBootTest를 가지고 있고, 보통 롤백을 위해 @Transactional을 붙이는데,
// 동시성 테스트 시에는 실제 DB 커밋이 일어나야 다른 스레드에서 볼 수 있으므로 @Transactional을 붙이면 안됨 (혹은 Propagation.REQUIRES_NEW 사용).
// 따라서 이 클래스에는 @Transactional을 붙이지 않고, setup/teardown에서 데이터를 정리하는 방식을 씁니다.
class ProductStockIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ProductServiceV1 productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@MockBean
	private UserServiceV1 userService;

	private MockedStatic<SecurityUtil> securityUtilMock;
	private UUID ownerId;
	private Category category;

	@BeforeEach
	void setUp() {
		ownerId = UUID.randomUUID();
		securityUtilMock = mockStatic(SecurityUtil.class);
		securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(ownerId);

		category = Category.builder()
			.name("Stock Test Category")
			.depth(1)
			.sortOrder(1)
			.isActive(true)
			.build();
		categoryRepository.save(category);
	}

	@AfterEach
	void tearDown() {
		securityUtilMock.close();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
	}

	@Test
	@DisplayName("1. 재고 차감 및 복원 (기본)")
	void stockDecreaseAndRestore() {
		// given
		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Stock Product")
			.price(Long.valueOf(1000))
			.stockQuantity(10)
			.hasOptions(false)
			.build();
		productRepository.save(product);
		UUID productId = product.getId();

		// when: 차감
		productService.decreaseStock(productId, null, 3);

		// then
		Product decreased = productRepository.findById(productId).orElseThrow();
		assertThat(decreased.getStockQuantity()).isEqualTo(7);

		// when: 복원
		productService.increaseStock(productId, null, 2);

		// then
		Product restored = productRepository.findById(productId).orElseThrow();
		assertThat(restored.getStockQuantity()).isEqualTo(9);
	}

	@Test
	@DisplayName("2. 동시성 테스트: 재고 100개, 100명이 동시에 1개씩 주문 -> 재고 0개")
	void concurrencyStockDecrease() throws InterruptedException {
		// given
		int stockQuantity = 100;
		int threadCount = 100;
		
		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Concurrency Product")
			.price(Long.valueOf(1000))
			.stockQuantity(stockQuantity)
			.hasOptions(false)
			.build();
		productRepository.save(product);
		UUID productId = product.getId();

		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					productService.decreaseStock(productId, null, 1);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		Product result = productRepository.findById(productId).orElseThrow();
		assertThat(result.getStockQuantity()).isEqualTo(0);
	}

	@Test
	@DisplayName("3. 동시성 테스트: 재고 1개, 10명이 동시에 주문 -> 1명 성공, 9명 실패")
	void concurrencyStockDecrease_raceCondition() throws InterruptedException {
		// given
		int stockQuantity = 1;
		int threadCount = 10;
		
		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title("Race Condition Product")
			.price(Long.valueOf(1000))
			.stockQuantity(stockQuantity)
			.hasOptions(false)
			.build();
		productRepository.save(product);
		UUID productId = product.getId();

		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					productService.decreaseStock(productId, null, 1);
					successCount.incrementAndGet();
				} catch (CustomException e) {
					failCount.incrementAndGet();
				} catch (Exception e) {
					// Other exceptions
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		Product result = productRepository.findById(productId).orElseThrow();
		assertThat(result.getStockQuantity()).isEqualTo(0);
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(9);
	}
}
