#!/bin/bash

# E-Commerce E2E 테스트 스크립트
# Owner 회원가입 → 상품등록 → User 회원가입 → 로그인 → 배송지등록 → 주문생성

set -e

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)_$RANDOM

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

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# JSON에서 값 추출 (jq 사용)
extract_json() {
    echo "$1" | jq -r "$2"
}

# ============================================
# 0. 서버 상태 확인
# ============================================
print_step "0. 서버 상태 확인"

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/categories" 2>/dev/null || echo "000")

if [ "$HTTP_STATUS" != "200" ]; then
    print_error "서버가 실행되지 않았습니다. (HTTP: $HTTP_STATUS)"
fi
print_success "서버 정상 작동 중"

# ============================================
# 1. 카테고리 조회
# ============================================
print_step "1. 카테고리 목록 조회"

CATEGORY_RESPONSE=$(curl -s "$BASE_URL/api/v1/categories")
CATEGORY_ID=$(extract_json "$CATEGORY_RESPONSE" '.[0].id')

if [ -z "$CATEGORY_ID" ] || [ "$CATEGORY_ID" == "null" ]; then
    print_error "카테고리가 없습니다. 먼저 카테고리를 생성해주세요."
fi

print_success "카테고리 ID: $CATEGORY_ID"

# ============================================
# 2. Owner 회원가입
# ============================================
print_step "2. Owner 회원가입"

OWNER_EMAIL="owner_test_${TIMESTAMP}@example.com"

SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"$OWNER_EMAIL\",
        \"password\": \"password123\",
        \"nickname\": \"판매자_${TIMESTAMP}\",
        \"phoneNumber\": \"010-1234-5678\",
        \"role\": \"OWNER\",
        \"store\": \"테스트 상점\",
        \"zipCode\": \"12345\",
        \"address\": \"서울시 강남구\",
        \"detailAddress\": \"테헤란로 123\",
        \"bank\": \"신한은행\",
        \"account\": \"110-123-456789\",
        \"approvalRequest\": \"판매자 승인 요청\"
    }")

HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -1)
if [ "$HTTP_CODE" == "201" ]; then
    print_success "Owner 회원가입 완료: $OWNER_EMAIL"
elif [ "$HTTP_CODE" == "409" ]; then
    print_info "Owner 계정이 이미 존재합니다. 기존 계정 사용: $OWNER_EMAIL"
else
    print_error "Owner 회원가입 실패 (HTTP: $HTTP_CODE)"
fi

# ============================================
# 3. Owner 로그인
# ============================================
print_step "3. Owner 로그인"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$OWNER_EMAIL\", \"password\": \"password123\"}")

OWNER_TOKEN=$(extract_json "$LOGIN_RESPONSE" '.accessToken')

if [ -z "$OWNER_TOKEN" ] || [ "$OWNER_TOKEN" == "null" ]; then
    print_error "Owner 로그인 실패"
fi

print_success "Owner 토큰 발급 완료"

# ============================================
# 4. 옵션이 있는 상품 등록
# ============================================
print_step "4. 옵션이 있는 상품 등록"

PRODUCT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/owner/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OWNER_TOKEN" \
    -d "{
        \"categoryId\": \"$CATEGORY_ID\",
        \"title\": \"테스트 신발 $TIMESTAMP\",
        \"description\": \"편안한 운동화입니다\",
        \"thumbnailUrl\": \"https://example.com/shoe.jpg\",
        \"hasOptions\": true,
        \"options\": [
            {
                \"name\": \"사이즈\",
                \"sortOrder\": 1,
                \"values\": [
                    {\"value\": \"260\", \"sortOrder\": 1},
                    {\"value\": \"270\", \"sortOrder\": 2},
                    {\"value\": \"280\", \"sortOrder\": 3}
                ]
            },
            {
                \"name\": \"색상\",
                \"sortOrder\": 2,
                \"values\": [
                    {\"value\": \"화이트\", \"sortOrder\": 1},
                    {\"value\": \"블랙\", \"sortOrder\": 2}
                ]
            }
        ],
        \"variants\": [
            {\"optionValueIndexes\": [0, 0], \"skuCode\": \"SHOE-260-WHITE-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 10},
            {\"optionValueIndexes\": [0, 1], \"skuCode\": \"SHOE-260-BLACK-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 10},
            {\"optionValueIndexes\": [1, 0], \"skuCode\": \"SHOE-270-WHITE-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 15},
            {\"optionValueIndexes\": [1, 1], \"skuCode\": \"SHOE-270-BLACK-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 15},
            {\"optionValueIndexes\": [2, 0], \"skuCode\": \"SHOE-280-WHITE-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 5},
            {\"optionValueIndexes\": [2, 1], \"skuCode\": \"SHOE-280-BLACK-$TIMESTAMP\", \"price\": 89000, \"stockQuantity\": 5}
        ]
    }")

PRODUCT_ID=$(extract_json "$PRODUCT_RESPONSE" '.productId')

if [ -z "$PRODUCT_ID" ] || [ "$PRODUCT_ID" == "null" ]; then
    echo "Response: $PRODUCT_RESPONSE"
    print_error "상품 등록 실패"
fi

print_success "상품 등록 완료: $PRODUCT_ID"

# ============================================
# 5. 상품 상세 조회 (Variant ID 확인)
# ============================================
print_step "5. 상품 상세 조회"

PRODUCT_DETAIL=$(curl -s "$BASE_URL/api/v1/products/$PRODUCT_ID")
VARIANT_ID=$(extract_json "$PRODUCT_DETAIL" '.variants[0].variantId')
VARIANT_NAME=$(extract_json "$PRODUCT_DETAIL" '.variants[0].optionName')
VARIANT_PRICE=$(extract_json "$PRODUCT_DETAIL" '.variants[0].price')

if [ -z "$VARIANT_ID" ] || [ "$VARIANT_ID" == "null" ]; then
    print_error "Variant 조회 실패"
fi

print_success "상품명: $(extract_json "$PRODUCT_DETAIL" '.title')"
print_success "Variant ID: $VARIANT_ID ($VARIANT_NAME, ${VARIANT_PRICE}원)"
