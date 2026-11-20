-- =====================================================================
-- 환승인생 11/20  출신 가정 · 집안 형편 변동 · 내 집 시세 (LIFE 컬럼 2개 추가)
--   서버 재시작 전에 먼저 실행해야 해요 (안 하면 인생 불러오기가 ORA-00904 로 실패)
--   ORIGIN_CLASS: 태어날 때 가정형편 1~5 (WEALTH_CLASS 는 부모님 형편이 바뀌면 따라 바뀜)
--   HOUSE_VALUE : 내 집 시세 (구매력 달러, 해마다 집값 흐름만큼 오르내림 / 집이 없으면 NULL)
-- =====================================================================
ALTER TABLE LIFE ADD (
    ORIGIN_CLASS NUMBER(1),
    HOUSE_VALUE  NUMBER(15)
);

COMMENT ON COLUMN LIFE.ORIGIN_CLASS IS '태어날 때 가정형편 1~5 (출신 가정)';
COMMENT ON COLUMN LIFE.HOUSE_VALUE IS '내 집 시세 (구매력 달러)';

-- 이미 있는 인생은 지금 형편을 출신 가정으로
UPDATE LIFE SET ORIGIN_CLASS = WEALTH_CLASS WHERE ORIGIN_CLASS IS NULL;

COMMIT;
