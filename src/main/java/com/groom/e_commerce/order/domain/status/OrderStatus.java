package com.groom.e_commerce.order.domain.status;

public enum OrderStatus {
	PENDING,
	PAID,
	SHIPPING,
	DELIVERED,
	CONFIRMED,
	CANCELLED;

	public boolean canCancel() {
		return this == PENDING || this == PAID;
	}

	public boolean canShip() {
		return this == PAID;
	}

	public boolean canDeliver() {
		return this == SHIPPING;
	}

	public boolean canConfirm() {
		return this == DELIVERED;
	}
}
