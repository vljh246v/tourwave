-- T-005: users 테이블에 소프트 딜리트 타임스탬프 컬럼 추가
-- deleted_at_utc 는 DELETED 상태 전이 시각을 기록 (nullable, 기존 데이터 무중단)
ALTER TABLE users
    ADD COLUMN deleted_at_utc DATETIME(6) NULL;
