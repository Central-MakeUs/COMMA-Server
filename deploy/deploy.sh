#!/usr/bin/env bash
#
# 지정한 태그(기본: 현재 커밋 SHA)를 운영 서버에 배포한다.
# 서버 .env의 APP_TAG를 바꾸고 재기동 → 앱 기동 확인(헬스체크) → 실패 시 이전 태그로 자동 롤백.
#
# 사용법:  deploy/deploy.sh [tag]
#   예)   deploy/deploy.sh            # 현재 SHA 배포
#         deploy/deploy.sh d61b7ec    # 특정 태그 배포
#
# 환경변수(기본값 있음): SSH_KEY, SERVER
#
set -euo pipefail
cd "$(dirname "$0")/.."

TAG="${1:-$(git rev-parse --short HEAD)}"
SSH_KEY="${SSH_KEY:-$HOME/Downloads/ssh-key-2026-07-11.key}"
SERVER="${SERVER:-ubuntu@167.126.10.48}"

ssh_run() { ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$SERVER" "$@"; }

# 현재(이전) 태그 백업 — 롤백 대상
PREV="$(ssh_run 'grep -E "^APP_TAG=" ~/.env | cut -d= -f2' || true)"
PREV="${PREV:-latest}"
echo "▶ 배포: $PREV  →  $TAG"

apply_tag() {
  local tag="$1"
  ssh_run "
    sed -i '/^APP_TAG=/d' ~/.env
    echo 'APP_TAG=$tag' >> ~/.env
    docker compose pull comma-app >/dev/null 2>&1
    docker compose up -d
  "
}

apply_tag "$TAG"

echo "▶ 헬스체크 (앱 기동 확인, 최대 ~75s)"
if ssh_run '
  for i in $(seq 1 25); do
    if docker logs comma-be 2>&1 | grep -q "Started CommaApplication"; then exit 0; fi
    if docker logs comma-be 2>&1 | grep -qiE "APPLICATION FAILED TO START"; then exit 1; fi
    sleep 3
  done
  exit 1
'; then
  echo "✅ 배포 성공 (APP_TAG=$TAG)"
else
  echo "❌ 기동 실패 → 이전 태그($PREV)로 자동 롤백"
  apply_tag "$PREV"
  echo "↩️  롤백 완료 (APP_TAG=$PREV)"
  exit 1
fi
