#!/bin/bash

# 카테고리 생성 스크립트 (Master 계정 사용)

set -e

BASE_URL="http://localhost:8080"
MASTER_EMAIL="master@example.com"
MASTER_PASSWORD="password123"

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

# 1. Master 로그인
echo "logging in as Master..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$MASTER_EMAIL\", \"password\": \"$MASTER_PASSWORD\"}")

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    print_error "Login Failed. Please check if Master account exists and password is correct."
fi

print_success "Login Successful. Token acquired."

# 2. 카테고리 생성 요청
echo "Creating Category..."

CATEGORY_NAME="New Category $(date +%s)"

CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/master/categories" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"$CATEGORY_NAME\",
        \"depth\": 1,
        \"sortOrder\": 1,
        \"isActive\": true
    }")

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -1)
RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "201" ]; then
    print_success "Category Created Successfully!"
    echo "Category Name: $CATEGORY_NAME"
else
    print_error "Category Creation Failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY" | jq .
fi
