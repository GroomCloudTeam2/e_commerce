package com.groom.e_commerce.e2e.scenario;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.groom.e_commerce.e2e.E2ETestSupport;

import io.restassured.http.ContentType;

@Tag("e2e")
class PurchaseToReviewE2ETest extends E2ETestSupport {

    @Test
    void 구매부터_리뷰까지_E2E_시나리오() {

        /* =========================
         * 1️⃣ 구매자 로그인
         * ========================= */
        String userToken =
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "email": "buyer@test.com",
                      "password": "1234"
                    }
                """)
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

        /* =========================
         * 2️⃣ 상품 조회
         * ========================= */
        UUID productId =
            given()
            .when()
                .get("/api/v1/products")
            .then()
                .statusCode(200)
                .extract()
                .path("content[0].productId");

        int beforeStock =
            given()
            .when()
                .get("/api/v1/products/{id}", productId)
            .then()
                .extract()
                .path("stock");

        /* =========================
         * 3️⃣ 장바구니 담기
         * ========================= */
        given()
            .auth().oauth2(userToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "productId": "%s",
                  "quantity": 1
                }
            """.formatted(productId))
        .when()
            .post("/api/v1/orders/cart")
        .then()
            .statusCode(200);

        /* =========================
         * 4️⃣ 장바구니 주문
         * ========================= */
        UUID orderId =
            given()
                .auth().oauth2(userToken)
            .when()
                .post("/api/v1/orders/cart/checkout")
            .then()
                .statusCode(201)
                .extract()
                .path("orderId");

        /* =========================
         * 5️⃣ 결제 승인
         * ========================= */
        given()
            .auth().oauth2(userToken)
        .when()
            .post("/api/v1/payments/{orderId}/confirm", orderId)
        .then()
            .statusCode(200);

        /* =========================
         * 6️⃣ 재고 차감 검증
         * ========================= */
        given()
        .when()
            .get("/api/v1/products/{id}", productId)
        .then()
            .statusCode(200)
            .body("stock", equalTo(beforeStock - 1));

        /* =========================
         * 7️⃣ 리뷰 작성
         * ========================= */
        given()
            .auth().oauth2(userToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "rating": 5,
                  "content": "E2E 구매 후 리뷰"
                }
            """)
        .when()
            .post("/api/v1/reviews/{orderId}/items/{productId}", orderId, productId)
        .then()
            .statusCode(201);

        /* =========================
         * 8️⃣ 리뷰 조회
         * ========================= */
        given()
        .when()
            .get("/api/v1/reviews/product/{productId}", productId)
        .then()
            .statusCode(200)
            .body("reviewCount", greaterThan(0))
            .body("reviews.content", hasItem("E2E 구매 후 리뷰"));
    }
}
