# 배포 (SHA 태그 + 롤백)

이미지를 항상 `:{git SHA}` 와 `:latest` 두 태그로 push 하고, 서버는 `APP_TAG`로
어떤 버전을 띄울지 지정한다. 문제가 생기면 이전 SHA로 즉시 롤백할 수 있다.

## 배포 흐름

```bash
# 1) main 최신 상태에서
git checkout main && git pull

# 2) 이미지 빌드 + push (:SHA, :latest)
deploy/build-push.sh

# 3) 서버에 배포 (기본: 현재 SHA). 기동 실패 시 이전 태그로 자동 롤백
deploy/deploy.sh
#   특정 태그 배포:  deploy/deploy.sh <tag>
```

## 롤백

```bash
# 이전에 배포했던 SHA로 되돌리기 (그 이미지가 Docker Hub에 있어야 함)
deploy/rollback.sh <tag>      # 예: deploy/rollback.sh d61b7ec
```

배포 가능한 태그 확인: Docker Hub의 `1030pmy/comma-be` 태그 목록, 또는 로컬 `docker images 1030pmy/comma-be`.

## 동작 원리

- `docker-compose.yaml`: `image: ${DOCKER_USERNAME}/comma-be:${APP_TAG:-latest}`
  → 서버 `~/.env`의 `APP_TAG` 값으로 실행할 이미지 버전을 고른다(없으면 `latest`).
- `deploy.sh`가 `~/.env`의 `APP_TAG`를 새 태그로 바꾸고 `docker compose up -d`.
- 헬스체크는 서버에서 `comma-be` 로그의 `Started CommaApplication` 등장 여부로 판단.

## 환경변수(선택)

- `SSH_KEY` (기본 `~/Downloads/ssh-key-2026-07-11.key`)
- `SERVER` (기본 `ubuntu@167.126.10.48`)
- `PLATFORM` (기본 `linux/arm64` — 운영 서버가 Arm A1)
