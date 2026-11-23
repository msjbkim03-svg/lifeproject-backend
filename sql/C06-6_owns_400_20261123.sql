-- C06-6 가진 물건 칸 늘리기 (2026-11-23, 유동현)
-- LIFE.OWNS 가 100바이트라, 물건·반려동물 이름·취미·병역·유언장이 쌓이면 넘쳐서 다음 해 진행이 ORA-12899 로 멈출 수 있음
-- 한글은 한 글자 3바이트 (반려동물 이름 12자 = 36바이트) → 400바이트로 늘림 (있는 값은 그대로)
ALTER TABLE LIFE MODIFY OWNS VARCHAR2(400);

COMMENT ON COLUMN LIFE.OWNS IS '가진 물건·상태 코드 쉼표 목록 (C06-3, PET-/HOB-/SVC-/WILL- 포함)';

COMMIT;
