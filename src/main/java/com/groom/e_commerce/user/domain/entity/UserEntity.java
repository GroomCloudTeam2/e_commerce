package com.groom.e_commerce.user.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity {

	// =========================
	// PK
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id", columnDefinition = "uuid")
	private UUID userId;

	// =========================
	// Basic Info
	// =========================
	@Column(name = "email", length = 100, nullable = false, unique = true)
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@Column(name = "nickname", length = 200, nullable = false, unique = true)
	private String nickname;

	@Column(name = "phone_number", length = 200, nullable = false)
	private String phoneNumber;

	// =========================
	// Status / Role
	// =========================
	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	@Builder.Default
	private UserStatus status = UserStatus.ACTIVE;

	// =========================
	// Relation
	// =========================
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<AddressEntity> addresses = new ArrayList<>();

	// =========================
	// Update Methods
	// =========================
	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void updatePhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	// =========================
	// Status Change
	// =========================
	public void withdraw(String deletedBy) {
		this.status = UserStatus.WITHDRAWN;
		// BaseEntity의 soft delete 사용
		super.softDelete(deletedBy);
	}

	// 기존 시그니처 유지용 (원하면 삭제 가능)
	public void withdraw() {
		withdraw(null);
	}

	public void ban() {
		this.status = UserStatus.BANNED;
	}

	public void activate() {
		this.status = UserStatus.ACTIVE;
	}

	// =========================
	// Status Check
	// =========================
	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}

	public boolean isBanned() {
		return this.status == UserStatus.BANNED;
	}

	// =========================
	// Reactivate
	// =========================
	public void reactivate(String encodedPassword, String nickname, String phoneNumber) {
		this.password = encodedPassword;
		this.nickname = nickname;
		this.phoneNumber = phoneNumber;
		this.status = UserStatus.ACTIVE;

		// BaseEntity의 deleted_at/deleted_by는 "복구" 처리 필요 시 여기서 null 처리하는 방식으로 확장 가능
		// (BaseEntity에 restore 메서드가 없어서, 보통은 BaseEntity에 restore()를 추가해서 처리)
	}
}
