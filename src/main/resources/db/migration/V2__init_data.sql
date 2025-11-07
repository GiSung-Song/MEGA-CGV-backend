-- ========================================
-- 영화관의 관(theater) 기본 데이터
-- ========================================
INSERT INTO theaters (name, total_seat, type, base_price)
VALUES
    (),
    (),
    (),
    ()
ON DUPLICATE KEY UPDATE updated_at = NOW();