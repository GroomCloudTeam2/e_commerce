package com.groom.e_commerce.product.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductOption;
import com.groom.e_commerce.product.domain.entity.ProductOptionValue;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.enums.VariantStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 상품 상세 조회 응답 DTO (구매자용)
 */
@Getter
@Builder
public class ResProductDetailDtoV1 {

	private UUID productId;
	private String title;
	private String description;
	private String thumbnailUrl;
	private ProductStatus status;
	private Boolean hasOptions;

	private CategoryInfo category;
	private OwnerInfo owner;

	private List<OptionInfo> options;
	private List<VariantInfo> variants;

	private RatingInfo rating;
	private LocalDateTime createdAt;

	public static ResProductDetailDtoV1 from(Product product) {
		return from(product, null, null, null);
	}

	public static ResProductDetailDtoV1 from(Product product, Double avgRating, Integer reviewCount,
		String ownerStoreName) {
		return ResProductDetailDtoV1.builder()
			.productId(product.getId())
			.title(product.getTitle())
			.description(product.getDescription())
			.thumbnailUrl(product.getThumbnailUrl())
			.status(product.getStatus())
			.hasOptions(product.getHasOptions())
			.category(CategoryInfo.from(product.getCategory()))
			.owner(OwnerInfo.from(product.getOwnerId(), ownerStoreName))
			.options(product.getOptions().stream()
				.map(OptionInfo::from)
				.collect(Collectors.toList()))
			.variants(product.getVariants().stream()
				.map(VariantInfo::from)
				.collect(Collectors.toList()))
			.rating(RatingInfo.from(avgRating, reviewCount))
			.createdAt(product.getCreatedAt())
			.build();
	}

	@Getter
	@Builder
	public static class CategoryInfo {
		private UUID categoryId;
		private String name;
		private String fullPath;

		public static CategoryInfo from(Category category) {
			return CategoryInfo.builder()
				.categoryId(category.getId())
				.name(category.getName())
				.fullPath(buildFullPath(category))
				.build();
		}

		private static String buildFullPath(Category category) {
			StringBuilder path = new StringBuilder();
			Category current = category;
			int maxDepth = 10; // 무한 루프 방지

			try {
				while (current != null && maxDepth-- > 0) {
					if (!path.isEmpty()) {
						path.insert(0, " > ");
					}
					path.insert(0, current.getName());
					current = current.getParent();
				}
			} catch (Exception e) {
				// LazyInitializationException 등 발생 시 현재까지 구성된 경로 반환
				if (path.isEmpty()) {
					return category.getName();
				}
			}

			return path.toString();
		}
	}

	@Getter
	@Builder
	public static class OwnerInfo {
		private UUID ownerId;
		private String storeName;

		public static OwnerInfo from(UUID ownerId, String storeName) {
			return OwnerInfo.builder()
				.ownerId(ownerId)
				.storeName(storeName)
				.build();
		}
	}

	@Getter
	@Builder
	public static class OptionInfo {
		private UUID optionId;
		private String name;
		private Integer sortOrder;
		private List<OptionValueInfo> values;

		public static OptionInfo from(ProductOption option) {
			return OptionInfo.builder()
				.optionId(option.getId())
				.name(option.getName())
				.sortOrder(option.getSortOrder())
				.values(option.getOptionValues().stream()
					.map(OptionValueInfo::from)
					.collect(Collectors.toList()))
				.build();
		}
	}

	@Getter
	@Builder
	public static class OptionValueInfo {
		private UUID optionValueId;
		private String value;

		public static OptionValueInfo from(ProductOptionValue optionValue) {
			return OptionValueInfo.builder()
				.optionValueId(optionValue.getId())
				.value(optionValue.getValue())
				.build();
		}
	}

	@Getter
	@Builder
	public static class VariantInfo {
		private UUID variantId;
		private String skuCode;
		private List<UUID> optionValueIds;
		private String optionName;
		private BigDecimal price;
		private Integer stockQuantity;
		private VariantStatus status;

		public static VariantInfo from(ProductVariant variant) {
			return VariantInfo.builder()
				.variantId(variant.getId())
				.skuCode(variant.getSkuCode())
				.optionValueIds(variant.getOptionValueIds())
				.optionName(variant.getOptionName())
				.price(variant.getPrice())
				.stockQuantity(variant.getStockQuantity())
				.status(variant.getStatus())
				.build();
		}
	}

	@Getter
	@Builder
	public static class RatingInfo {
		private Double avgRating;
		private Integer reviewCount;

		public static RatingInfo from(Double avgRating, Integer reviewCount) {
			return RatingInfo.builder()
				.avgRating(avgRating)
				.reviewCount(reviewCount)
				.build();
		}
	}
}
