#!/usr/bin/env bash
#
# 현재 git 커밋 SHA로 이미지를 빌드해 Docker Hub에 :{SHA} 와 :latest 두 태그로 push.
# 서버 아키텍처(ARM64)에 맞춰 빌드한다. 마지막 줄에 SHA를 출력한다(배포 스크립트에서 사용).
#
# 사용법:  deploy/build-push.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."

set -a; source ./.env; set +a
: "${DOCKER_USERNAME:?DOCKER_USERNAME이 .env에 없습니다}"

PLATFORM="${PLATFORM:-linux/arm64}"   # 운영 서버가 Arm(A1)이므로 arm64 고정
SHA="$(git rev-parse --short HEAD)"
IMG="${DOCKER_USERNAME}/comma-be"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "⚠️  커밋되지 않은 변경이 있습니다. 태그 $SHA 가 실제 배포 코드와 다를 수 있어요." >&2
fi

echo "▶ build $IMG:$SHA (+latest)  platform=$PLATFORM"
docker build --platform "$PLATFORM" -t "$IMG:$SHA" -t "$IMG:latest" .

echo "▶ push $IMG:$SHA"
docker push "$IMG:$SHA"
echo "▶ push $IMG:latest"
docker push "$IMG:latest"

echo "✅ pushed: $IMG:$SHA  (and :latest)"
echo "$SHA"
