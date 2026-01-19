// src/main/java/com/groom/e_commerce/payment/infrastructure/persistence/OrderQueryAdapter.java
package com.groom.e_commerce.payment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.port.out.OrderQueryPort;

@Component
public class OrderQueryAdapter implements OrderQueryPort {

	private final JdbcTemplate jdbcTemplate;

	public OrderQueryAdapter(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public OrderSummary getOrderSummary(UUID orderId) {
		// p_order에서 총액 + 주문번호 + 고객명(없으면 buyer_id를 임시로) 조회
		String sql = """
            SELECT o.order_id, o.total_payment_amt, o.order_number, u.nickname
            FROM p_order o
            JOIN p_user u ON u.user_id = o.buyer_id
            WHERE o.order_id = ?
        """;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
				new OrderSummary(
					UUID.fromString(rs.getString("order_id")),
					rs.getBigDecimal("total_payment_amt").longValue(),
					rs.getString("order_number"),
					rs.getString("nickname")
				)
			, orderId);
	}

	@Override
	public List<OrderItemSnapshot> getOrderItems(UUID orderId) {
		String sql = """
            SELECT owner_id, order_item_id, subtotal
            FROM p_order_item
            WHERE order_id = ?
        """;

		return jdbcTemplate.query(sql, (rs, rowNum) ->
				new OrderItemSnapshot(
					UUID.fromString(rs.getString("owner_id")),
					UUID.fromString(rs.getString("order_item_id")),
					rs.getLong("subtotal")
				)
			, orderId);
	}
}
