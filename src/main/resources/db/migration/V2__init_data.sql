-- ========================================
-- 상영관(theater) 기본 데이터
-- ========================================
INSERT INTO theaters (id, name, total_seat, type, base_price, created_at, updated_at)
VALUES
    (1, '1관', 50, 'TWO_D'  , 15000, NOW(), NOW()),
    (2, '2관', 50, 'FOUR_DX', 20000, NOW(), NOW()),
    (3, '3관', 30, 'IMAX'   , 17000, NOW(), NOW()),
    (4, '4관', 20, 'SCREENX', 25000, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    total_seat = VALUES(total_seat),
    type       = VALUES(type),
    base_price = VALUES(base_price),
    updated_at = NOW();


-- =========================================
-- 1관 좌석 (총 50석)
-- A~D 일반(40), E 프리미엄(7), F 룸(3)
-- =========================================
-- 일반좌석
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 1, r, c, 'NORMAL', NOW(), NOW()
FROM (SELECT 'A' AS r UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D') rows
CROSS JOIN (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 프리미엄좌석 (E 1~7)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 1, 'E', c, 'PREMIUM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 룸좌석 (F 1~3)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 1, 'F', c, 'ROOM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =========================================
-- 2관 좌석 (총 50석)
-- A~D 일반(40), E 프리미엄(7), F 룸(3)
-- =========================================
-- 일반좌석
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 2, r, c, 'NORMAL', NOW(), NOW()
FROM (SELECT 'A' AS r UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D') rows
CROSS JOIN (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 프리미엄좌석 (E 1~7)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 2, 'E', c, 'PREMIUM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 룸좌석 (F 1~3)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 2, 'F', c, 'ROOM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =========================================
-- 3관 좌석 (총 30석)
-- A~B 일반(20), C 프리미엄(5), D 룸(5)
-- =========================================
-- 일반좌석 (A~B 1~10)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 3, r, c, 'NORMAL', NOW(), NOW()
FROM (SELECT 'A' AS r UNION ALL SELECT 'B') rows
CROSS JOIN (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 프리미엄좌석 (C 1~5)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 3, 'C', c, 'PREMIUM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 룸좌석 (D 1~5)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 3, 'D', c, 'ROOM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =========================================
-- 4관 좌석 (총 20석)
-- A 일반(10), B 프리미엄(5), D 룸(5)
-- =========================================
-- 일반좌석 (A 1~10)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 4, 'A', c, 'NORMAL', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 프리미엄좌석 (B 1~5)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 4, 'B', c, 'PREMIUM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 룸좌석 (D 1~5)
INSERT INTO seats (theater_id, row_label, col_number, type, created_at, updated_at)
SELECT 4, 'D', c, 'ROOM', NOW(), NOW()
FROM (
    SELECT 1 AS c UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) cols
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ========================================
-- 장르(genre) 기본 데이터
-- ========================================
INSERT INTO genres (id, name)
VALUES
    (1, 'ACTION'),
    (2, 'DRAMA'),
    (3, 'COMEDY'),
    (4, 'ROMANCE'),
    (5, 'THRILLER'),
    (6, 'HORROR'),
    (7, 'FANTASY'),
    (8, 'SCIENCE_FICTION'),
    (9, 'DOCUMENTARY'),
    (10, 'ANIMATION'),
    (11, 'CRIME'),
    (12, 'ADVENTURE'),
    (13, 'FAMILY'),
    (14, 'WAR'),
    (15, 'MUSICAL'),
    (16, 'HISTORY'),
    (17, 'MYSTERY')
ON DUPLICATE KEY UPDATE name = VALUES(name);