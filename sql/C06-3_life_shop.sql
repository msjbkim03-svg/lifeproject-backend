-- C06-3 돈 쓰기·투자 (2026-11-18, 유동현)
-- LIFE 테이블에 컬럼 2개 추가. 서버 재시작 전에 먼저 실행해야 해요 (안 하면 인생 불러오기가 ORA-00904로 실패).
--   INVEST_TYPE: 여유 자금 굴리기 방식 (CASH 집에 둔 현금, DEPOSIT 예금, BOND 채권, MIX 반반 펀드,
--                STOCK 주식 지수, WORLD 해외 지수, PROPERTY 부동산 펀드, TRADE 종목 사고팔기 / NULL = 알아서 예금·현금)
--   OWNS:        가진 물건 코드 쉼표 목록 (PHONE, CELL, BIKE, MOTO, CAR, TV, PC, HOUSE)
ALTER TABLE LIFE ADD (
    INVEST_TYPE VARCHAR2(10),
    OWNS        VARCHAR2(100)
);

COMMENT ON COLUMN LIFE.INVEST_TYPE IS '여유 자금 굴리기 방식 (C06-3)';
COMMENT ON COLUMN LIFE.OWNS IS '가진 물건 코드 쉼표 목록 (C06-3)';

COMMIT;
