package com.groom.e_commerce.cart.domain.entity;

import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "p_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "cart_id")
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;



	public Cart(UUID userId) {
		this.userId = userId;
	}
}
