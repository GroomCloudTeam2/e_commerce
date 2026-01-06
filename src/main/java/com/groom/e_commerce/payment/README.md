# 💳 E-Commerce Payment Domain

본 프로젝트는 이커머스 플랫폼의 **결제 도메인**을 중심으로  
토스페이먼츠(Toss Payments)를 연동하여  
**결제 준비(READY) → 승인(CONFIRM) → 취소(CANCEL)**  
전체 결제 흐름을 백엔드 기준으로 구현한 모듈입니다.

프론트엔드 없이도 결제 플로우를 검증할 수 있도록  
**정적 HTML 기반 테스트 환경**을 함께 구성했습니다.

---

## 📌 설계 목표

- 결제 도메인을 주문, 회원 등 다른 도메인과 **명확히 분리**
- 외부 PG(토스페이먼츠)와의 의존성을 **포트/어댑터 구조로 격리**
- 금액 위변조 방지, 상태 전이, 멱등성 처리를 고려한 **안정적인 결제 설계**
- 프론트엔드 없이도 **백엔드 단독 테스트 가능**한 구조 확보

---

## 🧱 프로젝트 구조

```text
src
└─ main
   ├─ java
   │  └─ com
   │     └─ groom
   │        └─ e_commerce
   │           ├─ ECommerceApplication.java        # Spring Boot 애플리케이션 시작 클래스
   │           │
   │           └─ payment                          # 결제 도메인 루트
   │              ├─ domain                        # 결제 핵심 비즈니스 도메인
   │              │  ├─ entity
   │              │  │  ├─ Payment.java            # 주문 1건에 대한 결제 엔티티 (READY/PAID/CANCELED)
   │              │  │  └─ PaymentCancel.java      # 결제 취소 이력 엔티티 (부분/전체 취소)
   │              │  │  └─ PaymentSplit.java       # 주문상품 단위 결제 분할 엔티티(정산/부분취소 기준 라인)
   │              │  ├─ model
   │              │  │  ├─ PaymentMethod.java      # 결제 수단 Enum
   │              │  │  ├─ PaymentSplitStatus.java # 결제 분할(PaymentSplit) 상태 Enum (ACTIVE/CANCELED 등)
   │              │  │  └─ PaymentStatus.java      # 결제 상태 Enum
   │              │  └─ repository
   │              │     └─ PaymentRepository.java        # 결제 도메인 Repository 인터페이스
   │              │     └─ PaymentSplitRepository.java   # 결제 분할(PaymentSplit) Repository 인터페이스
   │              │
   │              ├─ infrastructure                # 외부 시스템 연동 및 구현 영역
   │              │  ├─ adapter.in
   │              │  │  └─ PaymentPortAdapter.java # 주문 도메인의 PaymentPort를 구현해 결제 부분취소 요청을 수신/위임
   │              │  ├─ api
   │              │  │  └─ toss                    # 토스페이먼츠 API 연동
   │              │  │     ├─ adapter
   │              │  │     │  └─ TossPaymentAdapter.java   # TossPaymentPort 구현체(토스 API 호출 위임)
   │              │  │     ├─ config
   │              │  │     │  ├─ TossWebClientConfig.java  # 토스 API 호출용 WebClient 설정
   │              │  │     │  └─ TossPaymentsProperties.java # 토스 결제 환경설정 바인딩
   │              │  │     ├─ client
   │              │  │     │  └─ TossPaymentsClient.java   # 토스페이먼츠 REST API 호출 클라이언트(WebClient 래퍼)
   │              │  │     └─ dto
   │              │  │        ├─ request
   │              │  │        │  ├─ TossConfirmRequest.java # 토스 결제 승인 요청 DTO
   │              │  │        │  └─ TossCancelRequest.java  # 토스 결제 취소 요청 DTO
   │              │  │        └─ response
   │              │  │           ├─ TossPaymentResponse.java # 토스 결제 승인 응답 DTO
   │              │  │           ├─ TossCancelResponse.java  # 토스 결제 취소 응답 DTO
   │              │  │           └─ TossErrorResponse.java   # 토스 API 에러 응답 DTO
   │              │  │
   │              │  ├─ repository
   │              │  │  └─ PaymentRepositoryImpl.java        # PaymentRepository 구현체(EntityManager/JPA)
   │              │  │  └─ PaymentSplitJpaRepository.java    # PaymentSplit 조회용 Spring Data JPA 레포지토리(락 조회 포함)
   │              │  │  └─ PaymentSplitRepositoryImpl.java   # PaymentSplitRepository 구현체(EntityManager 기반, 락 조회 지원)
   │              │  └─ stub
   │              │     └─ StubOrderQueryAdapter.java        # 주문 도메인 미구현 시 주문조회(OrderQueryPort) 테스트용 스텁
   │              │
   │              ├─ application                   # 결제 유스케이스 및 서비스 계층
   │              │  ├─ port
   │              │  │  ├─ in                      # 인바운드 유스케이스 인터페이스
   │              │  │  │  ├─ CancelOrderItemPaymentUseCase.java # 주문상품 단위 부분취소 유스케이스
   │              │  │  │  ├─ ConfirmPaymentUseCase.java         # 결제 승인 유스케이스
   │              │  │  │  ├─ CancelPaymentUseCase.java          # 결제 취소 유스케이스(전액/부분)
   │              │  │  │  ├─ GetPaymentUseCase.java             # 결제 조회 유스케이스
   │              │  │  │  └─ ReadyPaymentUseCase.java           # 결제 준비(READY) 유스케이스
   │              │  │  └─ out                     # 아웃바운드 포트
   │              │  │     ├─ OrderItemSnapshot.java # 주문상품 스냅샷 DTO(결제 split 생성/검증에 사용)
   │              │  │     ├─ OrderQueryPort.java     # 주문 도메인 조회 포트(주문 요약/주문상품 조회)
   │              │  │     ├─ OrderStatePort.java     # 주문 상태 변경 포트(PENDING→PAID 등 상태 전이)
   │              │  │     └─ TossPaymentPort.java    # 토스페이먼츠 연동 포트(승인/취소)
   │              │  │
   │              │  └─ service
   │              │     ├─ PaymentCommandService.java # 결제 커맨드 로직(READY/CONFIRM/CANCEL/부분취소 상태 변경)
   │              │     └─ PaymentQueryService.java   # 결제 조회 전용 로직
   │              │
   │              └─ presentation                 # 외부 요청 처리(API 계층)
   │                 ├─ controller
   │                 │  ├─ PaymentCancelItemControllerV1.java  # 주문상품 단위 부분취소 API 컨트롤러
   │                 │  ├─ PaymentControllerV1.java            # 결제 REST API 컨트롤러(ready/confirm/cancel 등)
   │                 │  └─ PaymentRedirectController.java      # 토스 결제 결과 리다이렉트 처리(성공/실패 URL)
   │                 ├─ dto
   │                 │  ├─ request
   │                 │  │  ├─ ReqCancelOrderItemPaymentV1.java  # 주문상품 단위 부분취소 요청 DTO
   │                 │  │  ├─ ReqConfirmPaymentV1.java          # 결제 승인 요청 DTO
   │                 │  │  ├─ ReqCancelPaymentV1.java           # 결제 취소 요청 DTO
   │                 │  │  └─ ReqReadyPaymentV1.java            # 결제 준비 요청 DTO
   │                 │  └─ response
   │                 │     ├─ ResPaymentV1.java        # 결제 승인 응답 DTO
   │                 │     ├─ ResCancelResultV1.java   # 결제 취소/부분취소 결과 응답 DTO
   │                 │     ├─ ResErrorV1.java          # 결제 공통 에러 응답 DTO
   │                 │     └─ ResReadyPaymentV1.java   # 결제 준비 응답 DTO
   │                 └─ exception
   │                    ├─ PaymentException.java      # 결제 도메인 비즈니스 예외(상태/금액 검증 실패 등)
   │                    └─ TossApiException.java      # 토스 API 호출 예외(외부 PG 오류 매핑)
   │
   └─ resources
      ├─ static
      │  ├─ pay.html             # 프론트 없이 결제 요청을 테스트하는 정적 페이지
      │  ├─ pay-fail.html        # 결제 실패 리다이렉트 테스트 페이지
      │  └─ pay-success.html     # 결제 성공 리다이렉트 테스트 페이지
      └─ application.yml         # 애플리케이션 및 토스 결제 환경 설정


```
## 🔄 결제 처리 흐름
1️⃣ 결제 준비 (READY)
- 주문 존재 여부 확인 (OrderQueryPort)
- 주문 금액과 요청 금액 비교를 통한 위변조 검증
- 결제 상태 READY 확인
- 토스 결제창 호출에 필요한 정보 반환

2️⃣ 결제 승인 (CONFIRM)
- 결제 멱등성 처리 (중복 승인 방지)
- 내부 결제 금액 검증
- 토스페이먼츠 승인 API 호출
- 승인 성공 시 결제 상태 PAID로 변경

3️⃣ 결제 취소 (CANCEL)
- 부분/전체 취소 지원
- 취소 가능 금액 검증
- 토스 취소 API 연동
- 이미 취소된 결제에 대한 멱등 처리
---
