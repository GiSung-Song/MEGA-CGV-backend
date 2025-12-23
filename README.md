# Movie Reservation Backend

영화 예매 서비스의 **백엔드 시스템**으로,  
영화 상영 정보 조회, 좌석 예약, 결제 및 환불(부분 환불 포함)을 지원합니다.

동시 좌석 예약 경쟁 상황, 결제 검증 및 환불 정책 등
**실제 서비스에서 발생할 수 있는 시나리오를 중심으로 설계·구현**했습니다.

---

## API Documentation
Spring RestDocs 기반으로 작성된 API 문서입니다.
모든 문서는 **테스트 코드 실행 결과로 자동 생성**되며,
실제 Request / Response와 항상 동기화됩니다.

==> [API 문서 바로가기](https://gisung-song.github.io/MEGA-CGV-backend/)

---

## 기술 스택

### Backend
- Java
- Spring Boot
- Spring Security (JWT 인증)
- JPA / QueryDSL
- Flyway (DB 마이그레이션)

### Database / Search
- **MySQL**
- **Elasticsearch**
    - 영화 검색 용도
    - Nori tokenizer(형태소) 적용
    - 기본적인 document 생성 / 삭제 수준으로 사용

### Cache
- **Redis**
- **Caffeine**

### Payment
- **PortOne 결제 API (v2)**
    - 결제 검증
    - 부분 환불 / 전액 환불
    - 상영 취소 시 전액 환불 처리

---

## Test
### API Documentation
 - **Spring RestDocs**
     - 테스트 코드 기반 API 문서 자동 생성

### Concurrency Test
 - **k6**
    - 동시 좌석 점유 경쟁 상황 테스트

### Payment
 - **PortOne SDK**
    - 결제 검증 로직 테스트
    - 예약 취소 시 부분 환불
    - 상영 취소 시 전액 환불

---

## 사용자 기능

### 인증 / 계정
- 회원가입
- 로그인
- 비밀번호 변경
- 휴대폰 번호 변경
- 회원 탈퇴

### 영화 / 상영 조회
- 상영 중인 영화 목록 조회
- 영화별 상영 회차 조회
- 좌석 현황 조회

### 예약 / 결제
- 좌석 예약
- 결제 완료 후 예약 확정
- 예약 취소 및 환불

---

## 운영자 기능
- 영화 등록
- 영화 목록 검색
- 상영 가능 회차 조회
- 상영 회차 등록
- 상영 취소
