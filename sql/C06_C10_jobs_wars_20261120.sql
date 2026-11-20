-- =====================================================================
-- 환승인생 11/20  직업군인 · 빠진 전쟁·역사 사건 추가 (데이터 행만, 테이블 구조 변경 없음)
--   실행 순서: 0) 컬럼 확인 → 1) JOB → 2) HISTORY_EVENT → 3) GAME_EVENT → COMMIT
--   역사 사건 데이터(HISTORY_EVENT·GAME_EVENT)는 장윤아 담당 데이터라 윤아 확인 후 실행
-- =====================================================================

-- 0) 먼저 컬럼 확인: 아래 INSERT 에 없는 NOT NULL 컬럼이 있으면 실행 전에 알려 주세요
SELECT TABLE_NAME, COLUMN_NAME, NULLABLE, DATA_DEFAULT
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME IN ('JOB', 'HISTORY_EVENT', 'GAME_EVENT')
 ORDER BY TABLE_NAME, COLUMN_ID;

-- ---------------------------------------------------------------------
-- 1) 직업군인 · 장교 (C06 직업)
--    엔진: 전쟁 중이면 사망 위험 ×2, "군인으로서 전선에 배치되었다." 기록
-- ---------------------------------------------------------------------
INSERT INTO JOB (JOB_ID, JOB_NAME, REQUIRED_EDU, INDUSTRY, INCOME_RATE)
VALUES ('JOB_SOLDIER', '직업군인(부사관)', 'HIGH', 'SERVICE', 1.1);

INSERT INTO JOB (JOB_ID, JOB_NAME, REQUIRED_EDU, INDUSTRY, INCOME_RATE)
VALUES ('JOB_OFFICER', '장교', 'UNI', 'SERVICE', 1.5);

-- ---------------------------------------------------------------------
-- 2) HISTORY_EVENT: 게임에 없던 전쟁·사건 (인도 · 중국 · 베트남 · 대만)
--    11/20 서버 조회로 확인: 인도 전쟁 0개, 중국 한국전쟁 참전·중월전쟁 없음, 베트남 중월전쟁 없음, 대만 사건 0개
-- ---------------------------------------------------------------------
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('IN_1962_SINOWAR', 'IND', '중인전쟁', 1962, 1962, '히말라야 국경에서 중국과 벌인 전쟁. 인도군이 크게 패했다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('IN_1965_WAR', 'IND', '제2차 인도·파키스탄 전쟁', 1965, 1965, '카슈미르를 두고 파키스탄과 벌인 전쟁.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('IN_1971_WAR', 'IND', '제3차 인도·파키스탄 전쟁', 1971, 1971, '방글라데시 독립 전쟁에 개입해 파키스탄과 싸웠다. 난민 약 1천만 명이 인도로 넘어왔다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('IN_1999_KARGIL', 'IND', '카르길 전쟁', 1999, 1999, '카슈미르 카르길 고지에서 파키스탄과 벌인 전투.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('CN_1950_KOREA', 'CHN', '한국전쟁 참전(항미원조)', 1950, 1953, '중국 인민지원군이 한국전쟁에 참전했다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('CN_1979_VIETNAM', 'CHN', '중월전쟁', 1979, 1979, '중국이 베트남 북부 국경을 공격한 전쟁.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('VN_1978_CAMBODIA', 'VNM', '캄보디아 침공과 중월전쟁', 1978, 1989, '크메르 루주를 몰아내려 캄보디아에 들어갔고, 1979년 중국의 국경 공격을 받았다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('TW_1947_228', 'TWN', '2·28 사건', 1947, 1947, '정부의 단속에 항의한 시민들이 진압되어 많은 사람이 희생됐다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('TW_1949_MARTIAL', 'TWN', '계엄령과 백색테러', 1949, 1987, '38년 동안 계엄령이 이어지며 정치적 탄압이 계속됐다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('TW_1958_KINMEN', 'TWN', '진먼 포격전', 1958, 1958, '중국이 진먼섬에 포격을 퍼부었다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('TW_1999_QUAKE', 'TWN', '9·21 대지진', 1999, 1999, '대만 중부에서 규모 7.6 지진이 일어나 2천여 명이 숨졌다.');
INSERT INTO HISTORY_EVENT (EVENT_ID, COUNTRY_CODE, TITLE, START_YEAR, END_YEAR, DESCRIPTION)
VALUES ('TW_2020_COVID', 'TWN', '코로나19 유행', 2020, 2022, '코로나19가 세계적으로 유행했다.');

-- ---------------------------------------------------------------------
-- 3) GAME_EVENT: 게임에서 실제로 일어나게 연결 (기존 행과 같은 형식, EVENT_TYPE = 'HIST')
--    PROBABILITY: 나라 전체가 휘말린 전쟁은 1, 국경 지역 전쟁은 낮게 (그 해에 겪을 확률)
--    INTENSITY: 1~3 (효과 = HIST_EVENT_TYPE 유형값 × 강도, 전쟁 사망 +1% × 강도)
-- ---------------------------------------------------------------------
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('IN_1962_SINOWAR', 'HIST', 'IN_1962_SINOWAR', '중인전쟁',
        '{연도}년, 히말라야 국경에서 중국과 전쟁이 벌어졌다. 라디오에서 패전 소식이 이어졌다.',
        0.15, 'N', 'Y', 'Y', 'N', 1, 'WAR', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('IN_1965_WAR', 'HIST', 'IN_1965_WAR', '제2차 인도·파키스탄 전쟁',
        '{연도}년, 카슈미르를 두고 파키스탄과 전쟁이 벌어졌다. 밤마다 등화관제가 이어졌다.',
        0.3, 'N', 'Y', 'Y', 'N', 1, 'WAR', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('IN_1971_WAR', 'HIST', 'IN_1971_WAR', '제3차 인도·파키스탄 전쟁',
        '{연도}년, 동쪽 국경 너머에서 피란민이 밀려들었고 곧 파키스탄과 전쟁이 시작됐다.',
        0.4, 'N', 'Y', 'Y', 'N', 1, 'WAR', 2);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('IN_1999_KARGIL', 'HIST', 'IN_1999_KARGIL', '카르길 전쟁',
        '{연도}년, 카르길 고지에서 전투가 벌어졌다는 소식이 매일 신문 1면을 채웠다.',
        0.1, 'N', 'Y', 'Y', 'N', 1, 'WAR', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('CN_1950_KOREA', 'HIST', 'CN_1950_KOREA', '한국전쟁 참전(항미원조)',
        '{연도}년, 압록강 너머 전쟁에 젊은이들이 지원군으로 떠났다. 마을마다 전사 통지가 날아들었다.',
        0.5, 'N', 'Y', 'Y', 'N', 1, 'WAR', 2);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('CN_1979_VIETNAM', 'HIST', 'CN_1979_VIETNAM', '중월전쟁',
        '{연도}년, 남쪽 국경에서 베트남과 전쟁이 벌어졌다.',
        0.15, 'N', 'Y', 'Y', 'N', 1, 'WAR', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('VN_1978_CAMBODIA', 'HIST', 'VN_1978_CAMBODIA', '캄보디아 침공과 중월전쟁',
        '{연도}년, 캄보디아로 군대가 들어갔고 이듬해 북쪽 국경에서는 중국과 전쟁이 벌어졌다.',
        0.6, 'N', 'Y', 'Y', 'N', 1, 'WAR', 2);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('TW_1947_228', 'HIST', 'TW_1947_228', '2·28 사건',
        '{연도}년, 2·28 사건의 진압으로 거리에 공포가 내려앉았다.',
        0.3, 'N', 'Y', 'Y', 'N', 1, 'WAR', 2);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('TW_1949_MARTIAL', 'HIST', 'TW_1949_MARTIAL', '계엄령과 백색테러',
        '{연도}년, 계엄령 아래에서 이웃이 끌려갔다는 소문이 돌았다. 말을 조심하며 지냈다.',
        0.05, 'N', 'Y', 'Y', 'N', 1, 'POLITICS', 2);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('TW_1958_KINMEN', 'HIST', 'TW_1958_KINMEN', '진먼 포격전',
        '{연도}년, 진먼섬에 포탄이 쏟아진다는 소식에 온 섬이 긴장했다.',
        0.2, 'N', 'Y', 'Y', 'N', 1, 'WAR', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('TW_1999_QUAKE', 'HIST', 'TW_1999_QUAKE', '9·21 대지진',
        '{연도}년, 새벽에 큰 지진이 섬을 흔들었다. 무너진 건물 소식이 이어졌다.',
        0.3, 'N', 'Y', 'Y', 'N', 1, 'DISASTER', 1);
INSERT INTO GAME_EVENT (EVENT_ID, EVENT_TYPE, HIST_EVENT_ID, TITLE, BASE_TEXT, PROBABILITY, IS_CHOICE, IS_ONCE,
                        AI_NARRATE, IS_REVIEWED, EFFECT_SCALE, HIST_TYPE_CODE, INTENSITY)
VALUES ('TW_2020_COVID', 'HIST', 'TW_2020_COVID', '코로나19 유행',
        '{연도}년, 코로나19가 퍼지며 마스크를 쓰고 사람을 피해 다녔다.',
        1, 'N', 'Y', 'Y', 'N', 1, 'DISASTER', 1);

-- 4) 확인 (14행: JOB 2 + 아래 12)
SELECT G.EVENT_ID, H.COUNTRY_CODE, G.HIST_TYPE_CODE, G.INTENSITY, G.PROBABILITY
  FROM GAME_EVENT G JOIN HISTORY_EVENT H ON H.EVENT_ID = G.HIST_EVENT_ID
 WHERE G.EVENT_ID IN ('IN_1962_SINOWAR','IN_1965_WAR','IN_1971_WAR','IN_1999_KARGIL','CN_1950_KOREA','CN_1979_VIETNAM',
                      'VN_1978_CAMBODIA','TW_1947_228','TW_1949_MARTIAL','TW_1958_KINMEN','TW_1999_QUAKE','TW_2020_COVID');
SELECT * FROM JOB WHERE JOB_ID IN ('JOB_SOLDIER', 'JOB_OFFICER');

-- COMMIT;   ← 위 결과가 맞으면 실행
