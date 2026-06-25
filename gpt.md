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

## 89) 2026-05-03 TurtlePick 대상서버/엔진 문서 및 프로젝트 재분석 메모
- 사용자 요청에 따라 `D:\workspace\turtlepick`의 `README.md`, `gpt.md`, `CLAUDE.md`, `docs/*.md`와 현재 `kjspringweb`의 `gpt.md`, `work_protocol.md`, `CLAUDE.md`, `docs/*.md`를 다시 읽고 실제 코드 구조를 대조했다.
- 이번 턴은 분석 중심이며, 코드/문서 직접 수정은 이 맥락 메모(`gpt.md`) 외에는 수행하지 않았다.
- 현재 `kjspringweb` HEAD는 `6d45d83916e8246239d29085b5af39bd20cb1366`, branch는 `work`다. git status 기준 추적되지 않은 `.claude/`만 보인다.
- 현재 `turtlepick` HEAD는 `dfaa9a4f04be89a0e094f099957f27b7ddb70498`, branch는 `work`다. git status 기준 `.claude/`가 추적되지 않았고, `agent-log-ready-e2e/*`, `server-engine-realtest/*`의 추적 파일 삭제 상태가 보인다.
- `kjspringweb`는 Spring Boot 4.0.2 + Java 17 + Thymeleaf + Security + JPA/H2 + Batch 구조이며, 테스트 목적상 서버/DB 제약과 배치 강제 오류로 관측 지점을 만드는 대상 서버다.
- `kjspringweb/turtlepick-agent-core`는 Java 8 호환 `javaagent` 모듈로, meta handshake, ASM method probe, Spring MVC HTTP context hook, endpoint resolver, trace writer/rolling, log-ready notifier까지 코드상 존재한다.
- agent 설정에는 SQL 계측 on/off 항목이 있지만, 현재 코드 검색 기준 datasource-proxy/MyBatis 실제 계측 구현은 아직 보이지 않는다. 현재 검증된 핵심 경로는 HTTP method trace + file rolling + log-ready 전송이다.
- `turtlepick` 엔진은 `engine-app`, `engine-core-private` 2모듈 구조다. `/api/agent/meta`, `/api/git/sync`, `/api/agent/log-ready`, startup resume 1회 전송, SQLite schema init, Git sync/AST 분석이 구현되어 있다.
- 2026-05-02 엔진 쪽 `ParamTypeResolver` 반영으로 과거 blocker였던 `String,Model` vs `java.lang.String,org.springframework.ui.Model` signature 불일치는 문서와 코드 기준 해결 상태다.
- 2026-05-02 E2E 문서 기준 `/auth/login`은 `endpointResolutionStatus=RESOLVED`로 검증됐고, agent log-ready도 `ACK`까지 확인됐다.
- 현재 남은 1순위 구현 공백은 엔진 `LogReadyService`/`LogFileProcessor`의 실제 파일 I/O다. `checkSourceFileExistsStub`, `readLogFileStub`, `storeDailyTempStub`, `deleteSourceFileStub`가 여전히 stub이다.
- 후속 공백: trace ndjson 파싱/DB 저장/archive/delete/error 이동, 수거 실패 시 LOG_OFF 전환, `/agent/resume` 수신 및 re-meta 경로, branch-agnostic/on-demand commit indexing.
- `kjspringweb`의 `src/test/java/com/example/demo/DemoApplicationTests.java`는 여전히 `com.example.demo` 패키지 잔재로 남아 있어 `gradlew test` 실패 원인 후보가 유지된다.
- 현재 `MemberController` 기본 경로는 `/member`이며 profile/withdraw는 `/member/profile`, `/member/withdraw`다. 과거 문서의 `/profile`, `/withdraw` 단독 표기는 최신 코드와 다르다.

## 90) 2026-05-03 서버 agent trace nodes[] 적재 반영
- 오늘은 기존 GPT 선제안/Claude 딴지 패턴을 반대로 진행했다. Claude가 서버 agent 로그 적재 확장안을 제안하고, GPT가 `methodId`를 node 식별자로 쓰면 안 된다는 딴지를 걸어 `callId/parentCallId/methodId` 구조로 보정했다.
- 반영 범위:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/MethodFrame.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/CompletedNode.java` 신규
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeTraceContext.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
  - `docs/h202605031414.md` 작업 일지 신규
- 핵심 변경:
  - 요청 단위 `RuntimeTraceContext`가 `nextCallId`, `traceStartNanoTime`, 완료 node 목록을 관리한다.
  - `MethodFrame`에 `callId`, `parentCallId`를 추가했다.
  - method exit 시 `CompletedNode(callId,parentCallId,methodId,fqcnMethod,startOffsetMs,endOffsetMs)`를 누적한다.
  - trace JSON에 `nodes[]`를 추가하고, 각 node는 `i`, `p`, `m`, `f`, `st`, `et` 필드로 출력한다.
  - `nodes[]`는 `callId` 오름차순으로 직렬화한다.
  - `error` 전문/args/result/sql 캡처는 이번 단위에서 제외했다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
  - `D:\workspace\kjspringweb`에서 `.\gradlew.bat bootJar` 성공.
  - engine health `http://localhost:8081/api/health`, `http://localhost:8081/api/agent/health` 모두 HTTP 200 UP 확인.
  - 현재 commit `6d45d83916e8246239d29085b5af39bd20cb1366`에 대해 `/api/agent/meta` HTTP 200 `status=OK` 확인.
  - agent 부착 임시 서버를 `localhost:18085`로 기동 후 `GET /board` HTTP 200 확인.
  - `turtlepick-logs/trace-202605031412.log`에서 `/board` trace에 `nodes[]` 확인:
    - node 1: `p=0`, `BoardController#list(...)`
    - node 2: `p=1`, `BoardService#getList(...)`
- 임시 기동 산출물은 `logs/smoke/agent-chain-20260503141242.out.log`, `logs/smoke/agent-chain-20260503141242.err.log`에 저장했다.
- E2E 임시 Java 프로세스는 검증 후 종료했다.
- 다음 논점:
  - 엔진 실파일 수거/파싱 전에 trace `nodes[]` 포맷을 엔진 파서 입력 계약으로 고정할지 검토.
  - 이후 args/result/sql/error 전문 캡처는 별도 섹터로 분리해서 진행한다.

## 91) 2026-05-03 파일 수정 권한 규칙 재확인
- 사용자 최신 지시:
  - `gpt.md`는 Codex가 맥락 유지용으로 권한 요청 없이 자유롭게 수정 가능하다.
  - 그 외 모든 파일은 사용자가 직접 지시하지 않으면 절대 손대지 않는다.
- 이번 턴의 `docs/h202605031414.md` 자동 작성은 사용자 명시 없이 범위를 넘긴 과잉 반영이었다.
- 이후 종료 일지/docs/CLAUDE.md/코드/설정 파일 등은 사용자의 명시 지시 전에는 생성/수정/삭제하지 않는다.
- 단, 사용자가 명시적으로 코드 반영을 지시한 범위의 파일만 수정한다.

## 92) 2026-05-03 로그 적재 사상 재확인
- 사용자 사상 기준으로 프로젝트에 지속적으로 쌓이는 로그 종류는 2개만 둔다.
  1. 서버 자체 범용 로그
     - 사용자의 운영 환경에 따라 각자 커스텀하는 일반 서버 로그.
     - 보통 에러 중심 운영 로그이며, TurtlePick 수거 대상이 아니다.
  2. TurtlePick agent trace 로그
     - 엔진 전달/수거를 위한 TurtlePick 전용 로그.
     - 엔진이 주기적으로 수거하고, 수거 완료 후 TurtlePick이 삭제한다.
- `logs/smoke`, `logs/runtime` 같은 수동 검증 stdout/stderr 산출물은 장기적으로 쌓이는 세 번째 로그 체계가 되면 안 된다.
- 향후 수동 smoke/E2E 검증 시에는 루트나 `logs/` 하위에 검증 산출물을 누적하지 않는 방향으로 처리한다.
- 이미 존재하는 파일 삭제/정리는 사용자 명시 지시가 있을 때만 수행한다.

## 93) 2026-05-03 서버 로그와 TurtlePick 로그 경로 분리 원칙
- 서버 자체 운영 로그와 TurtlePick agent trace 로그는 폴더 경로도 반드시 분리한다.
- 서버 자체 운영 로그:
  - 대상 서버가 자체 운영/장애 확인을 위해 남기는 범용 로그 경로.
  - 고객/사용자 환경에서 직접 커스텀하는 영역이다.
  - TurtlePick 엔진 수거 대상이 아니다.
- TurtlePick agent trace 로그:
  - 대상 서버 쪽에 기록되는 TurtlePick 전용 trace spool 경로가 따로 있어야 한다.
  - 엔진 쪽에도 수거/임시저장/archive/error 등 TurtlePick 전용 로그/저장 경로가 따로 있어야 한다.
  - 이 경로들은 서버 자체 운영 로그 경로와 섞이면 안 된다.
  - 엔진 수거 성공 후 대상 서버 쪽 TurtlePick trace 파일은 TurtlePick이 삭제한다.
- 향후 검증/기동 스크립트 작성 시 stdout/stderr 임시 산출물을 서버 운영 로그 경로나 TurtlePick trace spool 경로에 섞지 않는다.

## 94) 2026-05-03 TurtlePick trace 기본 포맷 정비 기준
- 현재 구현된 trace JSON(`traceId`, `entryFqcnMethod`, `requestUri`, `endpointResolutionStatus`, node 내부 `f=fqcnMethod` 등)은 "로그가 찍힌다"는 확인용 중간물일 뿐, TurtlePick 로그 설계 사상이 반영된 최종 포맷이 아니다.
- 사용자 판단 기준으로 현재 포맷은 운영/엔진 수거용 로그라기보다 초급 디버그 로그에 가까우며, 서버 trace 적재를 계속 진행하기 전에 기본 포맷 정비가 먼저 필요하다.
- 이번 정리는 최종 에러 로그 설계가 아니라, 정상/기본 trace 파일을 TurtlePick 사상에 맞게 최소화하는 기준이다.

### 기본 원칙
- trace 파일은 줄 단위로 완결되는 JSON 레코드 형식을 유지한다. 파일 전체가 하나의 JSON object일 필요는 없으며, 파일이 닫히지 않아도 이미 기록된 줄까지 엔진이 파싱 가능해야 한다.
- 파일 첫 줄은 header 레코드다. `commitHash`, `createdAt`, 포맷 버전, verbose 여부 같은 파일 단위 정보는 여기 한 번만 기록하고 요청 레코드마다 반복하지 않는다.
- 요청 레코드는 엔진 역참조가 가능한 ID 기반 최소 필드만 가진다. 기본 배포 포맷에서는 메서드 풀네임, request URI, endpoint entry key 같은 문자열 디버그 필드를 넣지 않는다.
- node는 배열이 아니라 object로 둔다. 배열(`[1,0,113932304,0,0]`)은 용량은 작지만 필드 추가 시 파서가 깨지기 쉬우므로, 아직 스키마가 확정되지 않은 현재 단계에서는 object가 더 안전하다.
- 정상/에러 파일 분리는 이번 단위에서 하지 않는다. 같은 trace 파일 안에서 요청 레코드의 `e` 플래그로 구분하고, 엔진 파싱/보관 단계에서 error/archive 분기를 다룬다.
- `err` 블록, args/result/sql/stack/source snapshot 등 에러 전문 구조는 이번 기본 포맷 정비 범위 밖이다. 다음 에러 로그 단위에서 별도 설계한다.

### verbose field names 옵션
- 보기 편한 디버깅용 항목명 풀네임 옵션명은 `verbose-field-names`로 정한다.
- 서버 agent 설정명:
  - `turtlepick.agent.logging.verbose-field-names=false`
- Java 필드명 후보:
  - `verboseFieldNames`
- 파일 헤더 축약 필드:
  - `vfn` = verbose field names
- 기본값은 `false`다. 배포/운영 기준은 최소화 포맷이며, verbose는 개발 중 눈으로 확인하기 위한 보조 옵션이다.
- 엔진에는 별도 `verbose-field-names` config를 두지 않는다. 엔진은 파일 헤더의 `vfn` 값을 보고 이후 레코드 파서 모드를 자동 선택한다.
- header는 자기 자신을 설명해야 하므로 항상 short key로 고정한다. 본문 레코드만 `vfn` 값에 따라 short/verbose field name을 선택한다.

### compact 기본 포맷 후보
```json
{"f":"h","v":1,"vfn":false,"c":"6d45d83...","ts":1777788119253}
{"f":"t","ep":1266277122,"e":false,"n":[{"i":1,"p":0,"m":113932304,"st":0,"et":0}]}
```

- header:
  - `f`: record type. `h` = header.
  - `v`: trace file format version.
  - `vfn`: verbose field names 여부.
  - `c`: commit hash.
  - `ts`: file createdAt epoch milliseconds.
- trace:
  - `f`: record type. `t` = trace/request.
  - `ep`: endpoint id.
  - `e`: request context 안에서 어느 method든 `RuntimeMethodBridge.exit(methodId, error=true)`가 한 번이라도 호출되었는지 여부.
  - `n`: completed method node 목록.
- node:
  - `i`: call id.
  - `p`: parent call id. root는 0.
  - `m`: method id.
  - `st`: trace 시작 기준 start offset ms.
  - `et`: trace 시작 기준 end offset ms.

### verbose 본문 포맷 후보
```json
{"f":"h","v":1,"vfn":true,"c":"6d45d83...","ts":1777788119253}
{"format":"trace","endpointId":1266277122,"error":false,"nodes":[{"callId":1,"parentCallId":0,"methodId":113932304,"startOffsetMs":0,"endOffsetMs":0}]}
```

- verbose는 디버깅용일 뿐이며 제품/배포 기준은 `vfn=false` compact 포맷이다.
- `full-name` 계열 명칭은 메서드 FQCN/full method name과 혼동될 수 있으므로 사용하지 않는다.

### `e` 플래그 계약
- `e=true`는 HTTP status 기준이 아니다.
- `e=true`는 root method 기준도 아니다. `@ControllerAdvice`, 내부 catch/retry 등으로 최종 응답이 200/302가 되어도 내부 예외 흔적이 있으면 관측해야 한다.
- 계약:
  - 요청 context 안에서 어느 깊이의 method든 `RuntimeMethodBridge.exit(methodId, error=true)`가 한 번이라도 호출되면 `e=true`.
- 현재 `ignoredError`로 둔 파라미터는 기본 포맷 정비 시 `hasError` 누적으로 되살려야 한다.
- 단, 예외 전파/try-finally 계측 안정화와 `err` 전문 기록은 별도 후속 단위다.

### 파싱 실패 처리
- 개별 파일의 header 파싱 실패, 미지원 `v`, 누락/타입 이상 `vfn` 등은 서버 LOG_OFF 전환 사유가 아니라 파일 단위 실패로 본다.
- 엔진은 해당 파일을 `FAILED` 처리하고 warn 로그를 남긴다. 원본 유지 또는 error-dir 이동은 엔진 파일 I/O/파싱 구현 단위에서 결정한다.
- LOG_OFF는 개별 파일 포맷 문제보다 넓은 구조적 수거 불능 상태에서 별도 판단한다.

## 95) 2026-05-03 trace 기본 포맷 전환 1단위 반영
- 사용자 지시에 따라 1단위로 `TraceLogSerializer + TraceLogWriter` 기본 포맷 전환을 반영했다.
- 반영 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/AgentConfig.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/TurtlepickConfigLoader.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/AgentPremain.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogWriter.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
- `turtlepick.agent.logging.verbose-field-names` 설정을 추가했고 기본값은 `false`다.
- `TraceLogWriter.install(...)`는 단일 시그니처로 정리했다:
  - `loggingDir`
  - `rollingIntervalMinutes`
  - `commitHash`
  - `verboseFieldNames`
  - `LogReadyNotifier`
- `AgentPremain`에서 bootstrap 결과의 `commitHash`와 config의 `verboseFieldNames`를 writer에 전달한다.
- trace 파일이 새로 열릴 때 첫 줄에 header를 쓴다. 기존 파일이 이미 있고 내용이 있으면 append 모드 특성상 헤더를 중복으로 쓰지 않는다.
- header 포맷:
```json
{"f":"h","v":1,"vfn":false,"c":"6d45d83...","ts":1777788119253}
```
- 요청 레코드 compact 포맷:
```json
{"f":"t","ep":1266277122,"e":false,"n":[{"i":1,"p":0,"m":113932304,"st":0,"et":0}]}
```
- 요청 레코드 verbose 포맷:
```json
{"format":"trace","endpointId":1266277122,"error":false,"nodes":[{"callId":1,"parentCallId":0,"methodId":113932304,"startOffsetMs":0,"endOffsetMs":0}]}
```
- 기존 `timestampMs`는 요청 레코드에서 제거했고, `RuntimeMethodBridge`의 `System.currentTimeMillis()` 호출도 제거했다.
- 이번 단위에서 `e`는 `false` 고정이다. `hasError` 누적과 예외 전파/try-finally 안정화는 2단위로 분리한다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.

## 96) 2026-05-03 trace hasError 2단위 반영
- 사용자 `확정 반영` 지시에 따라 trace 요청 레코드의 에러 플래그를 실제 `RuntimeTraceContext` 상태와 연결했다.
- 반영 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeTraceContext.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
- `RuntimeTraceContext`에 `hasError` 필드를 추가하고 `markError()`, `hasError()` 메서드를 추가했다.
- `clear()`에서 `hasError=false` 리셋을 추가했다.
- `RuntimeMethodBridge.exit(int methodId, boolean isError)`에서 stack mismatch 검사를 통과한 뒤 `isError=true`이면 `context.markError()`를 호출한다.
- compact trace의 `e`, verbose trace의 `error`는 더 이상 false 고정이 아니라 `context.hasError()` 실제값을 쓴다.
- 계약:
  - 요청 context 안에서 어느 깊이의 method든 `exit(..., true)`가 한 번이라도 호출되면 요청 레코드는 `e=true`/`error=true`.
  - HTTP status 기준이 아니며 root method 기준도 아니다.
- 이번 단위는 에러 여부 플래그만 연결한다. `err` 블록, stack, args/result/sql, error_call_id, try-finally 계측 안정화는 후속 단위다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.

## 97) 2026-05-03 try/finally 기반 exit 보장 3단위 반영
- 사용자 `확정 반영` 지시에 따라 예외 전파 시 trace stack이 닫히지 않는 문제를 해결하기 위한 3단위를 반영했다.
- 배경:
  - 기존 `AdviceAdapter.onMethodExit(ATHROW)` 방식은 해당 프레임에서 직접 `ATHROW` opcode가 실행될 때만 `exit(..., true)`가 호출된다.
  - 하위 메서드에서 올라온 예외가 프레임을 관통하는 경우 root frame이 닫히지 않아 `context.isEmpty()`가 되지 않고 trace flush가 누락됐다.
- 반영 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/instrument/MethodProbeAdviceAdapter.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
- `MethodProbeAdviceAdapter` 변경:
  - 필드 레벨 `startLabel`, `endLabel`, `handlerLabel`을 추가했다.
  - `visitCode()`에서 try-catch block을 먼저 등록하고 `super.visitCode()`를 호출한다.
  - `onMethodEnter()`는 `enter(methodId, fqcnMethod)` 호출 후 `startLabel`을 찍어 `enter()`를 try 범위 밖에 둔다.
  - `onMethodExit()`는 `opcode == ATHROW`일 때 아무것도 하지 않아 double-exit을 방지한다.
  - 정상 return 계열에서만 `exit(methodId, false)`를 호출한다.
  - catch handler에서는 스택의 예외를 `newLocal(Throwable)`로 확보한 local slot에 저장한 뒤 `exit(methodId, true)`를 호출하고, 예외를 다시 로드해 `ATHROW`로 재전파한다.
- `RuntimeMethodBridge` 변경:
  - public `exit(int methodId, boolean isError)`를 throw-free wrapper로 만들고 기존 로직은 `exitUnsafe(...)`로 분리했다.
  - bridge 내부 실패 시 warn 로그 후 `TraceContextHolder.clear()`로 정리한다.
- 전제:
  - `ApplicationMethodTransformer`는 이미 `ClassWriter.COMPUTE_FRAMES`와 `ClassReader.EXPAND_FRAMES`를 사용 중이라 catch block 추가에 필요한 frame 재계산 전제는 충족되어 있다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
  - 새 agent jar를 붙인 서버 재기동 후 `error:true` trace 검증은 아직 미실행이다.

## 98) 2026-05-03 예외 기본 메타 4단위 반영
- 사용자 `확정 반영` 지시에 따라 에러 trace 레코드에 stack trace 전 단계의 기본 예외 메타를 추가했다.
- 반영 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/instrument/MethodProbeAdviceAdapter.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeTraceContext.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
- `MethodProbeAdviceAdapter`:
  - `EXIT_THROWABLE_DESC = (int, Throwable)void` 시그니처를 추가했다.
  - catch handler에서 예외 local을 `exit(methodId, Throwable)`로 전달한다.
- `RuntimeMethodBridge`:
  - `exit(int methodId, Throwable throwable)` overload를 추가했다.
  - Throwable은 context에 오래 보관하지 않고 class name/message 문자열로 변환해 저장한다.
- `RuntimeTraceContext`:
  - `errorCallId`, `exceptionClass`, `exceptionMessage` 필드를 추가했다.
  - `markError(int callId, String exceptionClass, String exceptionMessage)`는 first-write-wins로 동작한다. 예외 전파 과정에서 service/controller/root frame이 뒤늦게 들어와도 최초 깊은 frame의 에러 메타를 덮지 않는다.
  - `clear()`에서 에러 메타를 리셋한다.
- `TraceLogSerializer`:
  - compact 에러 필드: `eci`, `ec`, `em`
  - verbose 에러 필드: `errorCallId`, `exceptionClass`, `exceptionMessage`
  - `e=false`/`error=false` 또는 `errorCallId`가 없으면 에러 메타 필드는 출력하지 않는다.
- 이번 단위는 stack trace, args/result/sql, source snapshot을 포함하지 않는다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
  - 새 agent jar를 붙인 서버 재기동 후 런타임 trace 검증은 아직 미실행이다.

## 99) 2026-05-03 Repository/DAO node 및 args 캡처 TODO
- 오늘은 서버 agent trace 포맷/에러 플래그/예외 기본 메타까지 서버 쪽을 우선 진행한다.
- DB duplicate 테스트 결과:
  - `POST /auth/join`에 기존 `user1@kjweb.com`으로 요청 시 `DataIntegrityViolationException`이 trace에 기록됐다.
  - 현재 trace는 Controller -> Service node까지만 있고, Repository/DAO node는 없다.
- 현재 JPA 흐름:
  - `AuthController.join()`
  - `MemberService.join()`
  - `memberRepository.save(member)`
  - Hibernate insert
  - `DataIntegrityViolationException`
- DAO/Repository node가 필요한 이유:
  - Service 안에서 파라미터를 바꿔 같은 DAO를 여러 번 호출할 수 있다.
  - Service node만 있으면 어떤 Repository 호출이 어떤 파라미터로 실패했는지 추적할 수 없다.
  - SQL 전문을 바로 찍지 않더라도 Repository/DAO method node와 args는 필요하다.
- 현재 서버 agent만으로 Repository node를 바로 추가할 수 없는 이유:
  - `MemberRepository#save(Member)`는 git 소스에 직접 선언된 메서드가 아니라 Spring Data inherited method다.
  - 기존 엔진 AST 기반 methodId 발번 체계에는 inherited Repository methodId가 없다.
  - 서버가 임시 해시를 자체 생성하면 엔진 DB/method registry와 불일치한다.
- Repository/DAO node 작업은 엔진 계약 선행 후 서버로 돌아와야 한다.

### 엔진/서버 계약 TODO
- meta request에 `repositories[]` 추가:
```json
{
  "owner": "com.kjweb.domain.repository.MemberRepository",
  "domainType": "com.kjweb.domain.entity.Member",
  "idType": "java.lang.Long"
}
```
- 엔진은 `repositories[]` 기반으로 inherited methodId를 발번하고 DB에 저장한다.
- methodId hash input 후보:
```text
REPOSITORY_INHERITED|{owner}#{method}({semanticParams})
```
- owner는 `CrudRepository`가 아니라 실제 Repository 인터페이스 FQCN이어야 한다.
- `inheritedFrom`은 별도 메타 필드로 기록한다.
- meta response에는 기존 `methods[]`와 별도로 `repositoryMethods[]`를 추가한다.
- 서버 agent는 `repositoryMethods[]`를 별도 registry에 적재한다.
- runtime matching key 후보:
```text
(owner, methodName, runtimeParams) -> methodId
```

### Repository inherited MVP 후보
| method | semantic params | runtimeParams |
|---|---|---|
| save | `[domainType]` | `[java.lang.Object]` |
| saveAll | `[java.lang.Iterable]` | `[java.lang.Iterable]` |
| findById | `[idType]` | `[java.lang.Object]` |
| findAll | `[]` | `[]` |
| delete | `[domainType]` | `[java.lang.Object]` |
| deleteById | `[idType]` | `[java.lang.Object]` |
| existsById | `[idType]` | `[java.lang.Object]` |
| count | `[]` | `[]` |

### 서버 후속 TODO
- 엔진에서 `repositoryMethods[]` 계약이 완료되면 서버 agent로 돌아와 Repository AOP를 구현한다.
- Repository AOP는 Spring proxy 계층에서 Repository 호출을 감싸 node를 추가한다.
- 그 다음 단위로 DAO/Repository args 캡처를 추가한다.
- Repository bean은 proxy일 수 있으므로 owner/domainType/idType 추출 시 실제 Repository 인터페이스를 찾아야 한다.
  - 후보 API: `AopUtils.getTargetClass(bean)`, `ClassUtils.getAllInterfacesForClass(bean.getClass())`
  - owner는 proxy class명이 아니라 실제 Repository 인터페이스 FQCN이어야 한다.
- `REPOSITORY_DECLARED` 사용자 정의 쿼리 메서드는 이번 TODO 범위 밖이며, 기존 scanner 포함 여부도 별도 확인 대상이다.

## 100) 2026-05-03 예외 핵심 메타 정제 5단위 반영
- 사용자 `확정 반영` 지시에 따라 에러 trace의 `exceptionMessage` 과다 출력 문제를 줄이고, 실제 사용자 코드 위치를 추출하는 5단위를 반영했다.
- 목표:
  - raw stack trace 20줄을 그대로 찍지 않는다.
  - 예외 종류별 하드코딩 없이 공통 추출 규칙으로 핵심만 남긴다.
  - outer exception, root cause, 사용자 패키지 frame을 분리한다.
- 신규 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/UserFrame.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/ErrorMeta.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/ErrorMetaExtractor.java`
- 변경 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/AgentConfig.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/TurtlepickConfigLoader.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/AgentPremain.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeTraceContext.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
  - `turtlepick.properties`
- 설정:
```properties
turtlepick.agent.error.user-frame-packages=com.kjweb
```
- `ErrorMetaExtractor` 동작:
  - `exceptionMessage`, `rootExceptionMessage`는 500자 고정 truncate.
  - cause chain은 outer -> root로 만든 뒤 user frame 수집은 root -> outer 순서로 수행한다.
  - `user-frame-packages`에 포함된 사용자 패키지 frame만 수집한다.
  - `(className, methodName, lineNumber)` 기준으로 중복 제거한다.
  - user frame은 최대 10개만 저장한다.
- trace 출력:
  - compact:
    - 기존 `eci/ec/em`
    - root가 outer와 다를 때만 `rc/rm`
    - user frame이 있을 때만 `uf`
  - verbose:
    - 기존 `errorCallId/exceptionClass/exceptionMessage`
    - root가 outer와 다를 때만 `rootExceptionClass/rootExceptionMessage`
    - user frame이 있을 때만 `userFrames`
- `RuntimeTraceContext.markError(callId, ErrorMeta)`는 기존처럼 first-write-wins를 유지한다.
  - 예외가 Service -> Controller 방향으로 전파되어도 최초 깊은 frame의 `errorCallId`와 에러 메타를 덮지 않는다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
- 미검증:
  - 새 jar로 서버 재기동 후 duplicate DB 에러에서 `rootExceptionClass/rootExceptionMessage/userFrames`가 실제 trace에 찍히는지 런타임 확인 필요.

## 101) 2026-05-03 5단위 에러 trace 런타임 검증
- 새 agent jar가 붙은 서버에서 에러 요청을 여러 개 보냈다.
- 정상 요청:
  - `GET /`
  - `GET /board`
- 에러 요청:
  - `GET /board/999999999`
  - `GET /board?page=-1`
  - CSRF 포함 `POST /auth/join` duplicate member (`user1@kjweb.com`)
- 확인 파일:
  - `turtlepick-logs/trace-202605032018.log`
  - `turtlepick-logs/trace-202605032019.log`
- 확인 결과:
  - `CustomAppException` 케이스에서 `userFrames`가 출력됐다.
    - `BoardService#getDetail`
    - `BoardController#detail`
  - `IllegalArgumentException` 케이스에서 `userFrames`가 출력됐다.
    - `BoardController#list`
  - DB duplicate 케이스에서 `exceptionMessage`가 500자 truncate됐다.
  - DB duplicate 케이스에서 root cause가 분리 출력됐다.
    - outer: `org.springframework.dao.DataIntegrityViolationException`
    - root: `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException`
  - DB duplicate 케이스에서 `userFrames`가 출력됐다.
    - `MemberService#join`
    - `AuthController#join`
- admin API null batch는 인증/CSRF 문제로 403이 떠서 이번 trace 검증 대상에서 제외했다.
- 현재 5단위 목표였던 `em truncate + root cause + userFrames`는 런타임 trace에서 확인됐다.

## 102) 2026-05-03 userFrames 프록시/비소스 frame 제거
- 사용자 `확정 반영` 지시에 따라 `userFrames`에서 Spring CGLIB proxy frame과 source line이 없는 frame을 제외했다.
- 배경:
  - duplicate DB 에러 trace에서 아래 frame이 출력됐다.
```json
{"className":"com.kjweb.web.service.MemberService$$SpringCGLIB$$0","methodName":"join","lineNumber":-1}
```
  - `com.kjweb`로 시작해서 기존 패키지 필터를 통과했지만 실제 사용자 소스 위치가 아니라 노이즈다.
- 반영 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/ErrorMetaExtractor.java`
- 반영 정책:
  - `className`에 `$$`가 포함되면 제외한다.
  - `lineNumber < 0`이면 제외한다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
- 미검증:
  - 새 jar로 서버 재기동 후 duplicate DB 에러에서 `MemberService$$SpringCGLIB$$0`가 빠지는지 런타임 재확인 필요.

## 103) 2026-05-03 userFrames 프록시 제거 런타임 검증
- 새 agent jar로 서버 재기동 후 CSRF 포함 duplicate join 요청을 보냈다.
- 요청:
  - `POST /auth/join`
  - `username=user1`
  - `email=user1@kjweb.com`
- 확인 파일:
  - `turtlepick-logs/trace-202605032027.log`
- 확인 결과:
  - `userFrames`에서 `MemberService$$SpringCGLIB$$0`가 제거됐다.
  - `lineNumber:-1` frame도 출력되지 않았다.
  - 남은 userFrames:
```json
[
  {"className":"com.kjweb.web.service.MemberService","methodName":"join","lineNumber":47},
  {"className":"com.kjweb.web.controller.AuthController","methodName":"join","lineNumber":35}
]
```

## 104) 2026-05-03 Unit 6 에러 지점 args 캡처 반영
- 사용자 `확정 반영` 지시에 따라 에러 경로에서만 method parameter args를 캡처하는 Unit 6을 반영했다.
- 서버 agent 원칙:
  - 서버 agent는 민감정보 암호화/마스킹/난독화/압축을 하지 않는다.
  - 엔진이 trace 파일을 수거/해석한 뒤 저장 직전 단계에서 암호화/마스킹/난독화/압축을 처리한다.
  - 서버 agent는 원본 관측성 보존을 우선하되, 위험 타입 exclude와 길이 상한으로 사고성 로그 폭주만 방지한다.
- 신규 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/ErrorArgCaptureOptions.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/ErrorArgExtractor.java`
- 변경 파일:
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/instrument/MethodProbeAdviceAdapter.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeMethodBridge.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/RuntimeTraceContext.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/trace/TraceLogSerializer.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/AgentConfig.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/config/TurtlepickConfigLoader.java`
  - `turtlepick-agent-core/src/main/java/com/turtlepick/agent/core/AgentPremain.java`
  - `turtlepick.properties`
- ASM:
  - catch handler에서 `loadArgArray()`로 원본 method args를 `Object[]`로 만든다.
  - `RuntimeMethodBridge.exit(int, Throwable, Object[])`로 전달한다.
  - 기존 `exit(int, Throwable)`와 `EXIT_THROWABLE_DESC`는 제거했다.
- Runtime:
  - `Object[]` 원본은 context에 저장하지 않는다.
  - `ErrorArgExtractor`가 즉시 `String[]` snapshot으로 변환한다.
  - `RuntimeTraceContext`는 `String[] errorArgs`만 방어 복사해 first-write-wins로 저장한다.
- serializer:
  - compact: `ea`
  - verbose: `errorArgs`
  - args가 null/empty면 필드를 생략한다.
- config:
```properties
turtlepick.agent.error.args.enabled=true
turtlepick.agent.error.args.max-length=10000
turtlepick.agent.error.args.exclude-classes=java.io.InputStream,java.io.Reader,java.io.File,java.nio.ByteBuffer,org.springframework.web.multipart.MultipartFile,byte[],char[]
```
- `max-length=0`은 무제한이다.
- `enabled=false`면 args 캡처를 끈다.
- 기본 exclude 목록은 `AgentConfig.defaultErrorArgsExcludeClasses()`에서 제공한다.
  - `byte[]` -> `[B`
  - `char[]` -> `[C`
  - alias 변환은 `TurtlepickConfigLoader`에서 수행한다.
- 후속 TODO:
  - Kafka/Batch/ETL/대용량 동기 인터페이스는 entry-type 또는 method rule 기반 별도 args 정책을 설계한다.
  - 일반 HTTP/service args는 기본적으로 원본 보존 방향을 유지한다.
- 검증:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
- 미검증:
  - 서버 재기동 후 verbose `errorArgs` 출력 확인 필요.
  - `verbose-field-names=false`로 compact `ea` 출력 확인 필요.
  - duplicate join 에러에서 DTO arg 캡처 확인 필요.

## 105) 2026-05-03 Unit 6 verbose errorArgs 런타임 검증
- 새 agent jar로 서버 재기동 후 에러 요청을 보냈다.
- 요청:
  - `GET /`
  - `GET /board/999999999`
  - `GET /board?page=-1`
  - CSRF 포함 `POST /auth/join` duplicate member (`user1@kjweb.com`)
- 확인 파일:
  - `turtlepick-logs/trace-202605032056.log`
- 확인 결과:
  - `GET /board/999999999`
    - `errorArgs:["999999999"]`
  - `GET /board?page=-1`
    - `errorArgs:["-1","null","{}"]`
    - primitive `int page` boxing과 null arg 캡처가 확인됐다.
  - duplicate join
    - `errorArgs:["com.kjweb.web.dto.MemberJoinDto@684cc1b0"]`
    - DTO arg가 safe `toString()`으로 snapshot 저장됐다.
  - CGLIB proxy frame 제거는 유지됐다.
- 미검증:
  - `turtlepick.agent.logging.verbose-field-names=false` 설정 후 compact `ea` 출력 확인 필요.
  - exclude 대상 arg placeholder(`<excluded: className>`) 출력은 실제 대상 endpoint가 없어 후속 검증 대상이다.

## 106) 2026-05-03 Unit 6 compact ea 런타임 검증
- `turtlepick.agent.logging.verbose-field-names=false`로 변경 후 서버를 재기동하고 에러 요청을 보냈다.
- 확인 파일:
  - `turtlepick-logs/trace-202605032100.log`
- header:
```json
{"f":"h","v":1,"vfn":false,"c":"6d45d83916e8246239d29085b5af39bd20cb1366","ts":1777809620063}
```
- 확인 결과:
  - `GET /board/999999999`
    - compact `ea:["999999999"]`
  - `GET /board?page=-1`
    - compact `ea:["-1","null","{}"]`
  - duplicate join
    - compact `ea:["com.kjweb.web.dto.MemberJoinDto@2cb5f7a"]`
  - compact 에러 필드 `eci/ec/em/rc/rm/uf/ea`가 정상 출력됐다.
- Unit 6 필수 런타임 검증 완료:
  - verbose `errorArgs` 확인 완료.
  - compact `ea` 확인 완료.
- 후속 미검증:
  - exclude 대상 arg placeholder(`<excluded: className>`) 출력은 별도 대상 endpoint가 없어 후속 검증 대상이다.

## 107) 2026-05-03 Unit 6 exclude placeholder 런타임 검증
- 임시로 `turtlepick.agent.error.args.exclude-classes`를 아래 값으로 변경해 exclude placeholder를 검증했다.
```properties
turtlepick.agent.error.args.exclude-classes=java.lang.Long,java.lang.Integer,com.kjweb.web.dto.MemberJoinDto
```
- 서버 재기동 후 요청:
  - `GET /board/999999999`
  - `GET /board?page=-1`
  - CSRF 포함 `POST /auth/join` duplicate member (`user1@kjweb.com`)
- 확인 파일:
  - `turtlepick-logs/trace-202605032105.log`
- 확인 결과:
```json
"ea":["<excluded: java.lang.Long>"]
```
```json
"ea":["<excluded: java.lang.Integer>","null","{}"]
```
```json
"ea":["<excluded: com.kjweb.web.dto.MemberJoinDto>"]
```
- 검증 후 `turtlepick.properties`의 exclude 목록은 원래 운영 후보 값으로 되돌렸다.
```properties
turtlepick.agent.error.args.exclude-classes=java.io.InputStream,java.io.Reader,java.io.File,java.nio.ByteBuffer,org.springframework.web.multipart.MultipartFile,byte[],char[]
```
- Unit 6 런타임 검증 상태:
  - verbose `errorArgs` 확인 완료.
  - compact `ea` 확인 완료.
  - exclude placeholder 확인 완료.

## 108) 2026-05-04 h202605031414 하루 총괄 일지 정리
- `docs/h202605031414.md`를 2026-05-03 하루 총괄 일지로 교체했다.
- 기존 문서는 Unit 1(nodes[] 적재) 중심이라 하루 작업 흐름을 복원하기 부족했다.
- 새 문서 구조:
  - Unit 1~6 전체 흐름
  - 각 Unit이 왜 필요했는지 인과관계
  - trace v1 header/compact/verbose 계약
  - 실제 검증 파일명과 핵심 출력
  - 서버 로그와 TurtlePick trace 로그 구분
  - 남은 TODO: log-ready 실패 시 LOG_OFF, engine resume, `/agent/resume` 등록, Repository/DAO node, Kafka/Batch/ETL 대용량 파라미터 정책
- Unit 6 상세는 중복을 피하기 위해 요약만 남겼고, 상세 맥락은 현재 `gpt.md` 102~107번 기록을 기준으로 둔다.

## 109) 2026-05-05 TurtlePick/대상서버 문서 및 코드 재분석 메모
- 사용자 요청에 따라 `D:\workspace\turtlepick`의 `README.md`, `gpt.md`, `CLAUDE.md`, `docs/*.md`와 현재 프로젝트 `kjspringweb`의 `gpt.md`, `work_protocol.md`, `docs/*.md`를 다시 읽고 실제 코드 상태를 대조했다.
- 양쪽 저장소 모두 `work` 브랜치이며, `git status` 기준 추적되지 않은 `.claude/`만 보인다.
- 현재 프로젝트 작업 모드는 계속 "제안/분석만"이다. `검토 완료. 확정. 수정해` 또는 `확정. 반영.` 전에는 `gpt.md` 외 코드/문서 수정 금지 원칙을 유지한다.
- `turtlepick` 엔진 실제 상태:
  - `/api/agent/meta`는 기존 `methods[] + endpoints[]` 계약까지만 구현되어 있다.
  - `AgentMetaRequest`에는 아직 `repositories[]`가 없고, `AgentMetaResponse`에는 아직 `repositoryMethods[]`가 없다.
  - `method_def` 스키마는 `method_id`, `commit_hash`, `fqcn_method`만 있어 `REPOSITORY_INHERITED` 저장에 필요한 `method_kind/owner/runtime_params/domain_type/id_type/inherited_from` 계열 저장 구조가 아직 없다.
  - `LogReadyService.checkSourceFileExistsStub`, `LogFileProcessor.read/store/delete*Stub`는 여전히 stub이다.
- `kjspringweb` 서버 agent 실제 상태:
  - trace v1 header/compact/verbose, method call tree, error flag, root cause/userFrames, errorArgs, log-ready notifier까지 구현되어 있다.
  - `MetaRequest/MetaResponse/MetaJsonCodec/MethodMappingRegistry`는 일반 method/endpoint만 처리하고, repository meta request/response/registry는 아직 없다.
  - Spring `ApplicationContext` 접근 경로와 `/agent/resume` 수신 경로는 아직 없다.
- 대상 서버 Repository 구조:
  - `BoardRepository`, `MemberRepository`, `MenuRepository`, `RoleRepository` 모두 `JpaRepository<..., Long>` 기반이다.
  - 현재 서버 trace에서 Controller -> Service는 잡히지만 inherited Repository 호출(`save`, `findById`, `delete`, `count` 등)은 methodId 계약 부재로 node화되지 않는다.
- 다음 우선순위 판단:
  1. Repository inherited node를 먼저 하려면 `docs/작업지시문_20260503.md`의 meta 계약 확장이 선행이다.
  2. 엔진 수거/저장을 먼저 하려면 `LogReadyService`/`LogFileProcessor` stub을 실제 파일 I/O와 trace v1 parser로 교체하는 것이 선행이다.
  3. LOG_OFF/resume 정책은 `/agent/resume` 수신 경로와 함께 잡아야 한다. 현재 log-ready 실패 시 즉시 LOG_OFF를 적용하면 복구 경로가 없어 장시간 정지될 수 있다.

## 110) 2026-05-05 서버 agent 우선순위 및 resume 설계 보류 TODO
- 사용자 최신 결정:
  - 엔진 쪽으로 넘어가기 전 서버 agent에서 할 수 있는 것을 먼저 끝낸다.
  - 우선 작업은 **엔진 죽었을 시 서버 trace 로그 기록 중단 로직**이다.
  - `/agent/resume` 수신 기반 설계는 일단 보류하고 TODO로만 남긴다.
- 다음 최우선 서버 작업:
  - `LogReadyNotifier`가 `/api/agent/log-ready` 전송 1회 + 1초 후 1회 재시도까지 실패하면 `AgentState=LOG_OFF`로 전환한다.
  - `LOG_OFF` 상태에서는 신규 trace 기록/flush/write가 중단되어야 한다.
  - 이미 닫힌 trace 파일은 서버가 삭제하지 않는다. 엔진 수거 권한/정책으로 처리한다.
  - LOG_OFF 전환은 조용히 삼키지 말고 warn 로그로 남긴다.
- 확인 필요:
  - 현재 `AgentStateHolder`는 `AgentPremain` 지역 조립 객체 성격이 강하므로, `LogReadyNotifier`, `RuntimeMethodBridge`, `TraceLogWriter`가 같은 상태를 볼 수 있게 런타임 공유 경로가 필요하다.
  - LOG_OFF 시 어느 지점에서 중단할지 결정해야 한다.
    - 후보 1: `RuntimeMethodBridge.enter()`에서 state 확인 후 context 생성 자체를 막는다.
    - 후보 2: root flush/write 직전 state 확인.
    - 후보 3: 둘 다 적용하여 신규 context와 파일 write를 모두 차단.
- `/agent/resume` 보류 설계 메모:
  - Boot 전용 `ApplicationContextInitializer`/`spring.factories` 경로는 baseline에서 제외한다. 레거시 Spring/WAR/외장 Tomcat 호환을 위해 기존 Servlet/Spring MVC hook 계층을 우선한다.
  - 현재 서버 agent는 `DispatcherServlet#doDispatch(javax/jakarta request,response)`를 이미 ASM으로 후킹하고 있으므로, 새 hook 추가보다 기존 HTTP hook 확장이 우선 후보다.
  - 현재 bridge는 request만 받으므로 `/agent/resume` 직접 응답을 위해서는 request/response 둘 다 받는 얇은 stable ABI bridge가 필요하다.
  - `AgentHttpBridge.safeEnterOrHandle(Object req, Object res): boolean` 같은 keep 대상 ABI를 두고, `/agent/resume`이면 내부 router로 위임 후 `doDispatch` 본문을 skip하는 방향이 유력하다.
  - stable ABI bridge에는 URI 분기만 두고, 실제 resume 처리(commitHash 비교, state 전환, JSON response write)는 난독화 가능 내부 클래스(`AgentInternalRouter`, `ResumeHandler` 등)로 분리한다.
  - `javax/jakarta`는 컴파일 import 없이 ASM descriptor 문자열 매칭 + bridge `Object`/reflection 방식으로 유지한다.
  - resume 응답은 HTTP status 분기보다 HTTP 200 + body state/reason 패턴을 우선한다.
  - 현재 엔진 startup resume body에는 `commitHash`가 없으므로 서버는 `commitHash` optional로 처리한다.
    - `commitHash` 없음: 구버전/현행 엔진 호환으로 `LOG_ON`.
    - `commitHash` 있음 + 일치: `LOG_ON`.
    - `commitHash` 있음 + 불일치: `LOG_OFF`, reason=`COMMIT_MISMATCH`, `serverCommitHash` 응답.
  - 내부 처리 예외는 앱 흐름으로 전파하지 않고 agent 응답 body/log로 정리한다.

## 111) 2026-05-05 오늘 작업 핑퐁 방식 변경
- 사용자 최신 지시:
  - 오늘 작업은 Claude가 먼저 상세안을 제안한다.
  - GPT는 그 상세안에 대해 딴지/보완/리스크 검토를 한다.
  - GPT-Claude 핑퐁으로 합의안을 만든다.
  - Claude가 최종 반영하고 테스트한다.
  - GPT는 반영 결과와 테스트 결과를 검증하는 역할을 맡는다.
- 오늘 한정 운영 방식으로 기록한다.
- 기존 `work_protocol.md`의 기본 역할 분담과 다를 수 있으나, 사용자 최신 지시가 우선한다.

## 112) 2026-05-05 `/agent/resume` 최종 설계 합의 메모
- `/agent/resume` 수신은 Boot 전용 controller/initializer가 아니라 기존 `DispatcherServlet#doDispatch` ASM hook 확장으로 간다.
- 신규 stable ABI 후보:
  - `AgentHttpBridge.install(AgentStateHolder, String commitHash, LogReadyNotifier)`
  - `AgentHttpBridge.safeEnterOrHandle(Object request, Object response): boolean`
- stable ABI bridge는 얇게 유지한다.
  - 내부 요청 여부 확인.
  - `/agent/resume`이면 내부 router에 위임 후 `true` 반환하여 `doDispatch` 본문을 skip.
  - 일반 요청이면 기존 `HttpRequestContextBridge.safeEnter(request)` 후 `false`.
- 실제 처리 로직은 난독화 가능 내부 클래스로 분리한다.
  - `AgentInternalRouter`
  - `ResumeHandler`
- URI 판정은 `endsWith("/agent/resume")` 금지.
  - `getContextPath()`를 reflection으로 읽고 context path를 제거한 뒤 정확히 `/agent/resume`과 비교한다.
  - `/api/agent/resume`, `/foo/agent/resume` 같은 애플리케이션 endpoint 오인 hijack을 막는다.
- resume body:
  - `command`는 `RESUME_LOGGING`만 허용.
  - `commitHash`는 optional이다.
  - hash 없음: 현행 엔진 호환으로 `LOG_ON`.
  - hash 일치: `LOG_ON`.
  - hash 불일치: `LOG_OFF`, reason=`COMMIT_MISMATCH`.
- 응답은 meta API 패턴과 맞춰 HTTP 200 + JSON body를 사용한다.
  - 성공: `{"state":"LOG_ON","serverCommitHash":"..."}`
  - 거절: `{"state":"LOG_OFF","reason":"...","serverCommitHash":"..."}`
- 내부 요청으로 확정된 뒤 처리 예외가 나면 앱으로 넘기지 않는다.
  - `RESUME_HANDLE_FAILED` JSON 응답을 시도하고 `LOG_OFF`로 둔다.
  - fatal(`VirtualMachineError`, `ThreadDeath`)만 재전파한다.
- `LogReadyNotifier`는 resume 후 worker 재시작이 가능해야 한다.
  - `start()`는 `worker != null && worker.isAlive()`일 때만 return.
  - `runLoop()` finally에서 `clearWorkerIfCurrent(Thread.currentThread())`로 죽은 worker 참조를 제거한다.
  - `onClosed()`는 enqueue 성공 후 `start()`를 호출하여 resume race로 worker가 살아나지 못한 경우에도 다음 rolling 시점에 복구한다.
- `DispatcherServletDoDispatchAdapter.visitCode()`는 `AgentHttpBridge.safeEnterOrHandle(req,res)` 호출 결과가 true면 `RETURN`, false면 기존 흐름을 이어간다.
- 현재 상태:
  - 최종 설계 딴지는 닫혔다.
  - 사용자가 반영 지시를 내리면 Claude가 구현/테스트하고 GPT가 검증한다.

## 113) 2026-05-05 `/agent/resume` 반영 후 GPT 실검증 결과
- Claude가 `/agent/resume` 설계를 반영했고, GPT가 코드/빌드/런타임을 검증했다.
- 반영 파일:
  - 신규: `AgentHttpBridge.java`, `AgentInternalRouter.java`, `ResumeHandler.java`
  - 변경: `LogReadyNotifier.java`, `DispatcherServletDoDispatchAdapter.java`, `AgentPremain.java`
- 정적 검증:
  - `AgentHttpBridge.install(...)`, `safeEnterOrHandle(...)` 존재 확인.
  - `AgentInternalRouter.isInternalRequest()`가 `getContextPath()` 기반 정규화 후 `/agent/resume` 정확 비교하는 것 확인.
  - `ResumeHandler`가 `RESUME_LOGGING`, optional `commitHash`, `COMMIT_MISMATCH`, `LOG_ON/LOG_OFF` 응답을 처리하는 것 확인.
  - `LogReadyNotifier.onClosed()` enqueue 성공 후 `start()` 호출 확인.
  - `DispatcherServletDoDispatchAdapter`가 `safeEnterOrHandle(req,res)` 결과 true면 `RETURN` 삽입 확인.
  - `AgentPremain`에서 `AgentHttpBridge.install(stateHolder, commitHash, logReadyNotifier)` 확인.
- 빌드:
  - `D:\workspace\kjspringweb\turtlepick-agent-core`에서 `..\gradlew.bat shadowJar` 성공.
- 런타임 조건:
  - 엔진 `http://localhost:8081/api/health` 정상 `UP`.
  - 대상 서버 `8080`은 검증 시작 시 리슨 중이 아니어서 GPT가 새 agent jar를 붙여 테스트용으로 기동했다.
- 핵심 실검증 실패:
  - unauthenticated `POST http://localhost:8080/agent/resume`는 agent JSON이 아니라 Spring Security redirect를 탔다.
  - auto redirect를 끄고 확인한 1차 응답:
    - HTTP `302`
    - `Location: http://localhost:8080/auth/login;jsessionid=...`
  - redirect를 따라가면 HTTP 200 로그인 HTML이 반환된다.
  - 기대했던 `{"state":"LOG_ON",...}` / `{"state":"LOG_OFF",...}` 응답은 나오지 않았다.
- 원인 판단:
  - 현재 hook 위치가 `DispatcherServlet#doDispatch`라서 Spring Security filter chain 이후에만 실행된다.
  - `SecurityConfig`는 `/agent/resume`을 permitAll로 열지 않고 `.anyRequest().authenticated()`로 묶고 있다.
  - 따라서 `/agent/resume` 원 요청은 `DispatcherServlet`에 도달하기 전에 security filter에서 `/auth/login`으로 리다이렉트된다.
  - trace 파일에는 `/agent/resume` 자체가 아니라 리다이렉트된 login endpoint trace만 남았다.
- 결론:
  - 이번 구현은 "보안 필터가 없는/permitAll인 앱"에서는 동작 가능성이 있지만, 실제 대상 서버 기준으로는 engine startup resume 수신 요구를 만족하지 못한다.
  - `/agent/resume` 내부 endpoint는 `DispatcherServlet`보다 앞선 Servlet filter chain 계층에서 선점해야 한다.
- 다음 보정 방향 후보:
  - 기존 `DispatcherServlet` hook은 일반 HTTP context 수집용으로 유지한다.
  - 내부 endpoint만 처리하는 별도 filter-chain hook을 추가한다.
  - Tomcat 기준 후보: `org.apache.catalina.core.ApplicationFilterChain#doFilter(ServletRequest, ServletResponse)`.
  - javax/jakarta 양쪽 descriptor를 지원한다.
  - filter-chain hook에서는 일반 요청에 `HttpRequestContextBridge.safeEnter()`를 호출하지 말고, `/agent/resume` internal 요청이면 응답 후 `RETURN`, 아니면 그대로 filter chain을 계속 진행한다.
  - 이를 위해 `AgentHttpBridge.safeHandleInternalOnly(Object req, Object res): boolean` 같은 별도 얇은 ABI가 필요하다.
- 미검증:
  - 현재 `/agent/resume`이 실제로 agent까지 도달하지 못하므로 invalid command, commit mismatch, LOG_OFF 후 resume worker 재시작은 런타임 검증하지 못했다.

## 114) 2026-05-05 `/agent/resume` Tomcat filter-chain 보정 후 GPT 검증
- 113번 실패 원인(Spring Security filter chain이 `DispatcherServlet`보다 앞에서 `/agent/resume`을 302로 차단)을 해결하기 위해 Tomcat filter-chain 선점 hook이 추가됐다.
- 반영 파일:
  - 신규: `TomcatFilterChainInterceptTransformer.java`
  - 신규: `TomcatFilterChainClassVisitor.java`
  - 신규: `TomcatFilterChainDoFilterAdapter.java`
  - 변경: `AgentHttpBridge.java`
  - 변경: `AgentInternalRouter.java`
  - 변경: `AgentPremain.java`
- GPT 정적 확인:
  - `AgentInternalRouter.install()`은 `serverCommitHash`, `logReadyNotifier`, `stateHolder` 순서로 대입하고, installed flag 역할의 `stateHolder`를 마지막에 세팅한다.
  - `AgentInternalRouter.isInstalled()`는 `stateHolder != null && serverCommitHash != null && logReadyNotifier != null`로 확인한다.
  - `AgentHttpBridge.safeIntercept(Object,Object)`는 `isInstalled()` guard 후 internal request만 처리한다.
  - `TomcatFilterChainDoFilterAdapter`는 `ApplicationFilterChain#doFilter` 진입 시 `safeIntercept(req,res)`가 true면 `RETURN`한다.
- 런타임 검증 상태:
  - `8080` 대상 서버 PID `523656` 리슨 확인.
  - `8081` 엔진 PID `512616` 리슨 확인.
  - `server-test-err.log`에서 transform 성공 로그 확인:
    - `filter chain intercept installed class=org/apache/catalina/core/ApplicationFilterChain`
  - `POST /agent/resume` + `{"command":"RESUME_LOGGING"}`:
    - HTTP 200
    - `Content-Type: application/json;charset=UTF-8`
    - `{"state":"LOG_ON","serverCommitHash":"237ce4dc523b120e3311d4937f4085d4209439be"}`
  - `POST /agent/resume` + `{"command":"BAD_COMMAND"}`:
    - HTTP 200
    - `{"state":"LOG_OFF","reason":"INVALID_COMMAND","serverCommitHash":"..."}`
  - `POST /agent/resume` + mismatched `commitHash`:
    - HTTP 200
    - `{"state":"LOG_OFF","reason":"COMMIT_MISMATCH","serverCommitHash":"..."}`
  - 검증 종료 시 `POST /agent/resume` + `RESUME_LOGGING`을 다시 보내 `LOG_ON`으로 복구했다.
  - hijack 방지 확인:
    - `POST /api/agent/resume`는 agent JSON이 아니라 Spring Security 302 `/auth/login`으로 동작했다.
    - 즉 `/agent/resume` 정확 경로만 선점하고 `/api/agent/resume`은 가로채지 않는다.
- 결론:
  - Tomcat filter-chain 보정으로 Spring Security/CSRF보다 앞에서 `/agent/resume` 선점이 동작한다.
  - 113번의 핵심 실패는 해결됐다.
- 남은 주의:
  - 이 보정은 Tomcat adapter다. Jetty/Undertow/외장 WAS는 별도 adapter가 필요하다.
  - 난독화 설정 시 `AgentHttpBridge.install`, `safeEnterOrHandle`, `safeIntercept`는 keep ABI로 유지해야 한다.

## 115) 2026-05-05 엔진 재기동 resume E2E 검증
- GPT가 `8081` 엔진과 `8080` kjspringweb 서버를 직접 기동해 startup resume 복구 E2E를 검증했다.
- 기동 조건:
  - 엔진: `D:\workspace\turtlepick\engine-app\build\libs\engine-app.jar`, profile `dev`, PID `557740` 최종 리슨.
  - 서버: `kjspringweb-0.0.1-SNAPSHOT.jar` + `-javaagent:turtlepick-agent-core-0.1.0-SNAPSHOT.jar` + `-Dturtlepick.config=D:\workspace\kjspringweb\turtlepick.properties`, PID `570456` 리슨.
  - 서버 `turtlepick.agent.logging.rolling.interval-minutes=1`.
- 검증 흐름:
  - 엔진 ON 상태에서 서버 bootstrap/meta 성공 및 `/auth/login` HTTP 200 확인.
  - 최초 rolling에서 `log_ready ok fileName=trace-202605051403.log resultCode=ACK` 확인.
  - 엔진 PID `564792`를 내려 `log_ready failed fileName=trace-202605051404.log reason=HTTP_ERROR:ConnectException` 유도.
  - 서버 agent가 `agent state LOG_OFF reason=LOG_READY_FAILED`로 전환되는 것 확인.
  - 엔진 재기동 후 `EngineStartupResumeNotifier`가 `POST http://localhost:8080/agent/resume` 전송:
    - 엔진 로그: `[RESUME] startup send success status=200 ...`
    - 서버 로그: `agent state LOG_ON reason=RESUME`
  - resume 이후 `/auth/login` 요청이 `trace-202605051406.log`에 기록됨.
  - 다음 rolling에서 `log_ready ok fileName=trace-202605051406.log resultCode=ACK` 확인.
- 결론:
  - LOG_OFF 전환, 엔진 startup resume, LOG_ON 복구, trace 재개, log-ready ACK까지 E2E 통과.
- 실행 산출물:
  - 서버 로그: `logs\e2e-20260505-140341\server.out.log`, `logs\e2e-20260505-140341\server.err.log`
  - 엔진 로그: `D:\workspace\turtlepick\logs\engine-app-20260505-140614.log`
- 주의:
  - 현재 검증용 서버/엔진 프로세스는 계속 리슨 중이다: 서버 `8080` PID `570456`, 엔진 `8081` PID `557740`.

## 116) 2026-05-05 루트 테스트 로그 생성 금지 재확인
- `server-test.log`, `server-test-err.log`는 오전 테스트 과정에서 프로젝트 루트에 남은 임시 실행 산출물이다.
- 이는 87/92/93번에서 정한 원칙과 충돌한다. `.gitignore`로 숨기는 것은 보조 안전망일 뿐이며, 핵심 규칙은 **루트에 생성하지 않는 것**이다.
- 이후 수동 기동, smoke, E2E 검증 시 stdout/stderr/pid 파일은 반드시 목적별 하위 디렉터리로 보낸다.
  - 예: `logs/e2e-YYYYMMDD-HHmmss/server.out.log`
  - 예: `logs/e2e-YYYYMMDD-HHmmss/server.err.log`
  - 예: `logs/smoke/<case-name>.out.log`
- 금지:
  - `server-test.log`
  - `server-test-err.log`
  - `agent-smoke.out.log`
  - `agent-smoke.err.log`
  - `*.pid` 루트 직접 생성
- 테스트 명령을 새로 만들 때는 먼저 출력 경로가 루트인지 확인한다. 루트 파일명 리다이렉션이면 실행하지 않는다.

## 117) 2026-05-09 문서 재확인 메모
- 사용자 요청에 따라 현재 저장소 `D:\workspace\kjspringweb`의 `gpt.md`, `work_protocol.md`, `CLAUDE.md`, `docs/*.md`를 다시 확인했다.
- 이어서 `D:\workspace\turtlepick`의 `gpt.md`, `CLAUDE.md`, `work_protocol.md`, `docs/*.md` 목록과 주요 문서 내용을 확인했다.
- 현재 작업 모드는 계속 명시적 확정 전 제안/분석 중심이다. `확정. 반영.` 또는 `검토 완료. 확정. 수정해` 전에는 `gpt.md` 외 코드/문서 수정 금지 원칙을 유지한다.
- 양쪽 문서 기준 현재 다음 큰 우선순위는 `fqcn_method v2` 완료 이후의 Repository inherited methodId/meta 확장이다.
- `turtlepick/docs/작업지시문_20260503.md` 기준 다음 단위는 서버 agent meta request `repositories[]`, 엔진 `repositoryMethods[]`, inherited Repository methodId 발번/저장/응답, agent 별도 registry 적재까지이며, Repository AOP/DAO args/SQL 캡처는 범위 밖이다.

## 118) 2026-05-09 레거시 Spring 대상 서버 계획 메모
- 사용자 최신 방향: 지금부터 할 일은 엔진 작업이 아니라, 현재 Boot 기반 대상 서버와 별개로 **레거시 Spring 기반 대상 서버를 하나 더 만드는 것**이다.
- 현재 `kjspringweb`는 Spring Boot 4.0.2 + Java 17 + Thymeleaf + Security + Batch + JPA(H2) 기반이며, TurtlePick agent 검증용 Boot 대상 서버 역할을 한다.
- 새 레거시 서버는 Boot 기능 복제가 목적이 아니라, TurtlePick agent의 비-Boot/레거시 Spring 호환성을 검증하는 별도 표본으로 설계하는 것이 맞다.
- 우선 후보는 별도 프로젝트 또는 별도 디렉터리의 `kjspringweb-legacy`이며, WAR + Spring MVC 5.x + javax.servlet + XML 또는 Java Config 혼합 + H2/JPA 또는 MyBatis 기반을 검토한다.
- 테스트 가치가 큰 차이점:
  - `@SpringBootApplication` 없음
  - Boot auto-configuration 없음
  - 외장 Tomcat 또는 provided servlet API 기반 WAR
  - `javax.servlet` 경로
  - XML web.xml / DispatcherServlet / Spring Security filter chain
  - 레거시 Repository/DAO 패턴(MyBatis XML 또는 구형 Spring Data JPA)

## 119) 2026-05-09 `kjspringweb-legacy` 골격 생성 완료
- 사용자 지시에 따라 `D:\workspace\kjspringweb-legacy`에 레거시 Spring 대상 서버 껍데기를 생성했다.
- 확정 기준은 하한 호환성 검증을 위해 `Java 8 bytecode + Spring MVC 4.3.30.RELEASE + Spring Security 4.2.20.RELEASE + MyBatis 3.5.16 + H2 1.4.200 + WAR + javax.servlet 3.1 + web.xml/XML bean` 조합이다.
- 생성 구조:
  - `build.gradle`, `settings.gradle`, `.gitignore`, `README.md`
  - `src/main/webapp/WEB-INF/web.xml`
  - `application-context.xml`, `dispatcher-servlet.xml`, `security-context.xml`
  - JSP view 3개(`home/index.jsp`, `home/login.jsp`, `home/detail.jsp`)
  - `HomeController`, `BoardLegacyController`, `BoardLegacyService`, `BoardLegacyMapper`, `BoardLegacy`, MyBatis XML mapper
  - `schema.sql`, `data.sql`, `logback.xml`
- Gradle 9 계열 wrapper(`D:\workspace\kjspringweb\gradlew.bat -p D:\workspace\kjspringweb-legacy clean war`)로 빌드 검증 완료.
- 산출물: `D:\workspace\kjspringweb-legacy\build\libs\kjspringweb-legacy-0.0.1-SNAPSHOT.war`
- 현재 PC 상태:
  - 기본 JDK/Javac는 17.
  - Java 8은 JRE(`C:\Program Files\Java\jre1.8.0_461`)만 존재.
  - 이번 레거시 프로젝트는 JDK 17 `options.release = 8`로 Java 8 호환 바이트코드 빌드 후, 필요 시 Java 8 JRE 런타임 검증으로 진행한다.
- 다음 작업은 `D:\workspace\kjspringweb-legacy`로 작업 기준을 옮겨 외장 Tomcat 실행 또는 Cargo/Gretty류 없이 수동 WAR 배포 검증 방식을 정하는 것이다.

## 120) 2026-05-09 `kjspringweb-legacy` SQLite/세션 인증 전환
- 사용자 지시에 따라 `D:\workspace\kjspringweb-legacy` DB를 H2에서 SQLite로 변경했다.
- 현재 레거시 프로젝트 기준은 `Java 8 bytecode + Spring MVC 4.3.30.RELEASE + Spring Security 4.2.20.RELEASE + MyBatis 3.5.16 + SQLite JDBC 3.36.0.3 + WAR + javax.servlet 3.1 + web.xml/XML bean`이다.
- `build.gradle`에서 H2 의존성을 제거하고 `org.xerial:sqlite-jdbc:3.36.0.3`을 추가했다.
- `application-context.xml` datasource:
  - driver: `org.sqlite.JDBC`
  - url: `jdbc:sqlite:${legacy.sqlite.path:kjspringweb-legacy.db}`
  - 외장 Tomcat 실행 시 `-Dlegacy.sqlite.path=...`로 DB 위치를 지정할 수 있다.
- `schema.sql`/`data.sql`은 SQLite 파일 DB 재기동을 고려해 `create table if not exists`, `where not exists` 기반 seed로 바꿨다.
- 인증은 JWT/토큰이 아니라 Spring Security 4.x의 서버 세션 기반 form-login으로 명시했다.
  - `create-session="ifRequired"`
  - `<session-management invalid-session-url="/login"/>`
- `D:\workspace\kjspringweb-legacy\README.md`도 한글 문서로 정리했고, SQLite/세션 인증 기준을 반영했다.
- 검증: `D:\workspace\kjspringweb\gradlew.bat -p D:\workspace\kjspringweb-legacy clean war` 성공.

## 121) 2026-05-09 `kjspringweb-legacy` Maven 전환
- 사용자 방향에 따라 레거시 프로젝트답게 Gradle 대신 Maven 기반 WAR 프로젝트로 전환했다.
- `D:\workspace\kjspringweb-legacy\pom.xml`을 생성했다.
  - packaging: `war`
  - Java source/target: `1.8`
  - Spring MVC `4.3.30.RELEASE`
  - Spring Security `4.2.20.RELEASE`
  - MyBatis `3.5.16`
  - SQLite JDBC `3.36.0.3`
  - Servlet API `3.1.0` provided
- 기존 `build.gradle`, `settings.gradle`은 삭제하지 않고 각각 `build.gradle.bak`, `settings.gradle.bak`로 이동했다.
- README와 레거시 `gpt.md`도 Maven 기준으로 갱신했다.
- 현재 PC에는 `mvn` CLI가 설치되어 있지 않아 `mvn clean package`는 아직 미실행이다.
- 다음 결정점:
  - 시스템 Maven을 설치해서 진짜 레거시 방식으로 갈지
  - 아니면 재현성을 위해 Maven Wrapper(`mvnw`)를 추가할지 결정해야 한다.

## 122) 2026-05-09 Maven/Tomcat 포터블 설치 완료
- 사용자 지시 `설치해. 확정 반영`에 따라 Maven과 Tomcat을 전역 설치가 아니라 `D:\workspace\tools` 하위 포터블 설치로 구성했다.
- 설치 경로:
  - Maven: `D:\workspace\tools\apache-maven-3.9.9`
  - Tomcat: `D:\workspace\tools\apache-tomcat-8.5.100`
  - 다운로드 zip: `D:\workspace\tools\downloads`
- 검증:
  - `D:\workspace\tools\apache-maven-3.9.9\bin\mvn.cmd -version`으로 Apache Maven 3.9.9 확인.
  - `CATALINA_HOME`/`CATALINA_BASE`를 `D:\workspace\tools\apache-tomcat-8.5.100`으로 지정 후 `catalina.bat version`으로 Apache Tomcat/8.5.100 확인.
  - `D:\workspace\tools\apache-maven-3.9.9\bin\mvn.cmd -f D:\workspace\kjspringweb-legacy\pom.xml clean package` 성공.
- Maven WAR 산출물:
  - `D:\workspace\kjspringweb-legacy\target\kjspringweb-legacy.war`
- 아직 미수행:
  - Tomcat에 WAR 배포 후 `/login` 접속 검증.
  - Tomcat JVM에 TurtlePick `-javaagent` 부착 검증.

## 123) 2026-05-09 `kjspringweb-legacy` 기능 구현 및 smoke 검증
- 사용자 지시:
  - `kjspringweb`에서 테스트하지 못하는 것을 테스트하기 위한 환경 구성 프로젝트로 진행.
  - `kjspringweb`에 없는 기능 위주.
  - 일부러 에러 내기 쉬운 구조.
  - 화면단 validation 없음.
  - 구성/화면/흐름은 Codex가 자율 설계.
  - Kafka 등 무거운 인프라는 1GB/2코어 서버 스펙상 제외.
- 구현 방향:
  - Kafka 대체로 가벼운 DB queue 사용.
  - 파일 dropbox 처리.
  - 서버 세션 기반 작업함.
  - SQLite 제약/서비스 파싱/MyBatis XML 오류로 관측 가능한 실패 유도.
- 추가 기능:
  - `/ops`: 벤더/재고/DB queue 운영 실험실.
  - `/settlements`: 서버 세션 기반 정산 작업함.
  - `/file-import`: 파일 manifest 등록 및 dropbox 처리.
- 주요 에러 유도:
  - 숫자 파싱 실패: `creditLimit=abc`, `amount=abc`, `expectedRows=abc`.
  - SQLite 제약 실패: UNIQUE/NOT NULL/CHECK.
  - MyBatis raw SQL 실패: `/ops?sortColumn=...`가 `${sortColumn}`로 들어감.
  - DB queue 강제 실패: `forceErrorCode=NPE|SQL|NEGATIVE_STOCK`.
  - 파일 처리 실패: 파일 없음, row count mismatch.
- 구현 파일군은 `D:\workspace\kjspringweb-legacy\gpt.md` 11번에 상세 기록했다.
- 검증:
  - `D:\workspace\tools\apache-maven-3.9.9\bin\mvn.cmd -f D:\workspace\kjspringweb-legacy\pom.xml clean package` 성공.
  - Tomcat 8.5.100 별도 base `D:\workspace\kjspringweb-legacy\runtime\tomcat-base`, 18080 포트로 WAR 배포 성공.
  - `/kjspringweb-legacy/login` HTTP 200 및 form 확인.
  - `user/password` 세션 로그인 후 `/kjspringweb-legacy/ops` HTTP 200 확인.
  - `/ops/vendors`에 `creditLimit=abc` POST 시 `NumberFormatException` 에러 화면 노출 확인.
  - 검증용 Tomcat은 종료 완료. 18080 리슨 없음.
- 아직 미수행:
  - TurtlePick agent `-javaagent`를 Tomcat JVM에 붙인 레거시 계측 검증.

## 124) 2026-05-09 `kjspringweb-legacy` 운영형 서버 로그 반영
- 사용자 지시에 따라 레거시 프로젝트 서버 로그를 콘솔 전용에서 운영형 날짜별 파일 로그로 변경했다.
- 변경 파일:
  - `D:\workspace\kjspringweb-legacy\src\main\resources\logback.xml`
  - `HomeController.java` 접근 INFO 로그 추가.
  - `LegacyErrorController.java` ERROR 로그 추가.
- 로그 정책:
  - 기본 경로: `D:\workspace\kjspringweb-legacy\logs\app`
  - JVM 옵션: `-Dlegacy.log.path=...`
  - `legacy-app.log`: INFO 이상 현재 로그.
  - `legacy-app.%d{yyyy-MM-dd}.log`: 일자별 일반 로그, 14일 보관.
  - `legacy-error.log`: ERROR 이상 현재 로그.
  - `legacy-error.%d{yyyy-MM-dd}.log`: 일자별 에러 로그, 30일 보관.
- 검증:
  - Maven `clean package` 성공.
  - Tomcat smoke에서 `/kjspringweb-legacy/login` HTTP 200 확인.
  - `legacy-app.log`에 `legacy login page requested` INFO 로그 기록 확인.
  - 검증용 Tomcat 종료 완료.

## 125) 2026-05-09 `kjspringweb-legacy` 매일 에러 패턴 배치 추가
- 사용자 질문에 따라 매일 패턴별로 에러를 터뜨리는 레거시 배치를 추가했다.
- Spring Boot Batch가 아니라 레거시 Spring `task:scheduled` 기반으로 구현했다.
- 추가 파일:
  - `D:\workspace\kjspringweb-legacy\src\main\java\com\kjweb\legacy\batch\LegacyErrorPatternBatch.java`
  - `D:\workspace\kjspringweb-legacy\src\main\java\com\kjweb\legacy\web\controller\BatchLabController.java`
- 설정:
  - `application-context.xml`에 `task` namespace, `legacyTaskScheduler`, `<task:scheduled-tasks>` 추가.
  - cron: `0 35 3 * * *` (매일 03:35)
- 패턴:
  - `dayOfMonth % 5 == 0`: vendor duplicate UNIQUE 실패.
  - `== 1`: inventory negative CHECK 실패.
  - `== 2`: job status `BROKEN_STATUS` CHECK 실패.
  - `== 3`: `NumberFormatException`.
  - `== 4`: `NullPointerException`.
- `/ops` 화면에 수동 실행 form을 추가했다.
  - `daily`, `duplicate`, `inventory`, `status`, `number`, `npe`
- 검증:
  - Maven `clean package` 성공.
  - Tomcat smoke에서 context 초기화 성공.
  - 로그인 후 `/ops` HTTP 200.
  - `/ops` 화면에 `Daily Error Batch Manual Trigger` 노출 확인.
  - 검증용 Tomcat 종료 완료.

## 126) 2026-05-09 `kjspringweb-legacy` GitHub 공개 저장소 업로드
- 사용자 요청에 따라 `D:\workspace\kjspringweb-legacy`를 별도 Git 저장소로 초기화하고 GitHub 공개 저장소에 업로드했다.
- 저장소 URL: `https://github.com/kingskj/kjspringweb-legacy`
- 브랜치: `main`
- 첫 커밋: `2eaf278 Initial legacy Spring test app`
- 공개 업로드 전 점검:
  - `.gitignore`에 `target/`, `runtime/`, `logs/`, `.gradle/`, `.vscode/`, DB/로그/키 파일 패턴을 포함했다.
  - `runtime`, `logs`, `target`, `.gradle` 등 실행 산출물은 추적 대상에서 제외했다.
  - 민감 문자열 검색 결과 실제 키/API 토큰류는 없고 테스트 계정(`user/password`, `admin/admin`)만 확인했다.
- 원격 상태:
  - `origin`: `https://github.com/kingskj/kjspringweb-legacy.git`
  - 로컬 `main`이 `origin/main`을 추적한다.

## 127) 2026-05-09 `kjspringweb-legacy` Java 8 release/SQLite/Async 개선 반영
- Gemini 분석에 대한 사용자 지시 `확정 반영`으로 레거시 프로젝트 개선 3가지를 반영했다.
- 대상 저장소: `D:\workspace\kjspringweb-legacy`
- 반영 내용:
  - `pom.xml`: Maven compiler를 `<release>8</release>`로 변경해 JDK 17 빌드 시 Java 9+ API 누수를 컴파일 단계에서 차단.
  - `application-context.xml`: SQLite URL에 `journal_mode=WAL`, `busy_timeout=5000` 추가.
  - `web.xml`: `springSecurityFilterChain`, `DispatcherServlet`에 `<async-supported>true</async-supported>` 추가.
  - `AsyncLabController` 신규 추가: `/async-lab/callable`, `/async-lab/callable-error`, `/async-lab/deferred`, `/async-lab/deferred-error`.
  - `README.md`, 레거시 `gpt.md`에 반영 목적과 검증 경로 기록.
- 검증:
  - `D:\workspace\tools\apache-maven-3.9.9\bin\mvn.cmd -f D:\workspace\kjspringweb-legacy\pom.xml clean package` 성공.
  - 컴파일 로그에서 `javac [debug release 8]` 확인.
  - Tomcat 8.5.100 runtime base를 새 WAR로 재배포하고 18080 smoke 수행.
  - 로그인 후 async success 2개는 HTTP 200/본문 확인.
  - async error 2개는 `Legacy Error Observed` 에러 화면과 메시지 노출 확인.

## 128) 2026-05-09 `kjspringweb-legacy` JSP 화면 정리
- 사용자 요청으로 레거시 프로젝트의 화면을 기본 HTML 수준에서 정리했다.
- 대상 저장소: `D:\workspace\kjspringweb-legacy`
- 방향:
  - 테스트베드 성격과 화면단 validation 없음 원칙은 유지.
  - 공통 CSS를 추가해 레이아웃, 네비게이션, 폼, 테이블, 에러 화면만 정돈.
- 추가 파일:
  - `src/main/webapp/resources/css/legacy.css`
- 변경 JSP:
  - `home/login.jsp`, `home/index.jsp`, `home/detail.jsp`
  - `ops/dashboard.jsp`
  - `settlement/index.jsp`
  - `fileimport/index.jsp`
  - `error/legacy-error.jsp`
- 검증:
  - Maven `clean package` 성공.
  - Tomcat 8.5.100 runtime base에 새 WAR 재배포.
  - 로그인 후 `/`, `/ops`, `/settlements`, `/file-import`, `/legacy/boards/1` HTTP 200.
  - 각 화면의 CSS 링크 및 `/resources/css/legacy.css` HTTP 200 확인.

## 129) 2026-06-25 TurtlePick agent meta recovery/retransform 반영
- 사용자 지시 `확정 반영`으로 COMMIT_NOT_INDEXED/engine down 복구를 위한 agent runtime 경로를 반영했다.
- 핵심 문제:
  - 기존 `AgentPremain`은 bootstrap meta 실패 시 조기 `return`해서 `/agent/resume` 수신기와 method transformer가 설치되지 않았다.
  - resume으로 LOG_ON만 해도 이미 로드된 Controller/Service 클래스에는 probe가 없어 trace가 재개되지 않는 구조였다.
- 반영 내용:
  - `ApplicationMethodTransformer`를 빈 `MethodProbeIndex`로 premain 초기에 등록하고, `Can-Retransform-Classes=true` manifest로 변경.
  - `AgentRuntimeController`를 추가해 bootstrap/reload meta, probe index 교체, loaded class retransform, LOG_ON 전환을 한 곳에서 관리.
  - `AgentBootstrapService`는 meta 적재까지만 담당하고 성공 시 직접 `LOG_ON` 하지 않도록 변경.
  - `AgentHttpBridge`/`AgentInternalRouter`/`ResumeHandler`를 controller 기반으로 변경해 bootstrap 실패 후에도 `/agent/resume` 수신 가능.
  - `RESUME_LOGGING`, `RELOAD_META` 요청 시 meta를 재요청하고 성공 후 retransform 수행.
  - 같은 commit 재활성화는 기존 `LogReadyNotifier`/`TraceLogWriter`를 재사용해 현재 trace 파일 강제 close와 불필요한 log_ready를 만들지 않도록 처리.
- 검증:
  - `..\gradlew.bat shadowJar` 성공.
  - 산출 jar manifest에서 `Can-Retransform-Classes: true` 확인.
  - 정상 bootstrap: `agent state LOG_ON reason=BOOTSTRAP`, trace 생성 확인.
  - 수동 `/agent/resume`: HTTP 200, `retransformTransformed=18`, `retransformFailed=0` 확인.
  - engine down 상태에서 kjspringweb 선기동: `meta log_off ... HTTP_ERROR:ConnectException resumeReceiver=true`, 서버 `/auth/login` HTTP 200 확인.
  - 이후 engine 기동: engine startup git sync 후 RESUME 전송, agent `LOG_ON reason=RESUME_LOGGING`, `retransformTransformed=18`, `failed=0` 확인.
  - 복구 후 `/auth/login` 요청 trace가 `turtlepick-logs/trace-202606251121.log`에 기록됨.
