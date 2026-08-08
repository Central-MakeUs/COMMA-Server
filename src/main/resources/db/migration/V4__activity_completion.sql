-- 휴식 완료 처리: activities.completed_at이 NULL이 아닐 때만 "완료된 기록"으로 취급한다.
-- (마이 리포트의 활동 순위/무드 비율/시간 비율은 이 컬럼을 기준으로 집계한다)
ALTER TABLE `activities` ADD COLUMN `completed_at` datetime(6) DEFAULT NULL;

-- 피드 작성이 곧 휴식 완료 처리다. 한 활동은 피드 하나로만 완료될 수 있으므로 유니크 제약을 둔다.
ALTER TABLE `feeds` ADD COLUMN `activity_id` bigint DEFAULT NULL;
ALTER TABLE `feeds` ADD CONSTRAINT `uk_feed_activity` UNIQUE (`activity_id`);
