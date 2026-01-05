package com.groom.e_commerce.user.domain.entity;

import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "p_seller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class SellerEntity extends BaseEntity {

	// =========================
	// PK
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "seller_id", columnDefinition = "uuid")
	private UUID sellerId;

	// =========================
	// Relation
	// =========================
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;

	// =========================
	// Store / Business Info
	// =========================
	@Column(name = "store_name", length = 200, nullable = false)
	private String storeName;

	@Column(name = "business_no", length = 50)
	private String businessNo;

	// =========================
	// Store Address
	// =========================
	@Column(name = "zip_code", length = 20)
	private String zipCode;

	@Column(name = "address", length = 200)
	private String address;

	@Column(name = "detail_address", length = 200)
	private String detailAddress;

	// =========================
	// Settlement Info
	// =========================
	@Column(name = "bank", length = 50)
	private String bank;

	@Column(name = "account", length = 100)
	private String account;

	// =========================
	// Status
	// =========================
	@Column(name = "status", length = 20)
	@Builder.Default
	private String status = "ACTIVE";

	// =========================
	// Business Methods
	// =========================
	public void updateInfo(String storeName, String businessNo, String zipCode,
		String address, String detailAddress, String bank, String account) {

		if (storeName != null) {
			this.storeName = storeName;
		}
		if (businessNo != null) {
			this.businessNo = businessNo;
		}
		if (zipCode != null) {
			this.zipCode = zipCode;
		}
		if (address != null) {
			this.address = address;
		}
		if (detailAddress != null) {
			this.detailAddress = detailAddress;
		}
		if (bank != null) {
			this.bank = bank;
		}
		if (account != null) {
			this.account = account;
		}
	}
}
