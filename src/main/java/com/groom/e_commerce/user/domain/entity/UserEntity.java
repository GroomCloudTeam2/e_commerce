package com.groom.e_commerce.user.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id", columnDefinition = "uuid")
	private UUID userId;

	@Column(name = "email", length = 100, nullable = false, unique = true)
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@Column(name = "nickname", length = 200, nullable = false, unique = true)
	private String nickname;

	@Column(name = "phone_number", length = 200, nullable = false)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	@Builder.Default
	private UserStatus status = UserStatus.ACTIVE;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<AddressEntity> addresses = new ArrayList<>();

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void updatePhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public void withdraw() {
		this.status = UserStatus.WITHDRAWN;
		this.deletedAt = LocalDateTime.now();
	}

	public void ban() {
		this.status = UserStatus.BANNED;
	}

	public void activate() {
		this.status = UserStatus.ACTIVE;
	}

	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}

	public boolean isBanned() {
		return this.status == UserStatus.BANNED;
	}

	public void reactivate(String encodedPassword, String nickname, String phoneNumber) {
		this.password = encodedPassword;
		this.nickname = nickname;
		this.phoneNumber = phoneNumber;
		this.status = UserStatus.ACTIVE;
		this.deletedAt = null;
	}
}
