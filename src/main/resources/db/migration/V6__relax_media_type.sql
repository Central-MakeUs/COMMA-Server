-- Relax 카드 미디어를 사진뿐 아니라 영상으로도 낼 수 있게. image_key 컬럼은 그대로 재사용하고
-- (VIDEO면 영상 객체 key), 이 컬럼으로 프론트가 <img>/<video> 중 뭘 그릴지 결정한다.
-- 기존 45개는 전부 사진이므로 기본값 IMAGE로 채워도 안전.
ALTER TABLE `relaxes` ADD COLUMN `media_type` varchar(20) NOT NULL DEFAULT 'IMAGE';
