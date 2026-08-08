-- 홈 화면 휴식 유도 배너 개인화용 체크포인트: 유저가 마지막으로 휴식을 완료(피드 작성)한 시각.
ALTER TABLE `users` ADD COLUMN `last_rested_at` datetime(6) DEFAULT NULL;
