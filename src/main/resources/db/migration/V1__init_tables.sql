-- 사용자 테이블
CREATE TABLE users
(
	id           BIGINT AUTO_INCREMENT PRIMARY KEY,    -- 식별자 ID
	name         VARCHAR(50)  NOT NULL,                -- 이름
	email        VARCHAR(50)  NOT NULL,                -- 이메일
	password     VARCHAR(255) NOT NULL,                -- 비밀번호
	phone_number VARCHAR(20)  NOT NULL,                -- 핸드폰 번호
	role         VARCHAR(20)  NOT NULL DEFAULT 'USER', -- 권한
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	CONSTRAINT uq_users_email        UNIQUE (email),
	CONSTRAINT uq_users_phone_number UNIQUE (phone_number),

	CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 영화 테이블
CREATE TABLE movies
(
	id          BIGINT AUTO_INCREMENT PRIMARY KEY, -- 식별자 ID 
	title       VARCHAR(100) NOT NULL,             -- 제목
	duration    INT          NOT NULL,             -- 상영 시간 (분 단위)
	description TEXT         NOT NULL,             -- 설명
	poster_url  VARCHAR(500) NOT NULL,             -- 포스터 파일 URL
	status      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE', -- 상태
	created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	INDEX idx_movies_title (title),

    CONSTRAINT chk_movies_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 장르 테이블
CREATE TABLE genres
(
	id   BIGINT AUTO_INCREMENT PRIMARY KEY, -- 식별자 ID
	name VARCHAR(50) NOT NULL,              -- 장르

	CONSTRAINT uq_genres_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 영화-장르 관계 테이블
CREATE TABLE movie_genres
(
	movie_id BIGINT NOT NULL,
	genre_id BIGINT NOT NULL,

	PRIMARY KEY (movie_id, genre_id),

	CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
	CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 영화-타입 관계 테이블
CREATE TABLE movie_types
(
	movie_id BIGINT      NOT NULL, -- 영화 식별자 ID
	type     VARCHAR(50) NOT NULL, -- 영화 타입(2D, 3D)

	PRIMARY KEY (movie_id, type),

	CONSTRAINT fk_movie_types_movie FOREIGN KEY (movie_id) REFERENCES movies(id),

	CONSTRAINT chk_movie_types_type CHECK (type IN ('TWO_D', 'THREE_D'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 상영관 테이블
CREATE TABLE theaters
(
	id         BIGINT PRIMARY KEY,                   -- 식별자 ID
	name       VARCHAR(10) NOT NULL,                 -- 상영관 이름
	total_seat INT         NOT NULL,                 -- 총 좌석 수
	type       VARCHAR(10) NOT NULL DEFAULT 'TWO_D', -- 상영관 타입(2D, 3D, 4D)
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	CONSTRAINT uq_theaters_name UNIQUE (name),

	CONSTRAINT chk_theaters_type   CHECK (type IN ('TWO_D', 'FOUR_DX', 'IMAX', 'SCREEN_X'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 좌석
CREATE TABLE seats
(
	id         BIGINT AUTO_INCREMENT PRIMARY KEY,     -- 식별자 ID
	theater_id BIGINT      NOT NULL,                  -- 상영관 식별자 ID
	row_label  VARCHAR(1)  NOT NULL,                  -- 행
	col_number INT         NOT NULL,                  -- 번호
	type       VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- 좌석 타입(일반/프리미엄/룸)
    created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	CONSTRAINT fk_seats_theater FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE,

	CONSTRAINT uq_seats_theater_row_col UNIQUE (theater_id, row_label, col_number),

	CONSTRAINT chk_seats_type CHECK (type IN ('NORMAL', 'PREMIUM', 'ROOM')),

	INDEX idx_seats_theater (theater_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 상영
CREATE TABLE screenings
(
	id         BIGINT AUTO_INCREMENT PRIMARY KEY, -- 식별자 ID 
	movie_id   BIGINT    NOT NULL,                -- 영화 식별자 ID
	theater_id BIGINT    NOT NULL,                -- 상영관 식별자 ID
	start_time DATETIME  NOT NULL,                -- 상영 시작 시간
	end_time   DATETIME  NOT NULL,                -- 상영 종료 시간
	sequence   INT       NOT NULL,                -- 상영회차 (1, 2, 3 ...)
    status     VARCHAR(10) NOT NULL,              -- 상태
    movie_type VARCHAR(50) NOT NULL,              -- 영화 타입 (2D, 3D)

	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	CONSTRAINT fk_screenings_movie   FOREIGN KEY (movie_id)   REFERENCES movies(id),
	CONSTRAINT fk_screenings_theater FOREIGN KEY (theater_id) REFERENCES theaters(id),

	CONSTRAINT uq_screenings_sequence UNIQUE (movie_id, sequence),

    CONSTRAINT chk_screenings_status     CHECK (status IN ('SCHEDULED', 'CANCELED', 'ENDED')),
    CONSTRAINT chk_screenings_movie_type CHECK (movie_type IN ('TWO_D', 'THREE_D')),

    INDEX idx_screenings_status_start (status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 상영-좌석 관계 테이블
CREATE TABLE screening_seats
(
	id           BIGINT AUTO_INCREMENT PRIMARY KEY, -- 식별자 ID
	screening_id BIGINT      NOT NULL,              -- 상영 식별자 ID
	seat_id      BIGINT      NOT NULL,              -- 좌석 식별자 ID
	status       VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- 좌석 상태
	price        INT         NOT NULL,              -- 좌석별 가격
	
	CONSTRAINT fk_screening_seats_screening FOREIGN KEY (screening_id) REFERENCES screenings(id),
	CONSTRAINT fk_screening_seats_seat      FOREIGN KEY (seat_id)      REFERENCES seats(id),

	CONSTRAINT uq_screening_seats_screen_seat UNIQUE (screening_id, seat_id),

	CONSTRAINT chk_screening_seats_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'FIXING')),

	INDEX idx_screening_seats_screen (screening_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 예약 그룹 테이블
CREATE TABLE reservation_groups
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,      -- 식별자 ID
    user_id     BIGINT      NOT NULL,                   -- 회원 식별자 ID
    total_price INT         NOT NULL,                   -- 총 가격
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 예약 상태
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 종료 시각

    CONSTRAINT chk_reservation_groups_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),

    INDEX idx_reservation_groups_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 예약 테이블
CREATE TABLE reservations
(
	id                   BIGINT AUTO_INCREMENT PRIMARY KEY, -- 식별자 ID 
	reservation_group_id BIGINT NOT NULL, -- 예약 그룹 식별자 ID
	screening_seat_id    BIGINT NOT NULL, -- 상영-좌석 식별자 ID

	CONSTRAINT fk_reservations_reservation_group FOREIGN KEY (reservation_group_id) REFERENCES reservation_groups(id),
	CONSTRAINT fk_reservations_screening_seat    FOREIGN KEY (screening_seat_id)    REFERENCES screening_seats(id),

	INDEX idx_reservations_reservation_group (reservation_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 결제 테이블
CREATE TABLE payments
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,    -- 식별자 ID
    reservation_group_id BIGINT NOT NULL,                      -- 예약 그룹 식별자 ID

    buyer_name           VARCHAR(50) NOT NULL,                 -- 거래자명
    buyer_phone_number   VARCHAR(20) NOT NULL,                 -- 거래자 휴대폰 번호
    buyer_email          VARCHAR(50) NOT NULL,                 -- 거래자 이메일

    payment_id           VARCHAR(100),                         -- 결제 고유번호

    expected_amount      DECIMAL(15, 2) NOT NULL,              -- 기대 금액
    paid_amount          DECIMAL(15, 2),                       -- 실제 결제 금액
    refund_amount        DECIMAL(15, 2),                       -- 환불 금액

    status               VARCHAR(30) NOT NULL DEFAULT 'READY', -- 결제 상태

    pg_provider          VARCHAR(50),                          -- 결제 PG사
    pay_method           VARCHAR(30),                          -- 결제 수단
    card_name            VARCHAR(50),                          -- 카드사명
    card_quota           INT,                                  -- 할부개월

    fail_reason          TEXT,                                 -- 실패 사유
    cancel_reason        TEXT,                                 -- 취소 사유

    paid_at              DATETIME,                             -- 결제 완료 시점
    cancelled_at         DATETIME,                             -- 결제 취소 시점

    created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 시각
    updated_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 종료 시각

    CONSTRAINT fk_payments_reservation_group FOREIGN KEY (reservation_group_id) REFERENCES reservation_groups(id),

    CONSTRAINT chk_payments_status CHECK (status IN ('READY', 'COMPLETED', 'FAILED', 'FAILED_VERIFICATION', 'CANCELLED')),

    CONSTRAINT uq_payments_payment_id   UNIQUE (payment_id),

    INDEX idx_payments_reservation_group (reservation_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;