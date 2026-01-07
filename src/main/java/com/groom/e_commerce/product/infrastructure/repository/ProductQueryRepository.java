package com.groom.e_commerce.product.infrastructure.repository;

import static com.groom.e_commerce.product.domain.entity.QCategory.*;
import static com.groom.e_commerce.product.domain.entity.QProduct.*;
import static com.groom.e_commerce.product.domain.entity.QProductVariant.*;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductSortType;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

	private final JPAQueryFactory queryFactory;

	// 사용자/관리자 상품 검색 (키워드, 카테고리, 가격 범위, 상태)
	public Page<Product> searchProducts(
		String keyword,
		UUID categoryId,
		Long minPrice,
		Long maxPrice,
		ProductStatus status,
		Pageable pageable
	) {
		List<Product> content = queryFactory
			.selectFrom(product)
			.leftJoin(product.category, category).fetchJoin()
			.where(
				keywordContains(keyword),
				categoryIdEq(categoryId),
				priceGoe(minPrice),
				priceLoe(maxPrice),
				statusEq(status),
				notDeleted()
			)
			.orderBy(product.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(product.count())
			.from(product)
			.where(
				keywordContains(keyword),
				categoryIdEq(categoryId),
				priceGoe(minPrice),
				priceLoe(maxPrice),
				statusEq(status),
				notDeleted()
			);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	/**
	 * 구매자용 상품 목록 검색 (판매중인 상품만)
	 */
	public Page<Product> searchProductsForBuyer(
		String keyword,
		UUID categoryId,
		Long minPrice,
		Long maxPrice,
		ProductSortType sortType,
		Pageable pageable
	) {
		List<Product> content = queryFactory
			.selectFrom(product)
			.leftJoin(product.category, category).fetchJoin()
			.leftJoin(product.variants, productVariant).fetchJoin()
			.where(
				keywordContains(keyword),
				categoryIdEq(categoryId),
				priceGoe(minPrice),
				priceLoe(maxPrice),
				onSaleOnly(),
				notDeleted()
			)
			.orderBy(getOrderSpecifier(sortType))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.distinct()
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(product.countDistinct())
			.from(product)
			.where(
				keywordContains(keyword),
				categoryIdEq(categoryId),
				priceGoe(minPrice),
				priceLoe(maxPrice),
				onSaleOnly(),
				notDeleted()
			);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	// Owner가 자신의 상품 목록을 조회할 때 사용
	public Page<Product> findSellerProducts(
		UUID ownerId,
		ProductStatus status,
		String keyword,
		Pageable pageable
	) {
		List<Product> content = queryFactory
			.selectFrom(product)
			.leftJoin(product.category, category).fetchJoin()
			.where(
				ownerIdEq(ownerId),
				statusEq(status),
				keywordContains(keyword),
				notDeleted()
			)
			.orderBy(product.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(product.count())
			.from(product)
			.where(
				ownerIdEq(ownerId),
				statusEq(status),
				keywordContains(keyword),
				notDeleted()
			);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	// manager 페이지에서 상품 전체를 관리할 때 사용
	public Page<Product> findAllForManager(
		String keyword,
		ProductStatus status,
		Pageable pageable
	) {
		List<Product> content = queryFactory
			.selectFrom(product)
			.leftJoin(product.category, category).fetchJoin()
			.where(
				keywordContains(keyword),
				statusEq(status),
				notDeleted()
			)
			.orderBy(product.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(product.count())
			.from(product)
			.where(
				keywordContains(keyword),
				statusEq(status),
				notDeleted()
			);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	//여러 상품 ID들을 받아, 각 상품의 옵션까지 한 번에 가져옴 (장바구니/주문용 일괄 조회)
	public List<Product> findProductsWithVariantsByIds(List<UUID> productIds) {
		return queryFactory
			.selectFrom(product)
			.leftJoin(product.variants, productVariant).fetchJoin()
			.where(product.id.in(productIds))
			.distinct()
			.fetch();
	}

	private BooleanExpression keywordContains(String keyword) {
		return StringUtils.hasText(keyword)
			? product.title.containsIgnoreCase(keyword)
			.or(product.description.containsIgnoreCase(keyword))
			: null;
	}

	private BooleanExpression categoryIdEq(UUID categoryId) {
		return categoryId != null ? product.category.id.eq(categoryId) : null;
	}

	private BooleanExpression priceGoe(Long minPrice) {
		return minPrice != null ? product.price.goe(minPrice) : null;
	}

	private BooleanExpression priceLoe(Long maxPrice) {
		return maxPrice != null ? product.price.loe(maxPrice) : null;
	}

	private BooleanExpression statusEq(ProductStatus status) {
		return status != null ? product.status.eq(status) : null;
	}

	private BooleanExpression ownerIdEq(UUID ownerId) {
		return ownerId != null ? product.ownerId.eq(ownerId) : null;
	}

	private BooleanExpression notDeleted() {
		return product.deletedAt.isNull();
	}

	private BooleanExpression onSaleOnly() {
		return product.status.eq(ProductStatus.ON_SALE);
	}

	private OrderSpecifier<?> getOrderSpecifier(ProductSortType sortType) {
		if (sortType == null) {
			return product.createdAt.desc();
		}
		return switch (sortType) {
			case PRICE_ASC -> product.price.asc();
			case PRICE_DESC -> product.price.desc();
			case NEWEST -> product.createdAt.desc();
			case RATING -> product.createdAt.desc(); // TODO: Review 도메인 연동 후 평점순 구현
		};
	}
}
