# 모니터링 (Prometheus + Grafana + node-exporter + Alertmanager)

앱 지표(`/actuator/prometheus`, RED)와 호스트 지표(node-exporter, USE)를 Prometheus가
내부 네트워크로 pull → Grafana 시각화 → 규칙 위반 시 Alertmanager가 Discord로 알림.

UI(Grafana/Prometheus/Alertmanager)는 **외부에 노출하지 않는다**(`127.0.0.1` 바인딩).
접근은 SSH 터널로.

## 접근 (SSH 터널)

```bash
ssh -i ~/Downloads/ssh-key-2026-07-11.key -N \
  -L 3000:localhost:3000 \
  -L 9090:localhost:9090 \
  -L 9093:localhost:9093 \
  ubuntu@167.126.10.48
```
- Grafana: http://localhost:3000  (admin / `GRAFANA_ADMIN_PASSWORD`)
- Prometheus: http://localhost:9090  (Status → Targets 로 스크레이핑 확인)
- Alertmanager: http://localhost:9093

## 서버에 필요한 값

`~/.env` (scp되는 파일):
- `GRAFANA_ADMIN_PASSWORD` — Grafana admin 비밀번호

`~/monitoring/alertmanager/discord_url` (git 아님, 서버에만 생성):
- Discord 채널 → 채널 설정 → 연동 → 웹후크 생성 → URL을 이 파일에 한 줄로 저장
  ```bash
  echo 'https://discord.com/api/webhooks/xxx/yyy' > ~/monitoring/alertmanager/discord_url
  ```

## Grafana 대시보드 (UI에서 Import → ID 입력)
- **JVM (Micrometer)**: `4701`
- **Spring Boot Statistics**: `6756` 또는 `11378`
- **Node Exporter Full**: `1860`

## 배포 (설정 파일은 이미지가 아니라 scp)

```bash
KEY=~/Downloads/ssh-key-2026-07-11.key; H=ubuntu@167.126.10.48
scp -i $KEY docker-compose.yaml nginx/default.conf $H:~/
scp -i $KEY -r monitoring $H:~/
ssh -i $KEY $H 'docker compose up -d && docker exec comma-nginx nginx -s reload'
```

## 알림 규칙
`monitoring/prometheus/alerts.yml` — RED(다운/5xx율/p95지연) + USE(디스크/메모리). 증상 기반.
