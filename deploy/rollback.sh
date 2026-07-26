#!/usr/bin/env bash
#
# 운영 서버를 지정한 태그로 즉시 롤백한다. (해당 태그 이미지는 Docker Hub에 있어야 함)
#
# 사용법:  deploy/rollback.sh <tag>
#   예)   deploy/rollback.sh d61b7ec
#
# 사용 가능한 태그 목록은 Docker Hub 또는:  docker images 1030pmy/comma-be
#
set -euo pipefail

TAG="${1:?사용법: deploy/rollback.sh <tag>   (예: deploy/rollback.sh d61b7ec)}"
SSH_KEY="${SSH_KEY:-$HOME/Downloads/ssh-key-2026-07-11.key}"
SERVER="${SERVER:-ubuntu@167.126.10.48}"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$SERVER" "
  sed -i '/^APP_TAG=/d' ~/.env
  echo 'APP_TAG=$TAG' >> ~/.env
  docker compose pull comma-app && docker compose up -d
"
echo "↩️  롤백 요청 완료 (APP_TAG=$TAG). 기동 확인:"
echo "    ssh -i $SSH_KEY $SERVER 'docker logs --tail 20 comma-be'"
