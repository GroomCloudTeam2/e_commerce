#!/bin/bash

# E-Commerce 장바구니 -> 주문 -> (토스 위젯 결제) E2E 테스트 스크립트
# User 회원가입 → 로그인 → 상품조회 → 배송지등록 → 장바구니담기 → 주문생성 → 결제 READY → (브라우저 결제) → 결제 CONFIRM → 결제상태 재조회

set -e

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)_$RANDOM

# ✅ 네가 만든 결제 테스트 HTML 경로 (Spring Boot static으로 서빙된다고 가정)
# 예) src/main/resources/static/toss-test.html
TOSS_TEST_PAGE="$BASE_URL/pay.html"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 유틸리티 함수
print_step() {
  echo ""
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}========================================${NC}"
}
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; exit 1; }
print_info() { echo -e "${YELLOW}ℹ️  $1${NC}"; }

# JSON에서 값 추출 (jq 사용)
extract_json() {
  echo "$1" | jq -r "$2"
}

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    print_error "jq가 설치되어있지 않습니다. (mac: brew install jq)"
  fi
}

open_browser() {
  local url="$1"
  if command -v open >/dev/null 2>&1; then
    open "$url"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url"
  else
    print_info "브라우저 자동 열기 명령어(open/xdg-open)가 없어 URL만 출력합니다."
    echo "$url"
  fi
}

# ============================================
# 0. 사전 체크
# ============================================
require_jq

print_step "0. 서버 상태 확인"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/categories" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" != "200" ]; then
  print_error "서버가 실행되지 않았습니다. (HTTP: $HTTP_STATUS)"
fi
print_success "서버 정상 작동 중"

# ============================================
# 1. User 회원가입
# ============================================
print_step "1. User 회원가입"
USER_EMAIL="cart_user_${TIMESTAMP}@example.com"

SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$USER_EMAIL\",
    \"password\": \"password123\",
    \"nickname\": \"장바구니테스트_${TIMESTAMP}\",
    \"phoneNumber\": \"010-9876-5432\",
    \"role\": \"USER\"
  }")

HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -1)
if [ "$HTTP_CODE" == "201" ]; then
  print_success "User 회원가입 완료: $USER_EMAIL"
elif [ "$HTTP_CODE" == "409" ]; then
  print_info "User 계정이 이미 존재합니다. 기존 계정 사용: $USER_EMAIL"
else
  print_error "User 회원가입 실패 (HTTP: $HTTP_CODE)"
fi

# ============================================
# 2. User 로그인
# ============================================
print_step "2. User 로그인"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USER_EMAIL\", \"password\": \"password123\"}")

USER_TOKEN=$(extract_json "$LOGIN_RESPONSE" '.accessToken')

if [ -z "$USER_TOKEN" ] || [ "$USER_TOKEN" == "null" ]; then
  echo "Response: $LOGIN_RESPONSE"
  print_error "User 로그인 실패"
fi
print_success "User 토큰 발급 완료"

# ============================================
# 3. 상품 목록 조회
# ============================================
print_step "3. 상품 목록 조회"
PRODUCTS=$(curl -s "$BASE_URL/api/v1/products")
PRODUCT_ID=$(extract_json "$PRODUCTS" '.content[0].productId')

if [ -z "$PRODUCT_ID" ] || [ "$PRODUCT_ID" == "null" ]; then
  print_error "상품이 없습니다. 먼저 상품 등록용 스크립트를 실행하여 상품을 등록해주세요."
fi
print_success "상품 ID: $PRODUCT_ID"

# ============================================
# 4. 상품 상세 조회 (Variant 확인)
# ============================================
print_step "4. 상품 상세 조회"
PRODUCT_DETAIL=$(curl -s "$BASE_URL/api/v1/products/$PRODUCT_ID")
PRODUCT_TITLE=$(extract_json "$PRODUCT_DETAIL" '.title')

VARIANT_1_ID=$(extract_json "$PRODUCT_DETAIL" '.variants[0].variantId')
VARIANT_1_NAME=$(extract_json "$PRODUCT_DETAIL" '.variants[0].optionName')
VARIANT_1_PRICE=$(extract_json "$PRODUCT_DETAIL" '.variants[0].price')

VARIANT_2_ID=$(extract_json "$PRODUCT_DETAIL" '.variants[1].variantId')
VARIANT_2_NAME=$(extract_json "$PRODUCT_DETAIL" '.variants[1].optionName')
VARIANT_2_PRICE=$(extract_json "$PRODUCT_DETAIL" '.variants[1].price')

if [ -z "$VARIANT_1_ID" ] || [ "$VARIANT_1_ID" == "null" ]; then
  echo "Response: $PRODUCT_DETAIL"
  print_error "Variant 조회 실패"
fi

print_success "상품명: $PRODUCT_TITLE"
print_success "Variant 1: $VARIANT_1_ID ($VARIANT_1_NAME, ${VARIANT_1_PRICE}원)"
print_success "Variant 2: $VARIANT_2_ID ($VARIANT_2_NAME, ${VARIANT_2_PRICE}원)"

# ============================================
# 5. 배송지 등록
# ============================================
print_step "5. 배송지 등록"
ADDRESS_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/users/me/addresses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "zipCode": "06100",
    "address": "서울시 강남구 테헤란로",
    "detailAddress": "123번지 APT 101호",
    "recipient": "홍길동",
    "recipientPhone": "010-1111-2222",
    "isDefault": true
  }')

HTTP_CODE=$(echo "$ADDRESS_CREATE_RESPONSE" | tail -1)
if [ "$HTTP_CODE" != "200" ]; then
  echo "Response: $(echo "$ADDRESS_CREATE_RESPONSE" | sed '$d')"
  print_error "배송지 등록 실패 (HTTP: $HTTP_CODE)"
fi
print_success "배송지 등록 완료"

# ============================================
# 6. 배송지 목록 조회
# ============================================
print_step "6. 배송지 목록 조회"
ADDRESS_LIST=$(curl -s "$BASE_URL/api/v1/users/me/addresses" -H "Authorization: Bearer $USER_TOKEN")
ADDRESS_ID=$(extract_json "$ADDRESS_LIST" '.[0].id')

if [ -z "$ADDRESS_ID" ] || [ "$ADDRESS_ID" == "null" ]; then
  echo "Response: $ADDRESS_LIST"
  print_error "배송지 조회 실패"
fi
print_success "배송지 ID: $ADDRESS_ID"

# ============================================
# 7. 장바구니에 첫 번째 상품 담기
# ============================================
print_step "7. 장바구니에 첫 번째 상품 담기"
CART_ADD_1=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/cart/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"productId\": \"$PRODUCT_ID\",
    \"variantId\": \"$VARIANT_1_ID\",
    \"quantity\": 1
  }")

HTTP_CODE=$(echo "$CART_ADD_1" | tail -1)
if [ "$HTTP_CODE" != "201" ]; then
  echo "Response: $(echo "$CART_ADD_1" | sed '$d')"
  print_error "장바구니 담기 실패 (HTTP: $HTTP_CODE)"
fi
print_success "장바구니 담기 완료: $VARIANT_1_NAME x 1개"

# ============================================
# 8. 장바구니에 두 번째 상품 담기
# ============================================
print_step "8. 장바구니에 두 번째 상품 담기"
CART_ADD_2=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/cart/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"productId\": \"$PRODUCT_ID\",
    \"variantId\": \"$VARIANT_2_ID\",
    \"quantity\": 2
  }")

HTTP_CODE=$(echo "$CART_ADD_2" | tail -1)
if [ "$HTTP_CODE" != "201" ]; then
  echo "Response: $(echo "$CART_ADD_2" | sed '$d')"
  print_error "장바구니 담기 실패 (HTTP: $HTTP_CODE)"
fi
print_success "장바구니 담기 완료: $VARIANT_2_NAME x 2개"

# ============================================
# 9. 장바구니 조회
# ============================================
print_step "9. 장바구니 조회"
CART_LIST=$(curl -s "$BASE_URL/api/v1/cart" -H "Authorization: Bearer $USER_TOKEN")

CART_COUNT=$(echo "$CART_LIST" | jq 'length')
CART_ITEM_1=$(extract_json "$CART_LIST" '.[0].cartItemId')
CART_ITEM_2=$(extract_json "$CART_LIST" '.[1].cartItemId')
CART_TOTAL=$(echo "$CART_LIST" | jq '[.[].totalPrice] | add')

if [ "$CART_COUNT" != "2" ]; then
  echo "Response: $CART_LIST"
  print_error "장바구니 아이템 수가 올바르지 않습니다: $CART_COUNT"
fi

print_success "장바구니 아이템 수: $CART_COUNT"
print_success "장바구니 총 금액: ${CART_TOTAL}원"
print_info "Cart Item 1: $CART_ITEM_1"
print_info "Cart Item 2: $CART_ITEM_2"

# ============================================
# 10. 장바구니에서 주문 생성
# ============================================
print_step "10. 장바구니에서 주문 생성"
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"addressId\": \"$ADDRESS_ID\",
    \"items\": [
      {\"productId\": \"$PRODUCT_ID\", \"variantId\": \"$VARIANT_1_ID\", \"quantity\": 1},
      {\"productId\": \"$PRODUCT_ID\", \"variantId\": \"$VARIANT_2_ID\", \"quantity\": 2}
    ],
    \"fromCartItemsIds\": [\"$CART_ITEM_1\", \"$CART_ITEM_2\"]
  }")

ORDER_ID=$(echo "$ORDER_RESPONSE" | tr -d '"')
if [[ ! "$ORDER_ID" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]]; then
  echo "Response: $ORDER_RESPONSE"
  print_error "주문 생성 실패"
fi
print_success "주문 생성 완료: $ORDER_ID"

# ============================================
# 11. 장바구니 비움 확인
# ============================================
print_step "11. 장바구니 비움 확인"
CART_AFTER=$(curl -s "$BASE_URL/api/v1/cart" -H "Authorization: Bearer $USER_TOKEN")
CART_COUNT_AFTER=$(echo "$CART_AFTER" | jq 'length')

if [ "$CART_COUNT_AFTER" == "0" ]; then
  print_success "장바구니가 정상적으로 비워졌습니다!"
else
  echo "Response: $CART_AFTER"
  print_error "장바구니에 아직 아이템이 남아있습니다: $CART_COUNT_AFTER개"
fi

# ============================================
# 12. 주문 상세 조회
# ============================================
print_step "12. 주문 상세 조회"
ORDER_DETAIL=$(curl -s "$BASE_URL/api/v1/orders/$ORDER_ID" -H "Authorization: Bearer $USER_TOKEN")

ORDER_STATUS=$(extract_json "$ORDER_DETAIL" '.status')
ORDER_TOTAL=$(extract_json "$ORDER_DETAIL" '.totalAmount')
ORDER_NO=$(extract_json "$ORDER_DETAIL" '.orderNo')

if [ -z "$ORDER_TOTAL" ] || [ "$ORDER_TOTAL" == "null" ]; then
  echo "Response: $ORDER_DETAIL"
  print_error "주문 상세 조회에서 totalAmount를 못 가져왔습니다."
fi

print_success "주문번호: $ORDER_NO"
print_success "주문상태: $ORDER_STATUS"
print_success "총 결제금액: ${ORDER_TOTAL}원"

# ============================================
# 13. 결제 상태 조회 (주문 생성 직후)
# ============================================
print_step "13. 결제 상태 조회 (주문 생성 직후)"
PAYMENT_DETAIL=$(curl -s "$BASE_URL/api/v1/payments/by-order/$ORDER_ID" -H "Authorization: Bearer $USER_TOKEN")

PAYMENT_STATUS=$(extract_json "$PAYMENT_DETAIL" '.status')
PAYMENT_AMOUNT=$(extract_json "$PAYMENT_DETAIL" '.amount')
PG_PROVIDER=$(extract_json "$PAYMENT_DETAIL" '.pgProvider')

print_success "결제상태: $PAYMENT_STATUS"
print_success "결제금액: ${PAYMENT_AMOUNT}원"
print_success "PG사: $PG_PROVIDER"

# ============================================
# 14. 결제 준비 (READY) - 서버 READY + 브라우저 결제 진행
#   - PaymentCommandService.ready(ReqReadyPaymentV1) 기준: {orderId, amount} 필요
# ============================================
print_step "14. 결제 준비 (READY)"

READY_API_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/payments/ready" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"amount\": $ORDER_TOTAL
  }")

READY_HTTP_CODE=$(echo "$READY_API_RESPONSE" | tail -1)
READY_BODY=$(echo "$READY_API_RESPONSE" | sed '$d')

if [ "$READY_HTTP_CODE" != "200" ]; then
  echo "Response: $READY_BODY"
  print_error "결제 READY 실패 (HTTP: $READY_HTTP_CODE)"
fi

READY_AMOUNT=$(extract_json "$READY_BODY" '.amount')
CLIENT_KEY=$(extract_json "$READY_BODY" '.clientKey')
SUCCESS_URL=$(extract_json "$READY_BODY" '.successUrl')
FAIL_URL=$(extract_json "$READY_BODY" '.failUrl')

print_success "결제 READY 완료"
print_info "READY amount: $READY_AMOUNT"
print_info "successUrl : $SUCCESS_URL"
print_info "failUrl    : $FAIL_URL"

# ✅ 브라우저에서 결제 페이지 자동 오픈 (orderId/amount 자동 입력)
PAY_URL="${TOSS_TEST_PAGE}?orderId=${ORDER_ID}&amount=${ORDER_TOTAL}&auto=0"
print_info "브라우저에서 아래 결제 테스트 페이지를 열어 결제를 완료하세요:"
echo "$PAY_URL"
open_browser "$PAY_URL"

print_info "결제 완료 후 successUrl에 포함된 paymentKey 값을 확인해서 입력하세요."
read -p "paymentKey 입력: " PAYMENT_KEY
if [ -z "$PAYMENT_KEY" ]; then
  print_error "paymentKey가 비어있습니다."
fi

# ============================================
# 15. 결제 승인 (CONFIRM)
#   - PaymentCommandService.confirm(ReqConfirmPaymentV1) 기준: {orderId, paymentKey, amount}
# ============================================
print_step "15. 결제 승인 (CONFIRM)"
CONFIRM_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/payments/confirm" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"paymentKey\": \"$PAYMENT_KEY\",
    \"amount\": $ORDER_TOTAL
  }")

CONFIRM_HTTP_CODE=$(echo "$CONFIRM_RESPONSE" | tail -1)
CONFIRM_BODY=$(echo "$CONFIRM_RESPONSE" | sed '$d')

if [ "$CONFIRM_HTTP_CODE" != "200" ]; then
  echo "Response: $CONFIRM_BODY"
  print_error "결제 CONFIRM 실패 (HTTP: $CONFIRM_HTTP_CODE)"
fi

CONFIRM_STATUS=$(extract_json "$CONFIRM_BODY" '.status')
print_success "결제 CONFIRM 완료 (status: $CONFIRM_STATUS)"

# ============================================
# 16. 결제 상태 재조회 (승인 후)
# ============================================
print_step "16. 결제 상태 재조회 (승인 후)"
PAYMENT_DETAIL_AFTER=$(curl -s "$BASE_URL/api/v1/payments/by-order/$ORDER_ID" -H "Authorization: Bearer $USER_TOKEN")

PAYMENT_STATUS_AFTER=$(extract_json "$PAYMENT_DETAIL_AFTER" '.status')
PAYMENT_AMOUNT_AFTER=$(extract_json "$PAYMENT_DETAIL_AFTER" '.amount')
PG_PROVIDER_AFTER=$(extract_json "$PAYMENT_DETAIL_AFTER" '.pgProvider')
PAYMENT_KEY_AFTER=$(extract_json "$PAYMENT_DETAIL_AFTER" '.paymentKey')

print_success "결제상태(After): $PAYMENT_STATUS_AFTER"
print_success "결제금액(After): ${PAYMENT_AMOUNT_AFTER}원"
print_success "PG사(After): $PG_PROVIDER_AFTER"
print_success "paymentKey(After): $PAYMENT_KEY_AFTER"

# ============================================
# 테스트 결과 요약
# ============================================
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   🛒 주문→결제 E2E 테스트 완료! 🛒     ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}📋 테스트 결과 요약${NC}"
echo "────────────────────────────────────────"
echo -e "User Email      : ${BLUE}$USER_EMAIL${NC}"
echo -e "상품 ID         : ${BLUE}$PRODUCT_ID${NC}"
echo -e "Variant 1       : ${BLUE}$VARIANT_1_NAME${NC}"
echo -e "Variant 2       : ${BLUE}$VARIANT_2_NAME${NC}"
echo -e "주문 ID         : ${BLUE}$ORDER_ID${NC}"
echo -e "주문번호        : ${BLUE}$ORDER_NO${NC}"
echo -e "주문상태        : ${BLUE}$ORDER_STATUS${NC}"
echo -e "결제상태(전)    : ${BLUE}$PAYMENT_STATUS${NC}"
echo -e "결제상태(후)    : ${BLUE}$PAYMENT_STATUS_AFTER${NC}"
echo -e "총 결제금액     : ${BLUE}${ORDER_TOTAL}원${NC}"
echo -e "장바구니 비움   : ${GREEN}✅ 성공${NC}"
echo "────────────────────────────────────────"
echo ""
echo -e "${YELLOW}💡 테스트 흐름:${NC}"
echo "  회원가입 → 로그인 → 상품조회 → 배송지등록 → 장바구니담기(2개)"
echo "  → 주문생성 → 결제 READY → (브라우저 결제) → 결제 CONFIRM → 결제상태 재조회"
echo ""
