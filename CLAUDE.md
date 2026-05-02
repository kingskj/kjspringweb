# CLAUDE.md — kjspringweb 프로젝트 컨텍스트

> 새 대화에서 이 파일을 읽으면 프로젝트 전체 컨텍스트가 복원된다.

---

## 0. 작업 규칙 (절대 준수)

- 파일 직접 수정 금지 — 명시적 명령 전까지
- 분석/수정안은 텍스트로만 제시
- 소스 수정은 GPT(VSCode)가 담당
- GPT가 수정안 제시 → Claude가 검토/딴지/개선안 제시 → 핑퐁 반복 → 합의점에서 GPT가 적용
- 사용자를 "오빠"라 부르고 편한 대화체 사용

---

## 1. 프로젝트 한 줄 요약

**터틀픽(TurtlePick) 비즉시성 모니터링 엔진의 테스트 대상 서버**
Spring Boot + Gradle + H2 기반 웹 애플리케이션. 에러 유발/관측 목적으로 의도적으로 불안정한 동작을 포함한다.

---

## 2. 프로젝트 성격

- 실서비스 목적 아님 — TurtlePick Agent/Engine 붙여볼 샌드박스
- 에러 유발 로직 의도적으로 포함 (짝수일 강제 예외 등)
- 소스단 벨리데이션 최소화 — 에러는 서버/DB 제약으로 유도
- 완성도보다 **TurtlePick 관측 시나리오** 우선

---

## 3. 기술 스택

- Java / Spring Boot
- Gradle
- H2 (인메모리 DB, 변경 없음)
- Spring Batch (스케줄 배치)
- Spring Security (역할 기반 권한)
- GCP 배포 / 2코어 1GB + 2GB 가상메모리

---

## 4. 폴더 구조 (GitHub)

```
kjspringweb/
├── src/
│   └── main/
│       ├── java/       ← 소스
│       └── resources/  ← 설정, 템플릿
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
└── CLAUDE.md           ← 이 파일
```

레포: https://github.com/kingskj/kjspringweb

---

## 5. 주요 기능 현황 (2026-02-28 기준)

| 기능 | 상태 |
|------|------|
| 게시판 CRUD | ✅ 완료 |
| 게시판 유형 (공지사항/인사/일반) | ✅ 완료 |
| 역할 기반 권한 (관리자/일반) | ✅ 완료 |
| 회원가입/로그인/로그아웃 | ✅ 완료 |
| 회원정보 수정 / 탈퇴 | ✅ 완료 |
| 메뉴관리 (추가/삭제/순서) | ✅ 완료 |
| REST 전환 (액티브 요청) | ✅ 완료 |
| 공통 에러 팝업 | ✅ 완료 |
| Spring Batch (인사/삭제) | ✅ 완료 |
| 트랜잭션 롤백 검증 | ✅ 완료 |

---

## 6. Spring Batch 스케줄 (현재)

| 배치 | 스케줄 | 동작 |
|------|--------|------|
| 인사 게시물 자동 생성 | 매일 19:20 | 인사 유형 게시물 insert |
| 인사 게시물 삭제 | 매일 19:22 | 인사 유형 게시물 삭제 후 짝수일 강제 예외 |

**짝수일 강제 에러 정책**: 삭제 배치 후 날짜(일)가 짝수면 강제 예외 발생 → TurtlePick 에러 관측용

---

## 7. 에러 처리 정책

- 소스단 벨리데이션 제거 — 빈값/이상값은 서버/DB 제약으로 처리
- 500 에러: 페이지 이동 대신 공통 팝업 노출 (현 화면 유지)
- REST 응답 기반 에러 처리

---

## 8. DB 제약 주요 항목

| 테이블 | 컬럼 | 제약 |
|--------|------|------|
| 게시판 | 제목 | NOT NULL, 길이 100 |
| 메뉴 | 메뉴명 | NOT NULL |
| 메뉴 | URL | NOT NULL + UNIQUE |
| 메뉴 | 순서 | NOT NULL, 0 입력 불가 |
| 메뉴 | 상태 | NOT NULL |

---

## 9. 권한 정책

| 역할 | 게시물 수정/삭제 | 게시판 유형 변경 |
|------|----------------|----------------|
| 관리자 | 모든 게시물 | 가능 |
| 일반 사용자 | 본인 게시물만 | 불가 (일반 고정) |

---

## 10. 보안 관련 (테스트 목적)

- 비밀번호: 단방향 암호화 + 원본 컬럼 별도 저장 (관리자 화면 확인용)
- 실서비스 보안 기준 적용 안 함 — TurtlePick 관측 목적 우선

---

## 11. 현재 상태 (2026-05-02)

- GCP 배포 상태로 동작 확인
- TurtlePick Agent 섹터 1~7 완료 (`turtlepick-agent-core` 모듈)
- TurtlePick Engine 로컬 기동 확인 (localhost:8081, health UP)
- kjspringweb HEAD: `6e32875811200f879762478aadc73a9850d89e98` (엔진에 인덱싱 완료)
- 실기동 E2E smoke 결과:
  - `GET /` → trace RESOLVED 확인 (`HomeController#home()`)
  - `GET /auth/login` → HTTP 200이지만 trace 누락 (FQCN 정규화 blocker)
  - `GreetingBatchConfig#...Job()` → NO_CANDIDATE (BATCH endpoint 미구현, 정상 범위)
- 로그 정비 완료 (`logback-spring.xml`, `.gitignore`, `application.yml`)
- `DemoApplicationTests.java` 잔재 파일 제거 필요 (`gradlew test` 실패 원인)

---

## 12. Server Agent 구현 현황 (2026-05-02)

### 모듈 방향 (확정)
- **javaagent JAR** 방식 (Spring Boot starter 아님) — 레거시 Spring 포함 호환
- `D:\workspace\kjspringweb\turtlepick-agent-core` 위치
- Java 8 호환, JDK-only, ASM 9.9.1 shadow/relocate
- 설정 파일: `turtlepick.properties` (`-Dturtlepick.config` 또는 `user.dir`)

### 섹터 구현 현황

| 섹터 | 내용 | 상태 |
|------|------|------|
| 1 | `AgentPremain` — premain 진입, 의존성 조립 | ✅ |
| 2 | `TurtlepickConfigLoader` — properties 로드 | ✅ |
| 3 | `GitCommitHashProvider` — full 40자 hex hash | ✅ |
| 4 | `EngineMetaClient` — `POST /api/agent/meta` | ✅ |
| 5 | `MethodMappingRegistry` — fqcnMethod→methodId 적재 | ✅ |
| 6 | ASM probe (`instrument` + `trace` 패키지) | ✅ |
| 7 | HTTP context + endpoint 귀속 + trace 파일 기록/롤링 | ✅ |
| 8 | `POST /api/agent/log-ready` 전송 | ❌ 미구현 |
| 9 | 엔진 수거 응답 처리 (파일 삭제/오프셋) | ❌ 미구현 |

### trace 파일
- 경로: `turtlepick-logs/trace-{yyyyMMddHHmm}.log`
- 포맷: 1줄 JSON (JDK-only, ndjson)
- 롤링: slot 기반 (`rollingIntervalMinutes`)

---

## 13. 다음 과제 (2026-05-02 기준)

| 순위 | 작업 | 위치 |
|------|------|------|
| 1 | 엔진 FQCN 파라미터 정규화 (`작업지시서_20260329.md`) | turtlepick/engine-core-private |
| 2 | 엔진 `POST /api/git/index` 추가 (commit on-demand 인덱싱) | turtlepick/engine-app |
| 3 | DB 재인덱싱 → `/auth/login` trace RESOLVED 검증 | - |
| 4 | Agent 8섹터: `POST /api/agent/log-ready` 전송 구현 | turtlepick-agent-core |
| 5 | Agent 9섹터: 엔진 수거 응답 처리 | turtlepick-agent-core |

---

## 14. TurtlePick Engine 연동 정보 (2026-03-28)

- 엔진 주소: `http://localhost:8081`
- meta 엔드포인트: `POST /api/agent/meta`
- log-ready 엔드포인트: `POST /api/agent/log-ready`
- resume 수신 엔드포인트 (대상서버): `POST /agent/resume`
- 대상서버 serverId: `kjspringweb-local`
- 현재 인덱싱된 최신 커밋: `586234a0a0ebbe8819d28805b32ee1c826c8e23f`

### 엔진 연동 정보 최신화 (2026-05-02)
- kjspringweb HEAD `6e32875...` 기준 meta → `status=OK`, `methods=88`, `endpoints=28` 정상
- short hash는 여전히 `LOG_OFF / COMMIT_NOT_INDEXED` (정책 변경 없음)
- **full hash 전달 필수** (agent `GitCommitHashProvider`가 full 40자로 처리 중)

### ⚠️ 설계 갭 — branch-agnostic 인덱싱 미구현
- 현재 엔진은 `monitoring-branches` 폴링으로만 commit 발견
- 엔진 runtime repo가 fetch 가능한 remote ref에 commit object가 없으면 → 폴링 실패 → LOG_OFF
- **해결책**: 엔진에 `POST /api/git/index` 추가 (hash가 remote repo에 존재하면 즉시 발번/인덱싱)
- Agent 8섹터에서 `LOG_OFF(COMMIT_NOT_INDEXED)` 수신 시 이 API 자동 트리거 포함 **검토 중**

---

## 15. 관련 프로젝트

| 프로젝트 | 역할 |
|----------|------|
| kjspringweb (이 레포) | TurtlePick 대상 서버 |
| TurtlePick | 비즉시성 모니터링 엔진 (로컬 기동 중) |
| kjmacro2 | Diablo2 Vision RPA 엔진 (별개 프로젝트) |

---

## 16. 작업 규칙 (2026-03-28)

- CLAUDE.md 수정: Claude 자율 허용
- 나머지 파일 수정: 금지 (오빠 명시적 지시 시에만)
- Claude 역할: GPT 제안 딴지/보완안 채팅 제시만
- 코드 수정 실행: GPT(Codex) 담당

## 17. 작업 프로토콜

`work_protocol.md` 참고 (GPT + Claude 핑퐁 방식 동일 적용)