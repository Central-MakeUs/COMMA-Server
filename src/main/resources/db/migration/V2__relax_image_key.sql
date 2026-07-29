-- Relax 이미지도 Feed와 동일하게 "URL이 아니라 객체 key만 저장" 방식으로 통일.
-- image_url 컬럼은 지금까지 어떤 코드 경로에서도 채운 적이 없어 운영 DB에서 항상 NULL이므로 안전.
-- RENAME COLUMN은 MySQL 8에서 메타데이터만 변경(ALGORITHM=INSTANT)되어 타입/collation/nullable이 그대로 유지된다.
ALTER TABLE `relaxes` RENAME COLUMN `image_url` TO `image_key`;
