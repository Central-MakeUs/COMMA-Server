---
name: feedback-entity-files
description: User prefers to write entity classes themselves; only create package structure (directories)
metadata:
  type: feedback
---

엔티티 클래스 파일은 직접 생성하지 않는다. 도메인 구조를 잡을 때는 패키지 디렉토리만 만들고, 엔티티는 사용자가 직접 작성한다.

**Why:** "엔티티 파일은 생성하지 말고 패키지만 만들어" — 엔티티는 직접 작성하겠다는 의사 표현.

**How to apply:** 새 도메인 스캐폴딩 시 entity/, controller/, service/, repository/, dto/ 디렉토리는 생성하되, entity 패키지 안에 .java 파일은 만들지 않는다. 명시적으로 요청받은 경우에만 엔티티 파일 생성.