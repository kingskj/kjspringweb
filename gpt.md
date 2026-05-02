# GPT Context (kjspringweb)

이 파일은 이 저장소 대화의 **단일 맥락 기준 파일**이다.
새 세션은 이 파일을 먼저 읽고 시작한다.

## 1) 프로젝트 성격
- 프로젝트: `kjspringweb`
- 목적: 서비스 안정성보다 **의도적 에러 유도/관측/검증** 중심 테스트 시스템
- 운영 전제: GCP 배포 환경 + 로컬 동시 운용
- 리소스 가정: 2코어 / 1GB 메모리 / 2GB 스왑
- DB: H2 유지(교체 계획 없음)

## 2) 작업 사상 (중요)
- 화면(클라이언트) 검증보다 서버/DB 제약으로 실패를 유도한다.
- 에러를 숨기지 않고 관측 가능하게 만든다(팝업/로그/실패 상태).
- 기본 데이터 시드는 "없을 때만" 수행하여 실제 DB처럼 유지한다.
- 테스트 시나리오를 위해 일반 서비스 관점의 보안/UX 최적화를 일부 의도적으로 완화한다.

## 3) 이 대화방에서 확정된 핵심 변경 요약
1. 벨리데이션/에러 처리
- 소스단/화면 필수값 검증 다수 제거
- 500 페이지 중심 처리 대신, REST 전환 흐름에서 팝업/현 화면 유지 중심으로 조정

2. REST 전환
- 액티브 요청을 REST 중심으로 전환
- 화면에서는 기존 UX처럼 보이도록 리다이렉트/후처리

3. 데이터/스키마
- 게시판 제목 `NOT NULL`, 길이 100 반영
- 메뉴 테이블 제약 강화: 메뉴명/URL/순서/상태 `NOT NULL`, URL `UNIQUE`
- 기본 데이터는 재생성 남발하지 않도록 정리

4. 회원/권한
- 회원관리 항목 확장(비밀번호 표시 등)
- 회원정보 수정/탈퇴 기능 추가
- 테스트 목적상 원본 비밀번호 컬럼 저장 및 관리자 노출 반영

5. 게시판 정책
- 게시판 유형 컬럼 도입: `공지사항 / 인사 / 일반`
- 관리자: 전체 수정/삭제 가능
- 일반 사용자: 본인 글만 수정/삭제 가능, 유형은 보이되 수정 불가(일반 고정)

6. 배치
- Spring Batch 구성 완료
- 인사 배치: Tasklet -> Service insert 방식
- 삭제 배치: Step 방식
- 짝수일 강제 에러 로직 포함

7. 스케줄 (현재값)
- 인사 배치 2개: 매일 19:20
- 삭제 배치: 매일 19:22

8. 트랜잭션 결론
- Step 간 트랜잭션은 분리됨(앞 step 커밋 후 다음 step 실패 시 롤백 안 됨)
- 현재는 삭제 + 강제에러를 동일 step/동일 트랜잭션에 묶어, 짝수일 실패 시 삭제 롤백되도록 수정됨
- 2026-02-28 검증에서 롤백 동작 확인 완료

9. TurtlePick Agent 연동(SQL 계측) 결정
- Agent MVP1은 **JPA 기반 SQL 계측**으로 진행한다.
- MyBatis Interceptor는 MVP1 범위에서 제외하고, `kjspringweb`의 MyBatis 전환 시점에 별도 적용한다.
- 단, Agent 설계는 JPA/MyBatis 모두 대응 가능해야 하므로 SQL 계측 레이어를 공통/옵션으로 분리한다.
  - 공통: `datasource-proxy`로 JDBC 레벨 SQL 원문/실행시간/에러 캡처 (JPA/MyBatis 공통)
  - 옵션: MyBatis 환경에서만 Interceptor 활성화하여 `statementId` 등 보강
- 방향성 설정값(초안):
  - `instrumentation.sql.datasource_proxy: true`
  - `instrumentation.sql.mybatis_interceptor: false` (MyBatis 환경에서만 `true`)

## 4) 참고 문서
- 상세 일지: `docs/h202602281926.md`

## 5) 유지 규칙
1. 작업 중 정책/구조/운영값이 바뀌면 이 파일을 우선 갱신한다.
2. 일지는 이력 기록용이고, 현재 맥락의 최종 기준은 항상 `gpt.md`다.
3. 사용자 최신 지시가 충돌하면 최신 지시를 우선한다.
4. 최신 사용자 지시(2026-03-29): `gpt.md`를 제외한 모든 파일은 사용자의 직접적 명시 지시 전까지 수정 금지다.
5. 최신 사용자 지시(2026-03-29): `gpt.md`는 사용자의 별도 요청이 없어도 맥락 유지 목적의 자율 수정이 허용된다.
6. 최신 사용자 지시(2026-03-29): 딴지/제안/코드 수정안은 `확정. 반영.` 지시 전까지 파일 직접 수정이 아니라 채팅으로만 제시한다.
7. 최신 사용자 지시(2026-03-29): 실제 파일 수정/반영 허용 트리거는 사용자의 명시 문구 `확정. 반영.`일 때만 성립한다.
8. 최신 사용자 지시(2026-03-29): `확정. 반영.` 후에는 반영 요약, 반영된 파일 목록, 테스트 후 테스트 결과 요약을 항상 출력한다.

## 6) 2026-03-02 코드베이스 분석 메모
- 현재 구조: Spring Boot 4 + Thymeleaf + Security + JPA(H2) + Spring Batch 조합.
- 예외 처리: MVC(`GlobalExceptionHandler`)는 referer 리다이렉트 + flash/popup, REST(`ApiExceptionHandler`)는 JSON 응답.
- 배치 스케줄: 인사 2건 `19:20`, 삭제 `19:22`, 삭제 step 내부 짝수일 강제 예외 유지.
- 확인된 구현 차이: 관리자의 "타인 게시글 수정" 시 게시판 유형 변경은 현재 비활성(`isAdmin && isOwner` 조건).
- 테스트 코드 상태: `src/test/java/com/example/demo/DemoApplicationTests.java` 기본 템플릿 잔존(패키지 경로가 실앱 패키지와 불일치).
- 최신 합의: Agent SQL 계측은 공통 `datasource-proxy` + MyBatis 전환 시 선택적 Interceptor 보강 전략으로 확정.

## 7) 2026-03-28 외부 참조 문서 기반 분석 메모
- 참조 문서: `D:\workspace\turtlepick\README.md`, `gpt.md`, `CLAUDE.md`, `docs/h202603071800.md`, `docs/conversation_archive_*`
- `turtlepick` 문서군은 현재 저장소(`kjspringweb`)를 Agent/Engine 계측 대상 서버로 전제하고 있으며, 방향성은 "JPA 환경 우선 + JDBC 레벨 SQL 계측"이다.
- 실제 코드 기준 현재 구조는 여전히 Spring Boot 4 + Thymeleaf + Security + JPA(H2) + Batch 조합으로, Turtlepick MVP1의 JPA 타깃 가정과 부합한다.
- 실제 진입점은 HTTP 25개 + `@Scheduled` 3개이며, `MemberController` 실제 기본 경로는 `/member`다(일부 외부 문서의 `/profile` 단독 표기는 최신 코드와 차이 있음).
- 예외 처리 구조상 MVC는 redirect/flash, REST는 JSON 응답으로 분기되므로, Agent의 에러 분류는 "최종 HTTP status"만이 아니라 발생 예외 자체도 함께 봐야 한다.
- `turtlepick` 실제 저장소 상태(2026-03-28 확인): 루트 멀티모듈은 `engine-app`, `engine-core-private` 2개만 존재하며, 문서에 다음 우선순위로 적힌 `server-agent` 모듈은 아직 생성되지 않았다.
- `engine-app`에는 이미 `/api/agent/meta`, `/api/agent/log-ready`, `/api/git/sync`, 엔진 기동 시 1회 `RESUME` 전송, SQLite 스키마 초기화, Git polling/manual sync가 구현되어 있다.
- AST 분석 파이프라인은 HTTP + `@Scheduled` + extra-entry 기반 `INTERFACE` 진입점을 수집하고, 서비스 호출 추적은 "단일 구현체 인터페이스는 계속 추적, 다중/미해결 인터페이스는 leaf 처리" 방식으로 구현되어 있다.
- 반면 `log-ready` 후처리는 아직 실제 파일 I/O가 아니라 `checkSourceFileExistsStub`, `read/store/delete*Stub` 단계로 남아 있어, 현재 엔진은 수신 계약과 DB 상태 전이 중심의 골격 단계다.

## 8) 2026-03-28 최신 작업 모드 메모
- 현재 턴부터 작업 모드는 "제안만"으로 고정한다.
- TurtlePick 대상 서버 agent 개발을 시작하되, 실제 코드/문서 수정은 사용자의 직접 지시가 있기 전까지 금지한다(`gpt.md` 제외).
- Codex는 Claude가 채팅으로 제시한 설계/수정안에 대해 근거 기반 딴지, 보완안, 최소 변경 방향만 텍스트로 제시한다.
- 단위 제안이 검토되어도 사용자가 해당 안에 대해 정확히 `확정. 반영`이라고 지시하기 전에는 코드 수정을 시작하지 않는다.

## 9) 2026-03-28 TurtlePick 엔진 런타임 테스트 메모
- 최신 엔진 로그 기준 `turtlepick` 엔진은 `8081` 포트에서 기동 중이며, `GET /api/health`, `GET /api/agent/health`는 모두 HTTP 200 `UP` 응답을 반환했다.
- `POST /api/agent/meta`는 **전체 commit hash**(`586234a0a0ebbe8819d28805b32ee1c826c8e23f`) 기준으로는 HTTP 200 `status=OK`와 method 목록을 정상 반환했다.
- 반면 **짧은 commit hash**(`586234a`)로 `POST /api/agent/meta` 호출 시 HTTP 200이지만 `status=LOG_OFF`, `reason=COMMIT_NOT_INDEXED`를 반환했다. 현재 엔진은 short hash 정규화/확장을 하지 않는 상태로 보인다.
- `POST /api/git/sync`는 HTTP 200 `status=SUCCESS`로 응답했지만, 응답의 `fromCommit/toCommit`가 `meta-incomplete-case`로 표시되어 현재 `commit_version` 데이터나 과거 테스트 흔적에 이상치가 남아 있을 가능성이 있다(미확정).
- 런타임 로그상 엔진 기동 시 `RESUME` 전송은 `http://localhost:8080/agent/resume` 대상으로 실패했다. 엔진 자체 장애는 아니지만 대상 서버 연동은 현재 정상 복구 상태가 아니다.
- Git polling은 동작하지만 설정된 `develop`, `work` 브랜치는 원격에 없어 주기적으로 `remote branch not found` 경고가 발생한다.

## 10) 2026-03-28 TurtlePick Agent 배포 전제 고정
- TurtlePick Agent/Engine이 탑재되는 대상 서버는 **모두 Git 기반 배포**를 전제로 한다.
- SVN, 수동 배포, `.git` 미존재 배포 형태는 고려 대상에서 제외한다.
- 따라서 Agent의 commit hash 확보는 우선순위상 `git rev-parse HEAD` 기준의 **full 40자 hash**를 불변 전제로 잡는다.

## 11) 2026-03-28 TurtlePick Agent 첫 구현 설계 메모
- 첫 구현 목표는 `ApplicationReadyEvent -> full commit hash 확보 -> /api/agent/meta -> AgentState 결정`까지로 한정한다.
- 설정 prefix는 `turtlepick.engine.*`, `turtlepick.agent.*` 구조로 먼저 고정한다.
- `repo-root`는 선택값으로 두고, 미설정 시 기본 working directory(`user.dir`)를 사용한다.
- `AgentState`는 초기 구현에서 `LOG_ON / LOG_OFF` 2상태만 사용하며, 기본값은 `LOG_OFF`로 둔다.
- bootstrap 성공(`meta status=OK`) 시에만 method registry를 메모리에 적재하고 `LOG_ON` 전환, 실패 시 registry 비우고 `LOG_OFF` 유지.
- `AgentStateHolder`는 멀티스레드 요청 경로를 고려해 `AtomicReference<AgentState>` 기반으로 구현하는 방향으로 고정한다.
- `/agent/resume` endpoint 자동 등록은 이번 단위에서 제외하고, 다음 단위(auto-configuration 구조)에서 starter의 `@RestController` 자동 등록 논점으로 다룬다.
- `EngineMetaClient`는 외부 HTTP 의존성 없이 JDK `HttpClient` 기반으로 구현한다.

## 12) 2026-03-28 TurtlePick Agent 설정 파일 초안 메모
- 대상 서버에는 엔진 `config.yml`과 별도로 agent 전용 설정 파일(`turtlepick.yml`)을 둔다.
- 고정 prefix는 `turtlepick.engine.*`, `turtlepick.agent.*`만 사용하고, `logging`, `instrumentation`도 `turtlepick.agent.*` 하위로 포함한다.
- `fail-open`은 설정 항목으로 두지 않고, 엔진 무응답 시 즉시 `LOG_OFF` 정책을 코드 고정값으로 유지한다.
- 파일명 규칙 `{server_id}_{yyyyMMdd}_{HHmmss}.ndjson`은 엔진-agent 계약값이므로 config으로 열지 않고 코드 고정값으로 둔다.
- `turtlepick.engine.log-ready.*`는 첫 단위 범위에서 제외하고, 롤링/log-ready 단위에서 별도 추가한다.

## 13) 2026-03-28 TurtlePick Agent 1단위 확정 요약
- 대상 서버 설정 파일은 `kjspringweb/turtlepick.yml`로 둔다.
- prefix 구조는 `turtlepick.engine.*`, `turtlepick.agent.*`로 고정한다.
- `repo-root`는 optional이며, 미설정 시 `user.dir`를 사용한다.
- `AgentState`는 `LOG_ON / LOG_OFF` 2상태만 사용하고 기본값은 `LOG_OFF`로 둔다.
- `AgentStateHolder`는 `AtomicReference<AgentState>` 기반으로 구현한다.
- `EngineMetaClient`는 JDK `HttpClient` 기반으로 구현한다.
- 파일명 패턴 `{server_id}_{yyyyMMdd}_{HHmmss}.ndjson`은 코드 고정값으로 유지한다.
- `LOG_OFF` 정책은 코드 고정값이며 config으로 열지 않는다.
- 이번 단위 범위 제외: `resume endpoint`, `AOP`, `SQL 계측`, `롤링`, `log-ready`.
- 다음 논점은 `starter auto-configuration 내부 구조`로 진행한다.

## 14) 2026-03-28 TurtlePick Agent 구현 전제 보강
- Agent 대상 서버는 Git 배포를 전제로 하지만, Java 버전이 항상 최신이라고 가정하지 않는다.
- Agent는 "Spring + Git"만 보장되면 동작해야 하며, 특정 외부 라이브러리 추가를 전제로 설계하지 않는다.
- 따라서 bootstrap 초기 통신/직렬화는 가능하면 JDK 기본 API와 수동 직렬화에 가깝게 유지하는 방향을 우선 검토한다.
- 단, Spring Boot 4.x 계열용 auto-configuration 등록 방식은 버전 의존성이 있으므로 공식 문서 기준으로 확인 후 결정한다.

## 15) 2026-03-28 TurtlePick Agent 패키징/부착 전제
- 서버 Agent는 **단일 JAR 파일 하나**로 배포되어 서버에 배치되고, 별도 설정 파일(`turtlepick.yml`)만으로 동작 가능해야 한다.
- 대상 서버의 `build.gradle` 또는 애플리케이션 소스 수정/재빌드를 전제로 하지 않는다.
- Agent는 대상 서버의 프레임워크/라이브러리 버전에 영향을 주지 않는 방향으로 부착되어야 하며, 난독화 배포를 전제로 한다.
- 따라서 기존의 "호스트 앱에 starter dependency를 직접 추가"하는 설계는 기본 전제와 충돌할 수 있으며, 런타임 부착 방식(`loader.path` 기반 외부 JAR 로드 등)을 먼저 확정해야 한다.
- auto-configuration 내부 클래스 설계보다 먼저, "단일 외부 JAR을 서버에 어떤 런타임 방식으로 부착할지"를 선결 논점으로 둔다.

## 16) 2026-03-28 TurtlePick Agent 대상 범위 확장
- 대상 서버 범위는 Spring Boot 애플리케이션에 한정하지 않고, 전자정부 프레임워크 등 **레거시 Spring 기반 애플리케이션까지 포함**한다.
- 따라서 "Spring Boot 전용 starter/auto-configuration 중심 1차 구현" 전제는 폐기하고, 처음부터 **Boot/legacy Spring 공통으로 확장 가능한 부착 방식**을 기준으로 설계해야 한다.
- 대상 서버 수정/재빌드 없이 단일 외부 JAR + 설정 파일만으로 동작해야 한다는 전제는 유지한다.
- 이후 논점은 "Spring Boot 여부와 무관하게 서버 버전에 영향 없이 붙는 공통 런타임 부착 방식"을 먼저 확정하는 방향으로 진행한다.

## 17) 2026-03-28 TurtlePick Agent javaagent 설계 보정 메모
- `javaagent` 기반 공통 bootstrap 방향은 유지하되, 현재 단계에서 `agent-core / spring-bridge / boot-adapter` 3모듈 즉시 분리는 과설계로 보고 보류한다.
- 1차 구현 범위는 여전히 `agent-core` 단일 모듈 기준의 meta 핸드셰이크(`config 로드 -> git hash -> /api/agent/meta -> LOG_ON/LOG_OFF`)로 한정한다.
- Spring/Boot 계측 계층 분리는 AOP/계측 단계의 후속 논점으로 미룬다.
- 다음 핵심 선결 논점은 "javaagent가 Spring 컨텍스트 초기화 완료 시점을 어떤 메커니즘으로 감지할 것인가"이다.
- `SpringApplicationRunListener` 같은 Boot 전용 SPI나 reflection polling만으로는 범위가 좁거나 불안정하므로, 레거시 Spring까지 포괄 가능한 공통 감지 지점을 먼저 검토한다.

## 18) 2026-03-28 TurtlePick Agent Boot 빠른 경로 메모
- `Instrumentation.appendToSystemClassLoaderSearch(agentJar)`를 이용한 self-injection은 Boot 대상에서 중간 경로 후보가 될 수 있지만, 전체 agent 설계의 공통 baseline으로 확정하지는 않는다.
- 이유 1: 대상 범위가 Boot에 한정되지 않고 레거시 Spring까지 포함된다.
- 이유 2: JDK 공식 문서상 `appendToSystemClassLoaderSearch`는 "system class loader가 정의할 instrumentation용 클래스/리소스" 용도로만 신중히 사용하라고 경고하며, 범용 애플리케이션 리소스 노출 경로로 쓰는 것은 위험할 수 있다.
- 따라서 Boot 대상 auto-configuration 활성은 2차 빠른 경로 후보로만 남기고, 1차 baseline은 여전히 `javaagent + agent-core(meta 핸드셰이크)`로 유지한다.

## 19) 2026-03-28 TurtlePick Agent 구현 단계 확정
- 공통 baseline: `javaagent + agent-core + meta 핸드셰이크`
- 1차 구현 범위: `turtlepick.yml 로드 -> git full hash 확보 -> /api/agent/meta -> LOG_ON/LOG_OFF`
- 1차 구현 기술 전제: `JDK only`, `HttpURLConnection`, `수동 JSON`, 프레임워크 무관
- 2차: Boot self-injection fast path (Boot 전용)
- 3차: 레거시 Spring `refresh()` 후킹 기반 계측
- 다음 작업은 추가 설계 핑퐁보다 `agent-core` 구현 초안 수준의 상세 설계로 진행한다.

## 20) 2026-03-28 TurtlePick Agent 설정 형식 보정
- 레거시 환경에서 YAML 처리 제약이 있다면 agent 설정 형식은 `properties`로 간다.
- 1차 `agent-core`는 JDK-only, 프레임워크 무관 원칙에 맞춰 `turtlepick.properties`를 기본 후보로 본다.
- 기존 `turtlepick.yml` 논의는 후순위/옵션으로 내리고, 1차 구현에서는 단순 key-value 기반 파싱을 우선 검토한다.
- 대응 예시:
  - `turtlepick.engine.base-url`
  - `turtlepick.engine.meta.timeout-ms`
  - `turtlepick.agent.server-id`
  - `turtlepick.agent.app-name`
  - `turtlepick.agent.git.repo-root`

## 21) 2026-03-28 TurtlePick Agent-core 코드 구조 메모
- 1차 구현 대상은 `javaagent` 단일 JAR의 `agent-core` 하나로 한정한다.
- 설정 파일은 `turtlepick.properties`를 사용하고, 탐색 순서는 `-Dturtlepick.config` -> `user.dir` -> `agentJarDir`로 둔다.
- 핵심 클래스 축은 `AgentPremain`, `AgentRuntime`, `AgentBootstrapService`, `TurtlepickConfigLoader`, `GitCommitHashProvider`, `EngineMetaClient`, `MetaJsonCodec`, `AgentStateHolder`, `MethodMappingRegistry`, `AgentLog`로 잡는다.
- 1차 범위는 `config 로드 -> git full hash -> /api/agent/meta -> LOG_ON/LOG_OFF`까지만 포함하고, Spring 감지/AOP/SQL 계측/롤링/log-ready/resume은 제외한다.

## 22) 2026-03-28 TurtlePick Agent Java 버전/클래스 구조 보정
- 대상 범위는 "최근 10년 이내의 Spring 프로젝트"까지 포함하며, Agent는 **Java 8 이상 JVM에서 로드 가능**해야 한다.
- `javaagent`는 대상 서버 JVM 위에서 직접 실행되므로, agent JAR 전체 바이트코드와 사용 문법은 Java 8 호환으로 제한한다.
- 따라서 `record`는 1차 `agent-core` 설계에서 전면 제외하고, `CommandResult`, `MetaRequest`, `MethodMapping`, `MetaResponse`, `BootstrapResult`, `AgentConfig` 등은 모두 일반 클래스로 설계한다.
- `HttpURLConnection` 유지, `var` 미사용, JDK 기본 API 중심 원칙을 계속 유지한다.
- `AgentRuntime`은 1차 범위에서 불필요한 의존성 묶음 래퍼로 보고 제외한다. 필요한 객체는 `AgentPremain`에서 직접 조립한다.
- `AgentConfig`는 긴 생성자 대신 `Builder` 패턴을 사용해 구성하며, 특히 다수의 boolean/optional 필드가 순서 실수 없이 조립되도록 한다.
- Java 8 호환 보장은 `source/targetCompatibility`만으로는 부족하므로, 가능하면 빌드 설정에서 `--release 8`을 사용하고 실제 Java 8 JVM에서 `-javaagent` smoke test를 수행하는 방향을 기준으로 둔다.
- 대상 서버의 호스트 빌드 도구는 Gradle로 한정하지 않는다. Maven, Gradle, 구형 빌드 스크립트 여부와 무관하게 **외부 `-javaagent` JAR + `turtlepick.properties`**만으로 부착 가능해야 한다.
- WAR + 외부 Tomcat 같은 경우에는 애플리케이션 소스가 아니라 `setenv.sh` / `setenv.bat` 등 컨테이너 JVM 옵션 위치에 `-javaagent`를 추가하는 운영 절차가 필요할 수 있으므로, 추후 온보딩 가이드에서 케이스별 삽입 지점을 정리한다.
- `AgentLog`의 시간 포맷은 Java 8의 `DateTimeFormatter` static 상수 + `Instant.now()` 기반을 우선 사용한다. `SimpleDateFormat`은 1차 기준에서도 기본안으로 삼지 않는다.
- `MetaJsonCodec`의 수동 JSON 파서는 정규식 전역 탐색이 아니라 **범위 제한(scanner) 방식**으로 설계한다. 필드 탐색 메서드는 `startIndex/endIndex`를 받거나, 잘라낸 객체 단위 문자열만 받도록 해서 중첩 객체/동일 필드명 충돌을 피한다.

## 23) 2026-03-28 TurtlePick Agent 현재 구현 섹터 재확정
- 사용자가 현재 섹터를 아래 1~4단계로 재확정했다.
  1. 서버 기동
  2. 서버 반영 Git 버전 파악
  3. 엔진에 meta 요청
  4. 엔진 meta 응답 수신
- `메타 메모리 저장`, `메소드별 메타 적용`, `로그 추출`, `5분 로그 파일 닫기`, `엔진 log-ready 요청/응답`, 반복 루프는 모두 **후속 섹터**로 미룬다.
- 따라서 현 시점 설계/초안의 성공 기준은 `premain -> git rev-parse HEAD -> /api/agent/meta -> status/reason/agentId/commitHash 수신`까지다.
- 현재 섹터에서 실제 반영/패키징의 선결 산출물은 `agent-core`용 `build.gradle` 초안이다. `methods` 배열 파싱 고도화나 registry 적재는 후속 섹터 블로커로 보지 않는다.

## 24) 2026-03-28 TurtlePick Agent build.gradle 보정
- `JavaCompile.options.release = 8`은 JDK 9+에서만 걸면 충분하며, `options.hasProperty('release')` 같은 Groovy property 체크는 의미 없는 방어로 보지 않는다.
- 1차 범위는 외부 의존성 없는 단일 JAR이므로 `duplicatesStrategy = DuplicatesStrategy.EXCLUDE`는 넣지 않는다. 필요해지는 시점에만 추가한다.
- `Agent-Class` manifest 항목은 `agentmain` 동적 부착용이므로, 1차 범위가 `premain`뿐이라면 제거하거나 "향후 attach 확장 대비"라는 의도를 주석/메모로 명확히 한다.
- `withSourcesJar()`는 1차 `javaagent` 산출물 기준으로 불필요하므로 넣지 않는다.
- 외부 의존성이 없는 현재 범위에서는 빈 `repositories {}` / `dependencies {}` 블록도 생략해 build script를 최소화한다.

## 25) 2026-03-28 kjspringweb 부팅 시간 메모
- 기존 `kjspringweb` 로그 기준 최근 부팅은 `Started KjwebApplication in 3.321 seconds`로 확인됐다.
- `spring.batch.job.enabled=false`라 배치 잡 자동 실행은 기본 부팅 지연 원인이 아니다.
- 새 `javaagent`를 붙여 체감이 더 느리다면, 현재 1차 구현 구조상 `premain`에서 동기 수행하는 `git rev-parse HEAD`와 `/api/agent/meta` HTTP 호출이 가장 유력한 추가 지연 요인이다.
- 현재 저장소 루트에는 `turtlepick.properties`가 없어, 명시적 `-Dturtlepick.config` 없이 실행하면 agent는 빠르게 비활성 실패해야 한다. 실제 체감 지연이 크다면 잘못된 엔진 URL/timeout 쪽을 먼저 본다.

## 26) 2026-03-28 TurtlePick 루트 설정 파일 반영
- 루트 실행 기준 기본 설정 파일 [turtlepick.properties](/d:/workspace/kjspringweb/turtlepick.properties)를 생성한다.
- 이 파일은 dependency/lib 내부 리소스가 아니라, 호스트 앱 루트에 두는 외부 실행환경 파일로 취급한다.
- 현재 기본값은 로컬 엔진 `http://localhost:8081`, `server-id=kjspringweb-local`, `app-name=kjspringweb`, `git.repo-root=.` 기준이다.

## 27) 2026-03-28 반영 턴 후속 규칙
- 사용자가 `확정. 반영`을 지시한 턴에서는 가능하면 반영 후 테스트까지 수행한다.
- 반영 완료 응답에는 최소한 아래 3가지를 포함한다.
  - 반영 요약
  - 반영한 파일 목록
  - 테스트 리뷰(무엇을 실행했고, 통과/실패/미실행 이유가 무엇인지)

## 28) 2026-03-28 TurtlePick agent 1차 smoke test 결과
- `turtlepick-agent-core`는 `..\gradlew.bat jar`로 빌드 성공했고, 산출물은 `turtlepick-agent-core/build/libs/turtlepick-agent-core-0.1.0-SNAPSHOT.jar`이다.
- `bootRun`에 `spring-boot.run.jvmArguments`로 `-javaagent`를 주는 방식은 이번 테스트에서 `[TP-AGENT-BOOT]` 로그가 남지 않아 신뢰 가능한 검증 경로로 보지 않았다.
- 대신 `kjspringweb`를 `bootJar`로 패키징한 뒤 `java -javaagent:... -Dturtlepick.config=... -jar build/libs/kjspringweb-0.0.1-SNAPSHOT.jar --server.port=18081`로 직접 기동해 검증했다.
- 이 경로에서 agent 로그는 아래 순서로 정상 확인됐다.
  - `begin`
  - `config loaded path=...\\turtlepick.properties`
  - `meta ok status=OK agentId=... commitHash=586234a0a0ebbe8819d28805b32ee1c826c8e23f`
- 즉 현재 섹터(서버 기동 전 premain -> git full hash -> engine meta 요청 -> meta 응답 수신)는 **실제로 성공 확인**됐다.
- 다만 그 후 본체 애플리케이션은 JPA/Hibernate 초기화에서 `Unable to determine Dialect without JDBC metadata`로 실패했다. 이는 agent 메타 핸드셰이크 이후의 앱 본체 DB 설정 이슈로 보며, 현재 agent 1차 섹터 성공 여부와는 분리해 본다.
- smoke test 중 띄운 임시 `bootRun` 테스트 인스턴스(18080 포트)는 종료했다.

## 29) 2026-03-28 다음 섹터 메모
- 다음 구현 섹터는 `5번: MethodMappingRegistry 적재`다.
- 목표는 `/api/agent/meta`의 `methods` 배열을 파싱해 메모리 registry에 적재하는 것까지이며, 실제 "메소드별 메타 적용"이나 AOP 연결은 아직 포함하지 않는다.
- 현재 `MetaJsonCodec`는 top-level 필드만 파싱하므로, 이 섹터에서 `MethodMapping`, `MethodMappingRegistry`, `MetaResponse.methods`, `AgentBootstrapService` 적재 흐름을 추가해야 한다.
- `methods` 파싱이 처음으로 핵심이 되는 시점이므로, 이 구간부터는 범위 제한(scanner) 방식 파서를 검토하고 정규식 임시 구현은 지양한다.

## 30) 2026-03-28 5번 섹터 범위 확정
- 현재 섹터는 `methods 배열 파싱 -> MethodMappingRegistry 적재 -> methodCount 로그`까지만 다룬다.
- 처리 기준:
  - `status=OK` + `methods.size() > 0` -> `registry.replaceAll(...)` 후 `LOG_ON`
  - `status=OK` + `methods.size() == 0` -> `registry.clear()` + `LOG_OFF` + WARN 로그
  - `status!=OK` 또는 예외 -> `registry.clear()` + `LOG_OFF`
- 엔진 다운/재시도/재연결/polling/push/agent endpoint/Spring 컨텍스트 후킹 논의는 이 섹터에서 제외하고 후속 섹터로 미룬다.
- `methods`는 이후 실행 경로의 핵심이므로 partial load를 허용하지 않고, 전체 파싱/검증이 끝난 뒤 원자적으로 registry를 교체하는 방향을 우선한다.
- 현재 메모리 적재의 기본 개념은 `canonicalMethodSignature -> methodId` 매핑이다. 즉 "`xxx.xxx.Class#method(paramTypes...) : id`" 쪽이 본질이며, 단순 "`메소드명 : 아이디`" 수준으로 두지 않는다.

## 31) 2026-03-28 엔진 메소드 독립성 판정 메모
- 현재 엔진은 `methodId` 생성 시 `fqcn + methodName + paramTypes`를 사용하고, `fqcnMethod`도 `Class#method(paramTypes...)` 형태로 저장/전달한다. 따라서 **저장 키 수준의 메소드 독립성은 상당 부분 확보**되어 있다.
- 근거:
  - `IdGenerator.methodId(...)`는 `"M|fqcn#method(paramTypes)"`를 CRC32로 ID화하고, 같은 commit 내 충돌 시 예외를 던진다.
  - `MappingAssembler`, `BusinessLayerScanner`, `BatchJobExtractor`, `ServiceCallExtractor` 모두 `fqcnMethod`에 파라미터 타입을 포함해 저장한다.
- 다만 **완전 보장이라고 단정하긴 어렵다.**
  - `ServiceCallExtractor`는 타깃 메소드 탐색 시 `메소드명 + 인자 개수` 기준으로 찾는 경로가 있어, 같은 인자 개수의 overload가 있으면 잘못 연결될 여지가 있다.
  - 파라미터 타입 문자열도 `JavaParser asString()` 기반이라 항상 완전 정규화된 FQCN 형태라고 단정할 수는 없다.
- 결론:
  - "단순히 메소드 시그니처를 구분할 수 있느냐" 기준이면 현재 엔진은 **대체로 예**.
  - "overload/타입 해석까지 포함해 완전 무오류 독립성이 보장되느냐" 기준이면 **아직 아니며, 이 부분은 엔진 보강 후보**다.
- 다만 이 이슈는 **현재 5번 섹터(MethodMappingRegistry 적재) 범위 밖**으로 본다. 현재 섹터는 엔진이 내려준 `methods` 배열을 메모리에 적재하는 것까지이며, 실제 호출 메소드 추출/매칭 단계에서 다시 다룬다.

## 32) 2026-03-28 5번 섹터 registry 방향/파서 메모
- 5번 섹터의 `MethodMappingRegistry` 기본 조회 방향은 `canonicalMethodSignature -> methodId`로 둔다. 런타임에서 실제 호출 메소드 시그니처를 얻은 뒤 ID를 찾아야 하므로, `fqcnMethod -> methodId` 방향이 현재 목적에 맞다.
- 과거 문서/초안에 있던 `methodId -> fqcnMethod` 방향은 6번 섹터 이후 런타임 조회 관점에서는 비효율적이므로, 필요하면 보조 인덱스로만 고려한다.
- `MetaJsonCodec`의 현재 scanner 초안에서 `findFieldValueStart()`가 `indexOf()` 기반이면 다른 string 값 내부의 필드명과 오탐 가능성이 있다. 현 시점에서는 블로커로 보지 않지만, scanner 고도화 시 문자열 컨텍스트 인식 방식으로 보강 후보로 남긴다.

## 33) 2026-03-28 5번 섹터 반영/테스트 결과
- 5번 섹터(`methods 배열 파싱 -> MethodMappingRegistry 적재 -> methodCount 로그`)를 실제 코드로 반영했다.
- 반영 내용:
  - `MethodMapping` 추가
  - `MethodMappingRegistry` 추가 (`fqcnMethod -> methodId`)
  - `MetaResponse.methods` 추가
  - `MetaJsonCodec`를 methods 배열까지 파싱하는 scanner 기반 구현으로 확장
  - `AgentBootstrapService`에서 `methods>0`일 때만 registry 적재 후 `LOG_ON`, 비어 있으면 `LOG_OFF + WARN`
  - `BootstrapResult.methodCount` 추가
  - `AgentPremain` 성공 로그에 `methodCount` 출력
- 테스트:
  - `turtlepick-agent-core`에서 `..\gradlew.bat jar` 성공
  - `kjspringweb`에서 `.\gradlew.bat bootJar` 성공
  - `java -javaagent:... -Dturtlepick.config=... -jar build/libs/kjspringweb-0.0.1-SNAPSHOT.jar --server.port=18081` smoke test 성공
  - 로그에서 `[TP-AGENT-BOOT] meta ok ... methodCount=88` 확인
  - 임시 테스트 인스턴스(18081)는 종료 완료

## 34) 2026-03-28 6번 섹터 범위 메모
- 6번 섹터는 "서버 런타임에서 실제 호출된 메소드/엔드포인트를 추출하고, 5번 섹터에서 적재한 registry와 매칭하는 단계"다.
- 서버 쪽 책임은 아래와 같다.
  - HTTP 요청 진입점 식별
  - 호출된 handler/service 메소드 시그니처 추출
  - 추출한 시그니처로 `MethodMappingRegistry(fqcnMethod -> methodId)` 조회
  - 매칭 성공 시 엔진 methodId를 로그 컨텍스트에 싣기
- 이 섹터에서는 아직 로그 파일 롤링/`log-ready`/엔진 재연결 정책까지 들어가지 않는다.
- registry에 없는 메소드 호출 시의 정책(tempId, skip, LOG_OFF 등)은 6번 후반~7번 로그 섹터에서 확정한다.

## 35) 2026-03-28 6번 섹터 서버 역할 한 줄 정리
- 6번 섹터에서 서버가 하는 일의 본질은 "`실제 호출된 메소드/엔드포인트를 런타임에서 알아내고, 이미 받아 둔 엔진 메타(methodId)와 연결하는 것`"이다.
- 즉 5번 섹터가 "메타를 받아 저장"이라면, 6번 섹터는 "실행 중인 코드에 그 메타를 붙이는 준비" 단계다.

## 36) 2026-03-28 엔진식 전수 추출 vs 서버 역할 구분
- 서버가 6번 섹터에서 해야 하는 일은 엔진처럼 소스 전체를 다시 스캔해 "전수 목록"을 만드는 작업과는 다르다.
- 엔진은 정적 분석으로 commit 기준의 endpoint/method 목록과 ID를 만든다.
- 서버는 런타임에서 "지금 실제로 호출된 endpoint/method"를 식별하고, 그 시그니처를 엔진이 준 메타와 매칭하는 쪽이 본체다.
- 다만 후속 섹터에서 엔진 메타와 서버 활성 메소드/엔드포인트의 불일치 여부를 검증하려면, 서버 쪽에서도 활성 endpoint/method 목록을 한 번 수집하는 보조 검증 단계는 들어갈 수 있다.
- 현재 6번 섹터의 최소 범위는 전수 추출이 아니라 런타임 식별 + registry 조회다.

## 37) 2026-03-28 6번 섹터와 로그 기록 타이밍의 관계
- "실행중인 메소드 식별"은 실제로는 로그를 남기기 직전/동시에 일어나는 작업이 맞다.
- 다만 설계상 6번 섹터에서는 이를 "`로그를 쓰기 위한 선행 정보 확보`"로 분리해서 본다.
- 즉 6번 섹터의 책임은 "어떤 메소드가 실행됐는지 알아내고 methodId를 붙일 수 있게 만드는 것"까지이고, 실제 파일 기록/롤링/전송은 7번 이후 로그 섹터로 넘긴다.

## 38) 2026-03-28 6번 섹터 최종 한 줄 정의
- 6번 섹터의 결론은 "`실행중인 요청에 대해 endpoint/method를 식별하고, 엔진 메타의 endpointId/methodId와 매핑해 로그 기록 직전에 붙일 수 있게 만드는 단계`"다.

## 39) 2026-03-28 6번 섹터 계약 보정 메모
- 현재 엔진 `/api/agent/meta` 계약은 `methods[] = { methodId, fqcnMethod }`만 내려주며, `endpointId` 목록은 내려주지 않는다.
- 따라서 6번 섹터의 현실적인 1차 구현안은:
  - method 쪽은 `fqcnMethod -> methodId` 매핑까지 완결
  - endpoint 쪽은 런타임에서 `HTTP_METHOD + normalizedPath + handlerSignature` 식별까지 수행
  - 단, `endpointId`까지 붙이려면 엔진 메타 계약 확장이 별도로 필요
- 즉 6번 섹터는 `6A(methodId 매핑 완성)`와 `6B(endpointId 계약 확장 후 연결)`로 나눠 보는 것이 정확하다.

## 40) 2026-03-28 6번 섹터 선결 과제 메모
- `TurtlepickHandlerInterceptor`, `@Aspect`, `BeanPostProcessor` 같은 Spring 참여 컴포넌트는 "이름만 정한다고" 동작하지 않는다.
- javaagent 기준으로 6번 섹터 선결 과제는 "`Spring 컨텍스트/BeanFactory 생명주기에 agent가 어떻게 끼어들 것인가`"다.
- 특히 단순 "refresh 완료 후 후킹"은 늦을 수 있다. AOP 프록시/HandlerMapping 초기화 전에 agent 컴포넌트를 등록할 수 있는 시점을 잡아야 한다.
- 따라서 6번 섹터의 실제 다음 논점은 "`SpringContextInstaller` 메커니즘(어느 시점에 BeanFactory에 무엇을 등록할지)`"이다.

## 41) 2026-03-28 바이트코드 후킹 라이브러리 선결 메모
- `ClassFileTransformer`만으로는 부족하고, 실제 `AbstractApplicationContext.refresh()` 후킹을 하려면 바이트코드 조작 라이브러리가 필요하다.
- 현재 권고는 `ASM`을 agent JAR에 **shading/relocation** 해서 번들하는 방향이다.
- 이유:
  - Java 8 호환성/경량성 면에서 유리
  - 이번 요구는 "특정 Spring 클래스 1~2개 지점에 정적 callback 삽입"이라 고수준 DSL보다 저수준 ASM이 오히려 단순하다
  - host 앱/host Spring이 들고 있는 ASM류와 충돌하지 않게 agent 내부 패키지로 relocate 하는 것이 안전하다
- `ByteBuddy`/`Javassist`는 대안이지만, 현재 기준에서는 크기/의존성/범용성보다 "작고 단순한 hook"이 우선이다.
- 따라서 6번 섹터 선결 구현은 "`ASM shaded agent + SpringContextInstaller hook point 확정`"으로 본다.
- 패키징 시 `shadowJar` 기본 classifier가 `all`이면 기존 `-javaagent` 경로가 깨질 수 있으므로, 필요 시 `shadowJar { archiveClassifier = '' }`로 산출물 파일명을 기존 기본 JAR 이름과 맞춘다.

## 42) 2026-03-28 6번 섹터 실제 구현 방향 메모
- 6번 섹터 실제 구현의 1차 목표는 `6A(methodId 매핑)`이다.
- 선결 패키징:
  - `shadow` 플러그인 적용
  - `asm`, `asm-commons` 번들
  - `relocate 'org.objectweb.asm', 'com.turtlepick.agent.shadow.asm'`
  - `shadowJar { archiveClassifier = '' }`
- 선결 hook:
  - `AgentPremain`에서 `ClassFileTransformer` 등록
  - 대상: `org/springframework/context/support/AbstractApplicationContext`
  - `refresh()` 내부에서 `invokeBeanFactoryPostProcessors(beanFactory)` 직전 또는 그와 동등한 조기 지점에 `SpringContextInstaller.install(this, beanFactory)` static call 삽입
- installer 역할:
  - context당 1회만 설치
  - `MappedInterceptor` 등록으로 HTTP 진입점 식별 준비
  - 이후 method interception용 `BeanPostProcessor` 등록
- 6A 런타임 목표:
  - 요청 시 endpoint 시그니처 식별
  - 실행 메소드 시그니처를 `fqcnMethod` 형식으로 변환
  - `MethodMappingRegistry(fqcnMethod -> methodId)` 조회
  - 찾은 `methodId`를 `TraceContextHolder`에 적재

## 43) 2026-03-28 ASM 없는 6번 섹터 대안 메모
- 현재 6번 섹터 범위를 "`controller/handler method -> methodId 매핑`"까지만 제한하면, `ASM` 없이도 갈 수 있는 대안이 있다.
- 방향:
  - `ServletContainerInitializer SPI` + `Filter`
  - 요청 진입 시 Spring `WebApplicationContext`를 얻고
  - `RequestMappingHandlerMapping.getHandler(request)`로 현재 `HandlerMethod`를 식별
  - `fqcnMethod -> methodId` registry 조회
  - `TraceContextHolder`에 적재 후 체인 진행
- 장점:
  - 외부 런타임 라이브러리 추가 없음
  - 현재 6섹터 최소 목표(controller method 매핑)에는 충분
- 주의:
  - `javax.servlet`와 `jakarta.servlet` 두 계열을 함께 고려해야 함
  - `-javaagent` JAR가 embedded Boot / 외부 servlet container 모두에서 `ServletContainerInitializer` 스캔 대상이 되는지는 PoC 검증이 필요
- 결론:
  - 6번 섹터를 controller 매핑으로 자르면 "무라이브러리 경로"가 유력하다
  - service method 깊이까지 가는 시점에만 ASM/추가 interception 여부를 재검토한다

## 44) 2026-03-28 6번 섹터 범위 축소 금지 메모
- `ServletContainerInitializer + Filter`는 **웹 요청 진입점**에 대한 유력 대안일 뿐, 6번 섹터 전체를 그것으로 확정하면 안 된다.
- 대상 서버는 일반적인 웹 MVC만이 아니라 배치, 스케줄러, Kafka/메시지 리스너 같은 비-HTTP 실행 경로도 포함될 수 있다.
- 따라서 6번 섹터의 상위 개념은 "`실행 진입점(entry point) 식별 + methodId 매핑`"이어야 하며, entry point 종류별 구현을 분리해서 봐야 한다.
- 최소 분류:
  - HTTP/MVC
  - Batch(Job/Step/Tasklet)
  - Schedule(@Scheduled)
  - Messaging(Kafka/JMS/기타 listener)
  - 기타 비동기 executor
- 이후 구체 구현은 "공통 TraceContext + entry type별 resolver/adapter" 구조로 가는 것이 범위 축소를 막는다.

## 45) 2026-03-28 추가 라이브러리 정책 메모
- "추가 라이브러리 없이"의 우선 의미는 **호스트 서버/호스트 프로젝트에 의존성 추가 없음**으로 본다.
- HTTP 외 Batch/Schedule/Kafka 등 전 진입점을 포괄하려면, agent 내부에 제3자 라이브러리를 **shaded + relocated** 형태로 번들하는 선택지는 열어 둔다.
- 단, 이 경우에도 조건은 엄격하다.
  - host build.gradle/pom 수정 없음
  - host classpath 충돌 없음 (relocate)
  - Java 8+ 호환 유지
  - agent JAR 1개 + properties 1개 배포 원칙 유지
- 만약 "agent JAR 내부도 순수 JDK만"을 절대 조건으로 잡으면, 전 진입점 포괄은 현실적으로 매우 어려워진다.

## 46) 2026-03-28 ASM 운영 기준 메모
- ASM을 쓴다면 최소 기준은 `9.6+`, 가능하면 최신 `9.x`를 우선 검토한다.
- `shadow + relocate`는 필수다.
- `ClassWriter.COMPUTE_FRAMES`는 stack frame / maxs 계산 실수로 인한 `VerifyError` 리스크를 크게 줄이는 기본 옵션으로 본다.
- 다만 이것만으로 모든 런타임 문제를 0으로 만드는 것은 아니다. 잘못된 변환 대상 선택, module 접근 제한, classpath 기반 타입 해석 문제 등은 별도로 남는다.
- 운영 기준은 `8/11/17/21` JVM smoke test를 붙여 실제 host JVM별로 확인하는 것이다.

## 47) 2026-03-28 6번 섹터 상세 구현 방향 메모
- 6번 섹터의 상세 구현은 "`Spring bean을 끼워 넣는 방식`"보다 먼저 "`엔진 메타(methodId) 기반으로 대상 애플리케이션 메소드를 직접 계측`"으로 보는 것이 범용성 면에서 낫다.
- 이유:
  - HTTP/Batch/Schedule/Kafka 모두 결국 "애플리케이션 메소드 실행"으로 귀결된다.
  - Spring 컨텍스트 참여 메커니즘만으로 풀면 entry type마다 설치 지점이 갈라진다.
  - 현재 엔진 meta 계약도 `methodId + fqcnMethod`만 내려주므로, method 중심 계측이 현재 계약과 맞다.
- 구현 축:
  - `MethodProbeIndex` : meta methods를 class/method/param 기준으로 파싱한 계측 대상 인덱스
  - `ApplicationMethodTransformer` : 대상 메소드 enter/exit hook 삽입
  - `RuntimeMethodBridge` / `TraceContextHolder` : 런타임 methodId 컨텍스트 관리
  - entry type별 부가 컨텍스트 capturer는 HTTP/SCHEDULE/KAFKA/BATCH 순으로 별도 확장

## 48) 2026-03-28 6번 섹터 구현 선결 디테일 메모
- 계측된 앱 클래스에서 agent bridge static 메소드를 직접 호출하려면, `AgentPremain`에서 `inst.appendToSystemClassLoaderSearch(agentJar)`가 선행돼야 한다.
- 그렇지 않으면 앱 클래스 로더가 `RuntimeMethodBridge`를 찾지 못해 `NoClassDefFoundError` 위험이 있다.
- `ApplicationMethodTransformer`의 대상 클래스 필터 기준은 "`MethodProbeIndex에 존재하는 className만 변환`"으로 명시한다.
- 즉 JDK/Spring/third-party 제외를 별도 복잡 규칙으로 풀기보다, meta에서 받은 계측 대상 클래스 집합을 allow-list로 쓰는 방식이 1차 구현에 가장 안전하다.

## 49) 2026-03-28 endpointId 메타 계약 확인 메모
- "현재 meta에 endpointId가 없다"는 뜻은 실제 `/api/agent/meta` 응답 JSON 계약에 `endpoints[]`나 `endpointId` 필드가 없다는 의미다.
- 엔진 내부 DB/정적 분석 모델에는 `endpoint_mapping`과 `endpoint_id`가 존재한다.
- 하지만 서버 agent에 내려주는 현재 wire contract는 `methods[] = { methodId, fqcnMethod }`만 포함한다.

## 50) 2026-03-28 endpoint 없는 로그에 대한 원칙 메모
- endpoint 없이 methodId만으로 로그를 남기는 것은 "기술적으로는 가능"하지만, 현재 TurtlePick 사상과는 맞지 않는다.
- 이유:
  - 이 프로젝트는 `entry point(endpoint)`를 흐름 귀속의 최상위 기준으로 본다.
  - 엔진 내부 모델도 `endpoint_mapping` + `method_mapping(parent_method_id)` 중심이다.
  - endpoint 없이 method만 기록하면 "어느 요청/배치/리스너 진입에서 시작된 흐름인지"가 약해진다.
- 따라서 장기 방향은 endpointId를 반드시 로그에 싣는 쪽으로 봐야 한다.
- 현재 meta 계약에 endpointId가 없다는 점은 "서버 로그에서 endpoint를 빼도 된다"는 뜻이 아니라, **계약 공백이 남아 있다**는 의미로 해석해야 한다.

## 51) 2026-03-28 순서 판단 메모
- 5번 섹터(method 메타 수신/registry 적재)는 이미 닫혔다.
- 6번 섹터를 더 진행하면 결국 endpointId 없는 임시 우회 구조를 만들 가능성이 높아진다.
- 따라서 현재 시점의 권고 순서는:
  1. 서버 agent 5번 섹터까지는 유지
  2. **지금 바로 엔진 meta 계약(endpointId/entrypoint 계층) 보강 논의로 이동**
  3. 엔진 계약이 닫힌 뒤 6번 섹터(server runtime 매핑) 재개
- 즉 "하던 걸 더 마무리"가 아니라, **현재가 끊는 지점**으로 본다.

## 52) 2026-03-28 순서 판단 보정 메모
- 위 판단은 과하게 끊은 표현이었다.
- 6번 섹터의 `ASM 기반 method 계측 공통축` 자체는 endpointId 계약과 독립적으로 병렬 진행 가능하다.
- 즉 다음 두 축은 병렬이 가능하다.
  - 서버: `MethodProbeIndex`, `ApplicationMethodTransformer`, `RuntimeMethodBridge`, `TraceContextHolder(methodId 중심)`
  - 엔진: `/api/agent/meta`에 endpoint/entrypoint 계층 추가 계약 보강
- 단, 서버 쪽은 **7번 로그 기록 포맷과 endpointId 귀속이 필요한 지점까지는 고정하지 않는다.**
- 정리하면: `6섹터 공통 계측 인프라`는 계속 가고, `endpointId를 실제 로그에 싣는 설계`는 엔진 계약 보강과 합류 후 닫는다.

## 53) 2026-03-28 6번 섹터 클래스 초안 메모
- 6번 섹터 코드 초안은 Java 8 기준으로 아래 축으로 정리한다.
  - `MethodSignatureParser`, `ParsedMethodSignature`
  - `MethodProbeSpec`, `MethodProbeIndex`, `MethodProbeIndexBuilder`
  - `ApplicationMethodTransformer`, `ApplicationClassVisitor`, `MethodProbeAdviceAdapter`
  - `RuntimeMethodBridge`, `TraceContextHolder`, `RuntimeTraceContext`, `MethodFrame`
- 현재 목표는 `methodId 중심 런타임 컨텍스트`까지이며, endpointId/로그파일/롤링은 포함하지 않는다.

## 54) 2026-03-28 6번 섹터 구현 함정 체크리스트
- `ApplicationMethodTransformer.transform()`는 `loader == null`이면 즉시 `return null`로 bootstrap classloader 클래스 변환을 건너뛴다.
- ASM 소스 import는 **여전히** `org.objectweb.asm.*`를 사용하고, relocation은 빌드 산출물에서 `shadowJar`가 처리한다. 소스에서 `com.turtlepick.agent.shaded.asm.*`를 직접 import하는 구조는 아니다.
- `MethodProbeAdviceAdapter`의 owner/descriptor는 문자열 수기 조립보다 `Type.getInternalName(RuntimeMethodBridge.class)` / `Type.getMethodDescriptor(...)` 방식 우선.
- `AgentPremain` 순서는 `appendToSystemClassLoaderSearch(agentJar)` -> `meta/bootstrap` -> `MethodProbeIndex build` -> `addTransformer(...)` 로 고정한다.
- `RuntimeMethodBridge.exit()`는 `peek/pop` 기준으로 stack mismatch를 방어하고, 계측된 각 종료 지점마다 1회 호출된다는 전제를 안전하게 처리해야 한다.
- `MethodSignatureParser`/`MethodProbeIndexBuilder`에서 메타 파싱 실패가 발생하면 silent skip 하지 않고 최소 WARN 로그는 남긴다.
- `ClassWriter(COMPUTE_FRAMES)` 사용 시 기본 `getCommonSuperClass()`는 transform 중 class loading/circularity 문제를 만들 수 있으므로, `ApplicationMethodTransformer`에서 안전한 override를 둔다.
- 1차 안전안은 `type1.equals(type2) ? type1 : "java/lang/Object"` 형태의 보수적 구현이다. 필요 이상으로 host class loading에 의존하지 않는다.
- `ApplicationClassVisitor.visitMethod()`에서는 `ACC_NATIVE` 메서드를 반드시 skip 한다. native 메서드는 바이트코드가 없어 `AdviceAdapter` 대상이 아니다.
- `MethodProbeIndex.find()`에서 ambiguous match는 `throw`로 올리지 않고 `WARN + null`로 처리한다. `ClassFileTransformer.transform()` 예외는 JVM에 의해 조용히 무시될 수 있으므로, 로그 가시성이 더 중요하다.

## 55) 2026-03-28 6번 섹터 1차 반영/검증
- 반영 범위:
  - `shadow + ASM` 패키징 도입 (`build.gradle`)
  - `AgentPremain`에 `appendToSystemClassLoaderSearch(...)` + `MethodProbeIndex` 기반 transformer 등록
  - `instrument` 패키지 추가 (`MethodSignatureParser`, `MethodProbeIndex`, `ApplicationMethodTransformer`, `ApplicationClassVisitor`, `MethodProbeAdviceAdapter` 등)
  - `trace` 패키지 추가 (`RuntimeMethodBridge`, `TraceContextHolder`, `RuntimeTraceContext`, `MethodFrame`)
- 구현 안전장치:
  - `loader == null` skip
  - `ACC_NATIVE`, `ACC_ABSTRACT`, `<init>`, `<clinit>` skip
  - `Type.getInternalName/getMethodDescriptor` 사용
  - `SafeClassWriter.getCommonSuperClass()` override
  - ambiguous probe -> `WARN + null`
- 테스트:
  - `turtlepick-agent-core`에서 `..\\gradlew.bat shadowJar` 성공
  - `kjspringweb`에서 `.\\gradlew.bat bootJar` 성공
  - `java -javaagent:... --server.port=18082` smoke test 성공
  - `/login` 요청 HTTP 200 확인
  - agent stderr에 `[TP-AGENT-BOOT][INFO] method probe installed ... methodCount=88` 확인

## 56) 2026-03-28 docs/ha202603281636.md 재검토 메모
- 사용자가 Claude 문서 수정 완료를 알렸고, 실제 문서를 다시 확인했다.
- 앞서 지적했던 3개 중 아래 2개는 수정 반영이 확인됐다.
  - 개요 상태 문구: `완료 — smoke test 통과`
  - `build.gradle` 예시: `repositories { mavenCentral() }` 포함, shadow 버전 블록은 `9.2.1`
  - `MetaJsonCodec` TODO: scanner 전환 미완 표현 제거, 이미 구현됨으로 정정
- 다만 문서 내부에 shadow 버전 문구가 아직 일부 `9.3.2`로 남아 있다.
  - 3-1 절 설명 문장
  - 9절 폴더 구조 주석
  - 10절 완료 체크 항목
- 실제 반영 코드의 shadow 버전은 `turtlepick-agent-core/build.gradle` 기준 `9.2.1`이다.
- 추가로 로컬 `CLAUDE.md`는 아직 예전 방향(`Spring Boot starter auto-configuration`, `AgentStartupListener`, `turtlepick.yml`)을 유지하고 있어 6섹터 javaagent/ASM 방향과는 동기화되지 않았다.

## 57) 2026-03-28 엔진 meta 계약 보강 지시문 메모
- 엔진 `/api/agent/meta` 계약 보강 방향은 `methods[].endpointId` 단일 필드가 아니라 `endpoints[] + endpointMethods[]` 다대다 구조로 정리됐다.
- 이유: 동일 service method가 복수 endpoint(HTTP/BATCH/SCHEDULE/KAFKA 등)에서 재사용될 수 있어 `method 1 -> endpoint N` 관계가 가능하다.
- 원칙은 additive change only:
  - 기존 `status`, `agentId`, `commitHash`, `methods[].methodId`, `methods[].fqcnMethod` 유지
  - 신규로 `endpoints[]`, `endpointMethods[]` 추가
- agent 측 후속 범위:
  - `MetaResponse`에 `endpoints`, `endpointMethods` 추가
  - `EndpointInfo`, `EndpointMethodLink` 신규
  - `MetaJsonCodec` 파싱 확장
  - 7섹터에서 `RuntimeTraceContext.endpointId`, `entryType` 사용
- 최종 지시문에는 다음 보강도 포함한다:
  - `entryType`는 기존 engine entryType 값 유지 (`HTTP/BATCH/SCHEDULE/KAFKA/INTERFACE` 등)
  - contract invariant:
    - `endpoints[].entryMethodId`는 반드시 `methods[].methodId` 중 하나
    - `endpointMethods[].endpointId`는 반드시 `endpoints[].endpointId` 중 하나
    - `endpointMethods[].methodId`는 반드시 `methods[].methodId` 중 하나
    - `endpointMethods[]` 내 `(endpointId, methodId)` pair 중복 금지

## 58) 2026-03-28 엔진쪽에서 본 서버 agent 상태 메모
- 엔진 쪽 관점에서도 서버 agent 평가는 크게 같다.
  - 6섹터(method probe 인프라)는 안정적으로 올라온 상태
  - 다음 핵심 블로커는 `endpoints[]` 수용과 `entryMethodId` 기반 endpoint 귀속
- 다만 최종 엔진 계약은 `endpointMethods[]` 제외 방향으로 정리됐으므로, 서버 문서 stale 포인트는 이후 정리 필요하다.
- 서버 agent의 본체 리스크는 기능 부족보다도 Java 8/레거시 Spring/Boot/classloader/배포 방식 호환성이다.

## 59) 2026-03-28 엔진 meta 계약 반영 결과 검토 메모
- 엔진 쪽 최종 방향은 `methods[] + endpoints[]` 구조로 정리됐고, `endpointMethods[]`는 이번 범위에서 제외됐다.
- 서버 7섹터 기준으로는 이 변경으로 핵심 블로커가 대부분 해소됐다.
- 다만 서버 구현 시 주의:
  - 공식 endpoint root는 더 이상 "첫 번째 계측 메서드"가 아니라 `endpoints[].entryMethodId` 기준으로 잡아야 한다.
  - 동일 `entryMethodId`가 복수 endpoint에 매핑될 가능성(HTTP alias 등)은 서버에서 `entryType/httpMethod/entryKey`를 활용해 분기할 여지가 남아 있다.
- 엔진 문서 텍스트 중 `EngineMetaClient = JDK HttpClient` 같은 agent 현황 설명은 실제 서버 코드(`HttpURLConnection`)와 어긋날 수 있으니 문서 인용 시 주의.

## 60) 2026-03-28 endpoint root invariant 메모
- 엔진에서 `엔드포인트당 루트 메서드는 하나`로 고정하기로 결정됐다.
- 따라서 서버 7섹터는 `endpoints[].entryMethodId`를 공식 root 기준으로 신뢰해도 된다.
- 엔진 쪽 invariant:
  - endpoint 하나당 root method는 정확히 1개
  - 0개 또는 2개 이상이면 `ENGINE_DATA_INTEGRITY_ERROR` 성격으로 처리
- 다만 `하나의 root method가 복수 endpoint에 대응`하는 가능성은 별도 축이므로, 서버는 필요 시 `entryType/httpMethod/entryKey`까지 보고 endpoint 귀속을 결정한다.

## 61) 2026-03-28 엔진 root method 조회 정책 보정
- 엔진에서는 `method_mapping.parent_method_id = 0` 루트 메서드를 조회하는 쿼리 단계에서 endpoint별 root 개수를 판정하기로 했다.
- 즉 후처리에서 "여러 개면 실패"가 아니라, 조회/조립 시점에 `정확히 1개` invariant를 강제하는 방향이다.
- root method가 0개이거나 2개 이상이면 정상 응답을 만들지 않고 `ENGINE_DATA_INTEGRITY_ERROR` 성격으로 처리한다.

## 62) 2026-03-28 서버 남은 작업 시간 추정 메모
- 현재 서버 agent 상태:
  - 1~6섹터 완료
  - 다음은 7섹터(endpoint 귀속 로그 + 파일 기록/롤링), 이어서 8~9섹터(log-ready/수거 응답)
- 대략 추정:
  - 7섹터만 happy path 기준: 5~8시간
  - 7섹터 + 8~9섹터까지 묶으면: 8~14시간
  - 레거시/호환성 이슈(Java 8, Boot 차이, 요청 외 진입점, 파일 I/O 엣지케이스)까지 붙으면: 12~20시간도 가능
- 현재 가장 시간이 갈 구간은 기능 구현보다 endpoint 귀속 규칙과 파일 상태 전이(safe close/rolling/log-ready 타이밍) 검증이다.

## 63) 2026-03-29 TurtlePick/대상서버 재분석 메모
- 이번 세션에서 확인한 참조 범위:
  - `D:\workspace\turtlepick\README.md`, `gpt.md`, `CLAUDE.md`, `work_protocol.md`, `docs/*`
  - 현재 저장소 `gpt.md`, `CLAUDE.md`, `work_protocol.md`, `docs/*`
- `turtlepick` 실제 루트 구조는 여전히 `engine-app`, `engine-core-private` 2모듈이며, 문서 초기에 반복되던 별도 `server-agent` 모듈은 현재 저장소에 없다.
- 현재 대상서버 쪽 실제 agent 코드는 `D:\workspace\kjspringweb\turtlepick-agent-core`에 들어와 있고, 상태는 1~6섹터 완료 수준이다.
  - `AgentPremain` + `MethodProbeIndex` + ASM transformer까지 반영돼 있다.
  - smoke test 로그 파일들이 현재 저장소 루트에 남아 있다.
- 엔진 쪽 `/api/agent/meta`는 현재 코드 기준 `methods[] + endpoints[]`를 반환한다.
  - `D:\workspace\turtlepick\engine-app\src\main\java\com\turtlepick\contract\AgentMetaResponse.java`
  - `D:\workspace\turtlepick\engine-app\src\main\java\com\turtlepick\adapter\AgentAdapter.java`
- 반면 현재 대상서버 agent consumer는 아직 `methods[]`만 파싱한다.
  - `turtlepick-agent-core/http/MetaResponse.java`
  - `turtlepick-agent-core/http/MetaJsonCodec.java`
  - 즉 엔진 계약 보강은 완료됐지만, 서버 agent 7섹터 진입에 필요한 `endpoints[]` 수용은 아직 로컬 agent에 미반영 상태다.
- 엔진의 `log-ready` 수신 골격은 구현돼 있지만 실제 파일 I/O는 아직 stub 단계다.
  - `LogReadyService.checkSourceFileExistsStub`
  - `LogFileProcessor.readLogFileStub/storeDailyTempStub/deleteSourceFileStub`
- 현재 `kjspringweb` 실제 HTTP 진입점은 컨트롤러 선언 기준 24개다.
  - 여기에 Spring Security 처리 경로 `POST /auth/login`, `POST /auth/logout`까지 포함하면 관측 관점 entry point 수는 달라질 수 있다.
  - 일부 외부 문서의 `25개` 표기는 이 프레임워크 처리 경로 포함 가능성을 염두에 두고 해석해야 한다.
- `MemberController` 실제 기본 경로는 여전히 `/member`이며, 구식 문서의 `/profile`, `/withdraw` 단독 표기는 stale이다.
- 예외 처리 구조는 현재도 이원화다.
  - MVC: `GlobalExceptionHandler`에서 referer redirect + flash 에러
  - REST: `ApiExceptionHandler`에서 JSON + HTTP status
  - 따라서 TurtlePick 로그/에러 귀속은 최종 응답 형태뿐 아니라 발생 예외와 진입 타입을 함께 봐야 한다.
- 로컬 설정 기준:
  - 대상서버 agent 설정은 `turtlepick.properties`
  - 엔진 설정은 `D:\workspace\turtlepick\config.yml`
  - 엔진 `monitoring-branches`에 `develop`, `work`가 포함돼 있어, 문서대로 현재 원격 부재 경고가 반복될 가능성이 높다.
- 이번 턴은 사용자 지시상 분석 전용으로 처리했고, 실제 파일 수정은 맥락 유지 목적의 `gpt.md` 갱신만 수행했다.

## 64) 2026-03-29 루트 로그 정리 분석 메모
- 현재 저장소 루트의 지저분한 로그 파일:
  - `bootrun.out.log`, `bootrun.err.log`
  - `agent-java.out.log`, `agent-java.err.log`
  - `agent-smoke.out.log`, `agent-smoke.err.log`
  - `agent6-java.out.log`, `agent6-java.err.log`
- 확인 결과 이는 Spring Boot `logging.file.*` 산출물이 아니라, 수동 실행 검증 과정에서 stdout/stderr를 루트 파일로 리다이렉션해 남긴 운영/실험 로그 성격이다.
  - 현재 `src/main/resources/application.yml`에는 파일 로그 경로 설정이 없고 `logging.level`만 있다.
  - `turtlepick.properties`의 `turtlepick.agent.logging.dir=./turtlepick-logs`는 향후 agent 자체 로그 파일 경로용이지만, 현재 7섹터 이전이라 루트 clutter 원인은 아니다.
- 따라서 정리 방향은 애플리케이션 로깅 정책 수정 이전에 "수동 실행 산출물 경로 표준화"가 우선이다.
  - 제안 최소안:
    - 루트 외부가 아니라 프로젝트 내부 `logs/yyyyMMdd/` 또는 `logs/manual/` 하위로 stdout/stderr 파일을 모은다.
    - `.gitignore`에 `*.out.log`, `*.err.log`, `logs/`, `turtlepick-logs/`를 추가한다.
    - 반복 실행 명령은 PowerShell 스크립트 또는 고정 명령 템플릿으로 묶어 루트 직접 redirection을 금지한다.
- 사용자의 최신 명시 트리거(`확정. 반영`) 전까지는 실제 파일 이동/삭제/설정 반영은 수행하지 않는다.

## 65) 2026-03-29 서버 자체 로그 정비 반영 메모
- 사용자 `확정. 반영.` 지시에 따라 서버 자체 로그 정비를 실제 반영했다.
- 반영 방향:
  - 콘솔 로그는 로컬 확인 편의를 위해 `INFO` 기준으로 넓게 유지
  - 파일 로그는 운영 서버용으로 `ERROR`만 적재
  - 로그 경로는 상대 `logs/` 하드코딩 대신 `LOG_PATH` 환경변수/프로퍼티 기본값 `${LOG_PATH:logs}`로 외부화
  - 에러 파일은 현재 파일 `logs/kjspringweb-error.log`, 롤링 파일 `logs/yyyyMMdd/kjspringweb-error.yyyymmdd.i.log.gz`
- 실제 반영 파일:
  - `src/main/resources/logback-spring.xml`
  - `src/main/resources/application.yml`
  - `.gitignore`
- 세부 정책:
  - `root=INFO`
  - `org.springframework.batch=INFO` 명시 유지
  - `org.springframework.security=WARN` 유지
  - Git 반영 제외: `logs/`, `turtlepick-logs/`, `*.out.log`, `*.err.log`
- 검증 결과:
  - `.\gradlew.bat bootJar` 성공
  - `.\gradlew.bat test` 실패
  - 실패 원인은 로깅 설정이 아니라 기존 기본 템플릿 테스트 `src/test/java/com/example/demo/DemoApplicationTests.java`의 패키지/설정 불일치다.
  - 실패 메시지 핵심: `Unable to find a @SpringBootConfiguration by searching packages upwards from the test`
- 후속 보정:
  - 최초 반영안의 `fileNamePattern`은 `%d{yyyyMMdd}`를 2회 사용해 `SizeAndTimeBasedRollingPolicy` 기동 오류 후보가 있었다.
  - 사용자 확정에 따라 B안으로 보정: 날짜 폴더를 제거하고 파일명에만 날짜를 포함한다.
  - 최종 패턴: `${APP_LOG_PATH}/${APP_NAME}-error.%d{yyyyMMdd}.%i.log.gz`
  - `bootJar` 재검증 성공
  - `java -jar ... --spring.datasource.url=jdbc:h2:mem:testdb --server.port=0` 기동 검증에서도 logback 파싱 오류 없이 정상 시작 확인

## 66) 2026-03-29 agent 7-1 endpoints 귀속 실구현안 메모
- 사용자 최신 합의 기준으로 agent 7-1의 첫 반영 범위는 `endpoints[] 수용 + HTTP context 적재 + root method endpoint 귀속`까지다.
- 1차 범위에서 제외:
  - trace 파일 기록/롤링
  - engine `log-ready` 호출
  - `*`, `**`, regex 기반 URI matcher
- 확정 구현 규칙:
  - `MetaResponse`는 기존 `methods[]` 외에 `endpoints[]`를 함께 수용한다.
  - endpoint registry는 `entryMethodId -> List<EndpointInfo>`만 유지한다. `byEndpointId` 맵은 7-1 범위에서 사용처가 없어 넣지 않는다.
  - HTTP 귀속은 `DispatcherServlet#doDispatch` instrumentation으로 `HttpRequestContextHolder`에 `httpMethod`, `requestUri`를 적재하는 방식으로 간다.
  - `HttpServletRequest`에 대한 agent compile dependency는 두지 않고 bridge에서 `Object` + reflection으로 `getMethod()`, `getRequestURI()`, `getContextPath()`를 호출한다.
  - HTTP context clear는 반드시 `try/finally`로 보장한다. Tomcat thread reuse 때문에 `finally` 누락은 오귀속 위험이 크다.
  - `javax.servlet` / `jakarta.servlet` descriptor는 둘 다 검사하되, 실제 매칭되는 `DispatcherServlet#doDispatch` 하나만 instrument한다.
- 1차 URI matcher 규칙:
  - `entryKey`와 `requestUri` 모두 leading slash 보정, duplicate slash 제거, trailing slash 제거 후 비교
  - `requestUri`는 `contextPath` 제거 후 정규화
  - segment 개수 exact match만 허용
  - literal segment는 exact match
  - `{...}` segment는 single-segment wildcard
  - 범위를 벗어나거나 애매하면 과감히 `unresolved + WARN` 처리
- 구현 우선순위:
  1. `MetaResponse` / `MetaJsonCodec` endpoints 파싱
  2. `EndpointRegistry`, `EndpointResolver`, `ResolvedEndpoint`
  3. `HttpRequestContextHolder`, `HttpRequestContextBridge`
  4. `DispatcherServlet` transformer
  5. `RuntimeTraceContext` / `RuntimeMethodBridge` root attach 확장

## 67) 2026-03-29 agent 7-1 구현안 보정 메모
- `EndpointRegistry.replaceAll()`의 내부 맵은 키 조회용이라 `LinkedHashMap`이 아니라 `HashMap`을 사용한다.
- `HttpRequestContextBridge.enter(Object request)`는 fail-open 원칙을 따른다.
  - reflection 실패, null 요청, 예상 외 runtime 예외가 나와도 절대 요청 처리 흐름으로 전파하지 않는다.
  - 내부에서 예외를 삼키고 `AgentLog.warn(...)`만 남긴 뒤 `HttpRequestContextHolder.clear()` 또는 no-op 처리한다.
- 동일 원칙으로 `HttpRequestContextBridge.exit()`도 예외를 전파하지 않는다.
- fatal 재전파는 `throw t`가 아니라 `throw (Error) t` 형태로 고정한다.
  - `catch (Throwable t)` 안에서 `throw t`를 쓰면 checked exception 가능성 때문에 `throws Throwable` 선언이 필요할 수 있다.
  - `isFatal(t)` 대상은 `VirtualMachineError` / `ThreadDeath` 계열만 보므로 `Error` 캐스팅 재전파가 의도와 컴파일 양쪽에서 더 안전하다.
- `DispatcherServlet` instrumentation의 try/finally 삽입 코드는 `DispatcherServletDoDispatchAdapter.visitCode()`에서 시작 label / try-catch block / bridge enter 호출을 배치하고, `visitMaxs()`에서 catch handler + `exit()` + `ATHROW`를 마감하는 방식으로 구현하는 쪽이 안전하다.
- 핵심 보장:
  - bridge 예외가 운영 요청 500으로 이어지지 않는다.
  - `finally` clear는 정상/예외 경로 모두에서 보장된다.

## 68) 2026-03-29 agent 7-1 endpoints 귀속 1차 반영 메모
- 사용자 `확정. 반영` 지시에 따라 agent 7-1의 1차 구현을 실제 반영했다.
- 실제 반영 범위:
  - `MetaResponse` / `MetaJsonCodec`에 `endpoints[]` 수용 추가
  - `EndpointInfo`, `EndpointRegistry`, `EndpointResolver`, `ResolvedEndpoint` 신규 추가
  - `HttpRequestContext`, `HttpRequestContextHolder`, `HttpRequestContextBridge` 신규 추가
  - `DispatcherServlet` 대상 HTTP transformer 신규 추가
  - `RuntimeTraceContext`, `RuntimeMethodBridge`에 root endpoint attach 확장
  - `AgentBootstrapService`, `BootstrapResult`, `AgentPremain`에 endpoint bootstrap / 로그 추가
- 구현 세부:
  - endpoint registry는 `entryMethodId -> List<EndpointInfo>`만 유지
  - URI matcher는 세그먼트 exact count + literal exact match + `{...}` wildcard 1단계 규칙만 지원
  - HTTP context bridge는 fail-open으로 동작하며 reflection 실패를 요청 500으로 전파하지 않는다
  - `DispatcherServlet#doDispatch`는 `javax` / `jakarta` descriptor 둘 다 검사하고, `safeEnter` + `safeExit` + catch-all clear 구조로 instrument한다
- 검증 결과:
  - `turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공
  - 루트 `.\gradlew.bat bootJar` 성공
  - 임시 Java smoke로 `MetaJsonCodec.decodeMetaResponse()` + `EndpointResolver.resolve()` 검증 성공
    - `codec-ok methods=1 endpoints=1 endpointId=2001 status=RESOLVED`
  - agent 부착 서버 smoke:
    - 임시 ASCII 설정으로 agent attach 후 `/` 요청 200 확인
    - mock HTTP listener는 현재 PowerShell background 제약 때문에 안정적으로 재현되지 않아, attach smoke에서는 engine 미연결 시 `meta log_off ... HTTP_ERROR:ConnectException` fail-open 경로만 확인
  - 루트 `.\gradlew.bat test`는 기존 `DemoApplicationTests` 설정 불일치로 계속 실패

## 69) 2026-03-29 agent 7-2 trace file writer 구현안 리뷰 메모
- 7-2 방향 자체는 맞다. 현재 `RuntimeMethodBridge.exit()` root exit에서 `RuntimeTraceContext`가 그냥 버려지고 있어 파일 기록 단계가 필요하다.
- 다만 구현 전 보정 포인트:
  - root flush 구간은 `TraceContextHolder.clear()`를 반드시 `finally`에서 보장해야 한다.
    - `TraceLogSerializer.serialize(...)` 또는 예상 밖 runtime 예외가 나면 현재 스레드의 trace context가 남아 stale 상태가 될 수 있다.
  - `TraceLogWriter.install(...)` 위치는 `AgentBootstrapService`보다 `AgentPremain` 쪽이 더 자연스럽다.
    - 현재 `RuntimeMethodBridge.installEndpointResolver(...)`도 `AgentPremain` 성공 경로에 있고, singleton runtime wiring을 bootstrap service 내부로 섞지 않는 편이 구조상 깔끔하다.
  - serializer JSON 필드명은 `entryKey` vs `endpointEntryKey` 중 하나로 확정해야 한다.
    - 현재 `RuntimeTraceContext` 필드/게터는 `endpointEntryKey` 축으로 되어 있어 serializer 출력 키도 이에 맞추는 편이 일관적이다.

## 70) 2026-03-29 agent 7-2 trace file writer 반영 메모
- 사용자 `확정. 반영` 지시에 따라 7-2를 실제 반영했다.
- 실제 반영 범위:
  - `TraceLogSerializer` 신규 추가
  - `TraceLogWriter` 신규 추가
  - `RuntimeMethodBridge.exit()` root exit에서 serialize -> write -> finally clear 추가
  - `AgentPremain` 성공 경로에 `TraceLogWriter.install(config.getLoggingDir(), config.getRollingIntervalMinutes())` 추가
- 구현 정책:
  - JSON 키는 `endpointEntryType`, `endpointEntryKey`, `endpointHttpMethod`, `endpointResolutionStatus` 기준으로 고정
  - serializer는 JDK-only 수동 JSON 조립, null 값은 필드 생략 없이 JSON `null`
  - writer는 `trace-{yyyyMMddHHmm}.log` 파일명, slot 기반 롤링, `synchronized` 보호
  - writer/flush 예외는 `AgentLog.warn(...)`만 남기고 삼킴
  - root flush는 `finally`에서 `TraceContextHolder.clear()`를 보장한다
- 검증 결과:
  - `turtlepick-agent-core` `..\gradlew.bat shadowJar` 성공
  - 루트 `.\gradlew.bat bootJar` 성공
  - 임시 Java smoke로 serializer + writer 검증 성공
    - `trace-202603291628.log`
    - JSON line에 `entryMethodId`, `timestampMs` 정상 포함 확인
  - 임시 Java smoke로 `RuntimeMethodBridge.enter/exit` root flush 경로 검증 성공
    - `trace-202603291630.log`
    - JSON line에 `entryMethodId=321`, `entryFqcnMethod=com.example.HomeController#home()` 기록 확인
  - 루트 `.\gradlew.bat test`는 기존 `DemoApplicationTests` 설정 불일치로 계속 실패

## 71) 2026-03-29 실엔진 연동 검증 메모
- 엔진 `http://localhost:8081/api/agent/meta` 실응답은 확인했다.
- 현재 저장소 HEAD `c50fe31e69440d2268ab700ef739eabd860c199f`로 직접 meta 요청 시 응답:
  - HTTP 200
  - `{"status":"LOG_OFF","reason":"COMMIT_NOT_INDEXED", ... }`
- 실제 agent 부착 서버 smoke도 수행했다.
  - `java -javaagent:turtlepick-agent-core-0.1.0-SNAPSHOT.jar -jar build/libs/kjspringweb-0.0.1-SNAPSHOT.jar --server.port=18082 ...`
  - `/` 요청 200 확인
  - agent stderr 로그에 `config loaded ...` 후 `meta log_off ... reason=COMMIT_NOT_INDEXED` 확인
- 결론:
  - 실엔진 네트워크 연동 및 fail-open 경로는 확인됨
  - 하지만 엔진이 현재 커밋을 아직 인덱싱하지 않아 `status=OK + methods[] + endpoints[]` 경로의 실엔진 end-to-end 검증은 아직 못 했다
  - 따라서 trace 파일 실엔진 양성 검증은 커밋 인덱싱 후 재실행 필요

## 72) 2026-03-29 turtlepick engine bootRun 실패 원인 메모
- 사용자가 `work`를 `main`에 병합한 뒤 `:engine-app:bootRun` 실패를 보고했고, 실제 `--stacktrace` 실행으로 원인을 확인했다.
- 실제 실패 원인:
  - `ConfigurationPropertiesBindException`
  - `database.url` 바인딩 값이 `null`
  - `DatabaseProperties` 검증에서 `공백일 수 없습니다`
- 직접 원인:
  - 현재 [application.yml](d:/workspace/turtlepick/engine-app/src/main/resources/application.yml) 의 `spring.config.import`가 `optional:file:./config.yml`만 가리킨다.
  - 실제 설정 파일은 [config.yml](d:/workspace/turtlepick/config.yml)에 있고, `bootRun` 작업 디렉터리는 `D:\workspace\turtlepick\engine-app`라서 `./config.yml`이 비어버린다.
  - 그래서 `database.url`, profile 등 외부 설정이 로드되지 않고 기본 profile + null datasource 설정으로 기동하다가 죽는다.
- 참고:
  - 이전 세션에서 읽었던 engine `application.yml`에는 `optional:file:../config.yml` 경로가 함께 있었고, 그때는 루트 `config.yml`을 정상 인식했던 정황이 있다.
  - 즉 이번 failure는 git sync/branch 문제 이전에 `config import path`가 루트 `config.yml`을 못 보게 된 설정 회귀로 보는 게 맞다.

## 73) 2026-03-29 turtlepick main/work 실제 상태 확인 메모
- 사용자 요청에 따라 `D:\workspace\turtlepick` 저장소의 실제 `main` / `work` 상태를 확인했다.
- 결과:
  - `main = 029dbc3f90dcfa6f880bad7b19928707128f03d6`
  - `work = 6c26240cf399cf6d16815ad013939a546ca9ace4`
  - `git branch --contains 6c26240...` 결과는 `work`만 반환되고 `main`은 반환되지 않는다.
  - `git log main..work`에는 8개 커밋이 남아 있어 현재 `main`은 `work` 최신 내용을 포함하지 않는다.
- 따라서 현재 눈앞의 `turtlepick` 워킹트리 기준으로는 `work`와 `main`이 동일하지 않다.
- `main` reflog에는 `merge work: Fast-forward` 흔적이 있지만 결과 포인터가 `029dbc3`에 머물러 있어, 지금의 `work` 최신 tip(`6c26240`)이 이 `main`에 실제 반영된 상태는 아니다.
- 결론:
  - `work`에서 bootRun이 되고 `main`에서 안 되는 건 정상이다.
  - 원인은 `main`이 아직 `work` 최신 커밋과 `application.yml` 변경(`../config.yml` import 포함)을 가지고 있지 않기 때문이다.

## 74) 2026-03-29 push 후 재검증 메모
- `kjspringweb` 현재 HEAD는 `4123e550a44763c53d41b4694149004732c871a3`이고, 로컬 기준 `work...origin/work`로 보여 push 자체는 반영된 상태다.
- 하지만 엔진 재기동 후에도 실응답은 동일했다.
  - `POST http://localhost:8081/api/agent/meta` with commit `4123e550...`
  - 결과: HTTP 200, `status=LOG_OFF`, `reason=COMMIT_NOT_INDEXED`
- 최신 engine 로그 `D:\workspace\turtlepick\logs\engine-app-20260329-165141.log` 기준:
  - startup 후 runtime clone은 여전히 `586234...`를 보고 있음
  - polling sync에서 `remote branch not found: work`
  - `branches=[main, develop, work] newCommits=0 skipped=5`
- 따라서 push는 되었지만 engine runtime clone의 fetch 대상이 여전히 `main`만이라 `origin/work`를 로컬 ref로 못 만들고 있다.
- 추가 확인:
  - engine startup resume는 `http://localhost:8080/agent/resume`로 실패했다
  - 직접 `http://localhost:8080/login` 조회도 연결 실패였으므로, target server가 현재 8080에서 떠 있지는 않은 상태로 보인다

## 75) 2026-03-29 재기동 후 최신 실검증 메모
- 현재 `kjspringweb` HEAD는 `4123e550a44763c53d41b4694149004732c871a3`.
- 엔진 `http://localhost:8081/api/health`는 HTTP 200 `UP`로 정상.
- 하지만 `POST /api/agent/meta` with commit `4123e550...` 결과는 여전히:
  - HTTP 200
  - `status=LOG_OFF`
  - `reason=COMMIT_NOT_INDEXED`
  - `methods=[]`
  - `endpoints=[]`
- 최신 engine 로그에서도 polling sync는 계속:
  - `remote branch not found: work`
  - `branches=[main, develop, work] newCommits=0 skipped=5`
  - startup analysis 대상은 여전히 `586234a0...`
- 따라서 현재 시점 결론은 동일하다.
  - 엔진은 살아 있지만 `kjspringweb` 현재 커밋 `4123e55...`는 아직 인덱싱되지 않았다.
  - 원인은 engine runtime clone이 `work` 브랜치 ref를 못 잡는 쪽에 남아 있다.

## 76) 2026-03-29 runtime clone fetch spec 수정 후 실엔진 메타 OK 확인
- 사용자가 engine runtime clone에서 아래 순서를 직접 수행했다.
  - `git -C D:\turtlepick\runtime\source\kjspringweb config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"`
  - `git -C D:\turtlepick\runtime\source\kjspringweb fetch --prune origin`
  - 결과로 `origin/work`가 생성되고 `rev-parse origin/work = 4123e550a44763c53d41b4694149004732c871a3` 확인
- 이어서 `POST /api/git/sync` 수동 호출 결과:
  - HTTP 200
  - `status=SUCCESS`
  - `toCommit=4123e550a44763c53d41b4694149004732c871a3`
  - `processedCount=2`
- 마지막으로 `POST /api/agent/meta` with commit `4123e550...` 결과:
  - HTTP 200
  - `status=OK`
  - `agentId=kjspringweb-local-...`
  - `methods[]`, `endpoints[]` 포함한 실제 메타 payload 수신 성공
- 결론:
  - 기존 `COMMIT_NOT_INDEXED` 원인은 engine runtime clone의 fetch spec/main-only 상태였다.
  - fetch spec 수정 + manual sync 후 실엔진 메타는 정상 복구됐다.

## 77) 2026-03-29 agent end-to-end 실기동 검증 결과
- 목적:
  - 실엔진 메타 `status=OK` 상태에서 `kjspringweb`를 `-javaagent`로 기동하고 실제 공개 엔드포인트 호출 후 trace 파일 생성까지 확인
- 기동 명령:
  - `java -javaagent:D:\workspace\kjspringweb\turtlepick-agent-core\build\libs\turtlepick-agent-core-0.1.0-SNAPSHOT.jar -jar D:\workspace\kjspringweb\build\libs\kjspringweb-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:h2:mem:testdb --server.port=8080 --spring.batch.job.enabled=false`
- 결과:
  - agent bootstrap 자체는 성공
  - boot 로그 기준:
    - `config loaded path=D:\workspace\kjspringweb\turtlepick.properties`
    - `method probe installed commitHash=4123e550... methodCount=88 endpointCount=28 httpInstrumentation=true`
  - 그러나 Spring MVC 초기화 중 `DispatcherServlet#doDispatch` 변조 bytecode 검증에서 서버가 기동 실패
    - `Caused by: java.lang.VerifyError: Bad type on operand stack`
    - location: `org/springframework/web/servlet/DispatcherServlet.doDispatch(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V`
    - reason: `Type 'java/lang/Object' ... is not assignable to 'java/lang/Exception'`
- 코드 기준 의심 지점:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/instrument/DispatcherServletDoDispatchAdapter.java`
  - 현재 `visitCode()` + `onMethodExit()` + `visitMaxs()`로 try/catch를 합성하는 방식이 Spring Boot 4 / Tomcat 11의 `jakarta` `DispatcherServlet#doDispatch` stack map과 충돌하는 것으로 보임
- 부수 결과:
  - 서버가 뜨기 전에 죽어서 `http://localhost:8080/auth/login`는 연결 실패
  - `turtlepick-logs/` 디렉터리도 생성되지 않았고 trace 파일 검증은 아직 못 감
- 현재 결론:
  - 엔진 메타/endpoint 공급은 정상
  - agent 7-1/7-2의 첫 실기동 blocker는 `DispatcherServlet` HTTP instrumentation의 verifier 오류

## 78) 2026-03-29 VerifyError 원인 추가 진단 메모
- Claude의 `VerifyError: Bad type on operand stack = ASM frame 문제` 진단은 맞는 방향으로 보인다.
- 특히 1차 용의자는 `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/instrument/SpringWebRequestTransformer.java`의 내부 `SafeClassWriter#getCommonSuperClass()` 구현이다.
  - 현재 구현은 `type1 != type2`면 거의 무조건 `java/lang/Object`를 반환한다.
  - `DispatcherServlet#doDispatch`처럼 기존 예외 핸들러와 stack map frame이 많은 복잡한 메서드에서 `Exception` 계열 공통 상위를 `Object`로 낮춰버리면 verifier가 `Object is not assignable to Exception`로 죽을 수 있다.
- 실제 기동 오류 메시지:
  - `Bad type on operand stack`
  - `Type 'java/lang/Object' ... is not assignable to 'java/lang/Exception'`
- 따라서 현재 판단은:
  - 단순히 `DispatcherServletDoDispatchAdapter`의 try/catch 삽입이 거칠다는 수준을 넘어서,
  - frame 재계산 시 공통 상위 타입 계산을 너무 보수적으로 `Object`로 뭉갠 것이 핵심 원인 후보다.
- 같은 `SafeClassWriter` 패턴은 `ApplicationMethodTransformer`에도 있으므로, 수정 시 둘 다 같이 보는 편이 안전하다.

## 79) 2026-03-29 DispatcherServletDoDispatchAdapter 로컬 슬롯 제거 제안 판정
- 사용자가 제안한 수정안:
  - `DispatcherServletDoDispatchAdapter`에서 `throwableLocalIndex` 필드 제거
  - `visitCode()`의 `newLocal(Throwable.class)` 제거
  - `visitMaxs()` handler에서 `storeLocal/loadLocal` 없이, handler 진입 스택의 `Throwable`를 그대로 둔 채 `safeExit()` 후 `ATHROW`
- 내 판정:
  - 이 수정은 `synthetic catch handler`의 복잡성을 줄이는 1차 보정으로 타당하다.
  - 다만 `ASM LocalVariablesSorter + newLocal + storeLocal/loadLocal` 조합 자체는 일반적으로 허용되는 패턴이라, `double remap`을 확정 원인으로 단정하기보다는 verifier-safe하게 단순화하는 패치로 보는 게 정확하다.
  - 즉 `로컬 슬롯 제거`는 해볼 가치가 높은 유효한 수정이다.
- 후속 판단 기준:
  - 이 패치 후에도 `VerifyError`가 남으면, 다음 1순위 원인은 여전히 `SafeClassWriter#getCommonSuperClass()`의 과도한 `java/lang/Object` 반환이다.

## 80) 2026-03-29 HTTP instrumentation verifier 수정 및 실검증 결과
- 반영:
  - `DispatcherServletDoDispatchAdapter`에서 synthetic handler의 `Throwable` 로컬 슬롯 사용 제거
  - `SpringWebRequestTransformer`, `ApplicationMethodTransformer`의 `SafeClassWriter#getCommonSuperClass()`를 실제 상속관계 기반으로 계산하도록 보정
- 실기동 결과:
  - 기존 `VerifyError: Bad type on operand stack`는 재현되지 않음
  - `-javaagent`로 `kjspringweb`를 띄우면 Tomcat 8080까지 정상 기동
  - agent bootstrap 로그:
    - `method probe installed commitHash=4123e550... methodCount=88 endpointCount=28 httpInstrumentation=true`
- 실제 요청 검증:
  - `GET /auth/login`은 HTTP 200이지만 trace 파일에 추가 라인이 생기지 않음
  - `GET /`는 HTTP 200이고 `turtlepick-logs/trace-202603291723.log`에 아래와 같이 실제 trace 기록 확인
    - `entryFqcnMethod=com.kjweb.web.controller.HomeController#home()`
    - `endpointEntryType=HTTP`
    - `endpointEntryKey=/`
    - `endpointHttpMethod=GET`
    - `requestMethod=GET`
    - `requestUri=/`
    - `endpointResolutionStatus=RESOLVED`
- `/auth/login`이 안 찍히는 직접 원인:
  - engine meta의 method signature가 `AuthController#loginPage(String,Model)`처럼 parameter simple name 기준으로 내려옴
  - agent `MethodProbeIndex`는 runtime descriptor를 `java.lang.String`, `org.springframework.ui.Model` 같은 canonical FQCN으로 비교
  - 그래서 object parameter가 있는 controller method 상당수가 probe 매칭에 실패하는 상태
- 현재 상태 요약:
  - 엔진 meta/endpoints 공급: 정상
  - HTTP instrumentation verifier 문제: 해결
  - trace writer/file rolling: 동작 확인
  - 남은 blocker:
    - simple-name parameter signature와 runtime canonical type 비교 불일치

## 81) 2026-03-29 simple-name vs FQCN 불일치 책임 위치 판정
- 현재 판단:
  - 남은 이슈의 1차 책임은 agent가 아니라 engine-core-private AST 추출 계층이다.
  - agent의 `MethodProbeIndex`는 runtime descriptor를 canonical FQCN으로 비교하고 있어 방향 자체는 맞다.
- 근거:
  - `engine-core-private/src/main/java/com/turtlepick/core/service/MappingAssembler.java`
    - `resolveHandlerParamTypes()`가 `p.getType().asString()`을 그대로 사용
  - `engine-core-private/src/main/java/com/turtlepick/core/service/BusinessLayerScanner.java`
    - method param types를 `asString()`으로 저장
  - `engine-core-private/src/main/java/com/turtlepick/core/service/BatchJobExtractor.java`
    - method param types를 `asString()`으로 저장
  - `engine-core-private/src/main/java/com/turtlepick/core/service/ServiceCallExtractor.java`
    - `toFqcnMethod()`도 `asString()` 기반
  - `IdGenerator.methodId(...)`는 이 param type 문자열 자체를 해시 키로 사용하므로, 수정은 adapter가 아니라 method-id 생성 파이프라인 전체에서 일관되게 이뤄져야 한다.
- 결론:
  - 수정 위치는 `engine-app` 어댑터보다 앞단인 `engine-core-private` method definition 생성 계층이 맞다.
  - 수정 후에는 현재 commit 재인덱싱이 필요하다.

## 82) 2026-03-29 FQCN 정규화 작업지시 문서 리뷰 메모
- `task-fqcn-param-normalization.md` 방향은 전반적으로 타당하다.
- 다만 구현 시 아래 2가지는 보정 권장:
  - `ParamTypeResolver.stripGenerics()`를 먼저 적용하면 `List<String>[]` 같은 타입에서 배열 suffix가 유실될 수 있다. 배열/varargs 처리는 제네릭 제거보다 앞이나 별도 로직으로 다루는 편이 안전하다.
  - 재인덱싱 SQL은 `method_def`, `method_mapping`만이 아니라 같은 commit의 `endpoint_mapping`도 함께 비우는 쪽이 더 안전하다. methodId가 바뀌는 재인덱싱이므로 target commit row 일괄 삭제 후 sync가 깔끔하다.
- 추가 의견:
  - `String...` 같은 varargs는 bytecode에선 `String[]`이므로 resolver에서 `... -> []` 정규화가 필요하다.

## 83) 2026-03-29 FQCN 작업지시서 추가 리뷰 포인트
- `rawType`에 점(`.`)이 있다고 바로 FQCN으로 간주하는 규칙은 느슨하다.
  - `Outer.Inner`, `ResponseEntity.BodyBuilder` 같은 nested/simple type도 점을 포함할 수 있다.
  - package-qualified FQCN인지 판정 규칙을 더 보수적으로 두거나, import/current package 해석을 먼저 거치는 편이 안전하다.
- 재인덱싱 SQL은 문서 본문처럼 `commit_version 유지 + is_active=false`보다, 현재 샌드박스 단계에서는 `endpoint_mapping`, `method_mapping`, `method_def`, `commit_version` 전체 초기화 쪽이 합의된 방향이다.

## 84) 2026-03-29 엔진 작업지시서 확정본 반영 메모
- 사용자 지시에 따라 엔진 docs에 최종 작업지시서 파일 생성:
  - `D:\workspace\turtlepick\docs\작업지시서_20260329.md`
- 반영한 보정 포인트:
  - 배열 suffix는 제네릭 제거보다 먼저 처리
  - varargs `...`는 `[]`로 정규화
  - `contains(".")`만으로 FQCN 확정하지 않고 더 보수적 규칙 사용
  - 샌드박스 환경 기준 재인덱싱 SQL은 전체 초기화 방식으로 정리

## 85) 2026-05-02 TurtlePick 대상서버/엔진 문서 재분석 메모
- 사용자 요청에 따라 `D:\workspace\turtlepick`의 `README.md`, `gpt.md`, `CLAUDE.md`, `docs/*`와 현재 `kjspringweb`의 `gpt.md`, `work_protocol.md`, `docs/*`를 다시 읽고 실제 코드 구조를 대조했다.
- 현재 `kjspringweb` HEAD는 `6e32875811200f879762478aadc73a9850d89e98`이며, git status 기준 추적되지 않은 `.claude/`만 보인다.
- 현재 `turtlepick` HEAD는 `842c0385035ef56af0ace351940ac85747f2f8b8`, branch는 `work`이며, git status 기준 추적되지 않은 `.claude/`만 보인다.
- `kjspringweb`는 여전히 Spring Boot 4.0.2 + Java 17 + Thymeleaf + Security + JPA/H2 + Batch 구조이고, `turtlepick-agent-core`가 대상서버 저장소 내부에 별도 javaagent 모듈로 존재한다.
- `turtlepick-agent-core`는 meta handshake, ASM method probe, HTTP context bridge, endpoint resolver, trace serializer/writer까지 구현되어 있으며 아직 `log-ready` 전송 단위는 구현되어 있지 않다.
- `turtlepick` 엔진은 `engine-app`/`engine-core-private` 구조, Git sync, AST 발번, `/api/agent/meta`의 `methods[] + endpoints[]`, `/api/agent/log-ready` ACK 스텁, startup RESUME 전송까지 구현된 상태다.
- 최신 확인된 핵심 blocker는 문서와 실제 코드가 일치한다: `engine-core-private`의 `BusinessLayerScanner`, `BatchJobExtractor`, `MappingAssembler`, `ServiceCallExtractor`가 여전히 `p.getType().asString()` 기반이라 파라미터 타입이 FQCN으로 정규화되지 않는다.
- 따라서 `/auth/login`처럼 `String`, `Model` 등 파라미터가 있는 메서드는 engine method signature(`String,Model`)와 agent runtime descriptor(`java.lang.String,org.springframework.ui.Model`)가 불일치하여 probe 매칭/trace 누락 위험이 남아 있다.
- 현재 프로젝트의 `MemberController` 실제 기본 경로는 `/member`이고 profile/withdraw는 `/member/profile`, `/member/withdraw`다. 과거 외부 문서의 `/profile`, `/withdraw` 단독 표기는 최신 코드와 다르다.
- `kjspringweb`의 `DemoApplicationTests.java`는 여전히 `com.example.demo` 패키지 잔재라 `gradlew test` 실패 원인으로 남아 있으며, 이번 분석 턴에서는 코드/테스트 수정은 수행하지 않았다.

## 86) 2026-05-02 TurtlePick 엔진 연동 실검증 메모
- 사용자가 TurtlePick 엔진 서버를 기동했다고 알려서 `kjspringweb` 대상서버에서 엔진 방향 가능한 검증을 수행했다.
- 엔진 health:
  - `GET http://localhost:8081/api/health` -> HTTP 200 `UP`
  - `GET http://localhost:8081/api/agent/health` -> HTTP 200 `UP`
- 현재 대상서버 commit:
  - `6e32875811200f879762478aadc73a9850d89e98`
- 엔진 meta:
  - full hash `POST /api/agent/meta` -> HTTP 200, `status=OK`, `methodCount=88`, `endpointCount=28`
  - short hash `6e328758` -> HTTP 200, `status=LOG_OFF`, `reason=COMMIT_NOT_INDEXED`
  - unknown hash -> HTTP 200, `status=LOG_OFF`, `reason=COMMIT_NOT_INDEXED`
  - invalid payload -> HTTP 400, `INVALID_REQUEST`
- `POST /api/git/sync` -> HTTP 200, `status=SUCCESS`, `processedCount=0`, `fromCommit/toCommit=6e328758...`
- `kjspringweb`와 `turtlepick-agent-core` 빌드:
  - `.\gradlew.bat bootJar` 성공
  - `turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공
- agent 부착 실기동:
  - `-javaagent:...\turtlepick-agent-core-0.1.0-SNAPSHOT.jar`
  - 임시 포트 `18081`, H2 mem DB로 기동
  - agent stderr 기준 `method probe installed commitHash=6e328758... methodCount=88 endpointCount=28 httpInstrumentation=true`
  - `/`, `/auth/login`, `/auth/join`, `/board` 요청은 HTTP 200 확인
- trace 결과:
  - `turtlepick-logs/trace-202605021610.log` 생성/갱신
  - `/` 요청은 `HomeController#home()`으로 `endpointResolutionStatus=RESOLVED`, `endpointEntryKey=/`, `requestUri=/` 기록 확인
  - `GreetingBatchConfig#morningGreetingJob()`, `afternoonGreetingJob()`, `cleanupGreetingJob()`은 `NO_CANDIDATE`로 기록됨. 이는 batch config method가 probe 대상에는 있으나 endpoint root 후보에는 없는 상태로 보인다.
  - `/auth/login`은 HTTP 200이지만 trace 라인으로 확인되지 않았다. meta 응답의 `AuthController#loginPage(String,Model)`처럼 아직 FQCN 정규화 전 signature라 agent descriptor와 불일치하는 기존 blocker가 재확인됐다.
- log-ready API:
  - 첫 `POST /api/agent/log-ready` -> HTTP 200 `{"resultCode":"ACK"}`
  - 순차 중복 요청 -> HTTP 200 `{"resultCode":"ALREADY_PROCESSED"}`
  - 병렬 중복 요청 2건은 둘 다 `ACK`가 나왔다. 현재 DB unique는 `file_name` 단독이고 코드 선조회 후 insert 구조라 동시 중복 경합은 아직 완전히 닫히지 않은 것으로 보인다.
  - invalid payload -> HTTP 400 `INVALID_REQUEST`
- 결론:
  - 엔진 기동/health/meta/git-sync/log-ready 기본 계약은 살아 있다.
  - agent bootstrap과 HTTP `/` trace 귀속은 정상이다.
  - 다음 실제 blocker는 여전히 `engine-core-private`의 `ParamTypeResolver` 기반 파라미터 타입 FQCN 정규화 반영이다.

## 87) 2026-05-02 수동 기동/E2E 실행 산출물 경로 규칙
- 서버 자체 로그와 TurtlePick agent trace spool, 수동 실행 산출물을 명확히 분리한다.
- `turtlepick-logs/trace-*.log`는 엔진 수거 대상 agent trace spool이며, 서버 자체 로그 정비나 루트 찌꺼기 청소 대상이 아니다.
- `logs/`는 서버 자체 logback 로그 및 수동 검증 산출물을 모으는 경로로 사용한다.
- 수동 서버 기동, smoke, E2E 검증 시 stdout/stderr/pid 같은 실행 산출물은 프로젝트 루트에 생성하지 않는다.
- 필요한 경우 실행 산출물은 반드시 아래 하위 경로로 리다이렉트한다.
  - `logs/runtime/`: 수동 기동 프로세스의 stdout/stderr/pid
  - `logs/smoke/`: 일회성 smoke/E2E 검증 stdout/stderr/pid
- 금지 예:
  - `app-stdout.log`
  - `app-stderr.log`
  - `agent-e2e-run.pid`
  - `agent-e2e*.out.log`
  - `agent-e2e*.err.log`
- 재발 방지 원칙:
  - PowerShell `Start-Process -RedirectStandardOutput/-RedirectStandardError` 사용 시 출력 경로를 루트 파일명으로 두지 않는다.
  - `>`/`2>` 리다이렉션 사용 시에도 `logs/runtime/` 또는 `logs/smoke/` 하위 파일을 명시한다.
  - `.gitignore`는 보조 안전망일 뿐이며, 핵심 규칙은 "루트에 만들지 않기"다.

## 88) 2026-05-02 TurtlePick 추출 검증 테스트베드 초안 보류
- 잼미니가 제안/생성한 TurtlePick 추출 검증용 초안 파일 5개는 사용자 확정 전 독단 생성물로 판단하여 반영하지 않는다.
  - `TurtlePickDummyEvent.java`
  - `TurtlePickTestService.java`
  - `TurtlePickTestController.java`
  - `TurtlePickTestListener.java`
  - `TurtlePickTestRunner.java`
- 실제 확인 결과 위 파일들은 프로젝트 루트에 0바이트 파일로 생성되어 있었고, 사용자 `확정 반영` 지시에 따라 물리 삭제했다.
- 향후 추출 검증 테스트베드는 코드부터 작성하지 않고, 먼저 아래 기준으로 설계안을 확정한 뒤 반영한다.
  - 공통 인프라/AOP/Filter/Interceptor/ExceptionHandler/ControllerAdvice는 추출 대상에서 제외한다.
  - 대상은 공통 작업이 아니라 프로그래머가 직접 작성한 업무적 사이클이다.
  - 현대 Spring Boot 대상서버 검증과 레거시 Spring 검증은 섞지 않는다.
  - 레거시 Spring 검증은 필요 시 별도 대상 서버 프로젝트로 분리한다.
  - 특수 업무 entry는 자동 광역 스캔보다 config의 FQCN 명시(`instrumentation.ast.extra-entry-classes`)를 우선 고려한다.
