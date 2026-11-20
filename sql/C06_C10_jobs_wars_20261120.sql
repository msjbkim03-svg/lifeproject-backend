-- =====================================================================
-- 환승인생 11/20  C06 직업 추가: 직업군인 · 장교 (JOB 데이터 2행, 테이블 구조 변경 없음)
--   엔진: 전쟁 중이면 사망 위험 ×2, "군인으로서 전선에 배치되었다." 기록 (EngineService.soldier)
--   실행 순서: 0) 컬럼 확인 → 1) INSERT → 2) 확인 → COMMIT
-- =====================================================================

-- 0) JOB 컬럼 확인: 아래 INSERT 에 없는 NOT NULL 컬럼이 있으면 실행 전에 확인
SELECT COLUMN_NAME, NULLABLE, DATA_DEFAULT
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'JOB'
 ORDER BY COLUMN_ID;

-- 1) 직업군인(부사관) · 장교
INSERT INTO JOB (JOB_ID, JOB_NAME, REQUIRED_EDU, INDUSTRY, INCOME_RATE)
VALUES ('JOB_SOLDIER', '직업군인(부사관)', 'HIGH', 'SERVICE', 1.1);

INSERT INTO JOB (JOB_ID, JOB_NAME, REQUIRED_EDU, INDUSTRY, INCOME_RATE)
VALUES ('JOB_OFFICER', '장교', 'UNI', 'SERVICE', 1.5);

-- 2) 확인 (2행)
SELECT * FROM JOB WHERE JOB_ID IN ('JOB_SOLDIER', 'JOB_OFFICER');

-- COMMIT;   ← 위 결과가 맞으면 실행
-- 직업 목록은 서버가 처음 읽을 때 메모리에 올려 두므로, COMMIT 뒤 서버 재시작
