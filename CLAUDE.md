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

## 11. 현재 상태 (2026-05-05)

- GCP 배포 상태로 동작 확인
- **TurtlePick Agent 섹터 1~9 모두 완료 (2026-05-05)**
- TurtlePick Engine 로컬 기동 확인 (localhost:8081)
- kjspringweb HEAD: `6d45d83916e8246239d29085b5af39bd20cb1366` (엔진에 인덱싱 완료)
- 서버 agent trace 포맷 정비 Unit 1~6 완료 (2026-05-03):
  - Unit 1: nodes[] call tree (callId/parentCallId)
  - Unit 2: trace v1 포맷 전환 (header record, compact/verbose, vfn)
  - Unit 3: try/catch 기반 exit 보장 (ATHROW 관통 대응)
  - Unit 4: 에러 메타 (errorCallId/ec/em, first-write-wins)
  - Unit 5: root cause + userFrames 추출 (CGLIB frame 제거)
  - Unit 6: 에러 지점 파라미터 캡처 (ea/errorArgs, exclude-classes)
- **섹터 9 완료 (2026-05-05)**:
  - log-ready 최종 실패 → `AgentStateHolder.markLogOff()` 전환
  - LOG_OFF 상태에서 신규 trace 진입 차단 (`RuntimeMethodBridge.enter()` 가드)
  - `POST /agent/resume` → Spring Security 앞에서 Tomcat 필터체인 선점 → LOG_ON 복구
  - E2E 검증 통과: LOG_ON/LOG_OFF/INVALID_COMMAND/COMMIT_MISMATCH/hijack방지 모두 정상
- **다음 작업: Repository/DAO 계측** — 엔진 meta 계약 선행 필요 → `turtlepick/docs/작업지시문_20260503.md`

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
| 8 | `POST /api/agent/log-ready` 전송 | ✅ (2026-05-02 완료) |
| 9 | log-ready 실패 → LOG_OFF + `/agent/resume` 수신 → LOG_ON 복구 루프 | ✅ (2026-05-05 완료) |

### trace 파일
- 경로: `turtlepick-logs/trace-{yyyyMMddHHmm}.log`
- 포맷: 1줄 JSON (JDK-only, ndjson)
- 롤링: slot 기반 (`rollingIntervalMinutes`)

---

## 13. 완료된 것 (2026-06-25 세션)

### agent bootstrap 실패 후 resume 수신부 유지 + retransform 복구

**문제**: 엔진 다운 상태에서 agent bootstrap 시 meta 실패 → `AgentPremain` 조기 return → `AgentHttpBridge` / `TomcatFilterChainInterceptTransformer` 미설치 → 엔진 복구 후 `RESUME_LOGGING` 수신 불가. 또한 bootstrap 실패 시 `ApplicationMethodTransformer` 미등록 → resume 후 meta 재요청 성공해도 trace 안 찍힘.

**수정 파일**:
- `AgentPremain.java` — bootstrap 결과와 무관하게 `ApplicationMethodTransformer(empty, canRetransform=true)`, `AgentHttpBridge.install(runtimeController)`, Tomcat/Spring hook 먼저 등록. 실패 시 `resumeReceiver=true` 로그 후 return
- `AgentRuntimeController.java` (신규) — `Instrumentation` 생성자 주입. `bootstrapAndActivate()` / `reloadMetaAndActivate(trigger, expectedHash)`. `synchronized(lock)` 동시 reload 방지. `ensureLogReadyNotifier()`: 동일 commit → 재사용, 다른 commit → shutdown + TraceLogWriter 재설치. `retransformLoadedClasses()`: previousIndex ∪ nextIndex class 대상, 개별 try-catch + VMError/ThreadDeath re-throw
- `ApplicationMethodTransformer.java` — `volatile MethodProbeIndex` + `updateIndex(next)` 이전 index 반환
- `MethodProbeIndex.java` — `empty()` static factory, `classNames()` dot FQCN keySet 반환
- `ResumeHandler.java` — `AgentRuntimeController` 위임으로 교체. `RESUME_LOGGING`/`RELOAD_META` 양쪽 수신. retransform 결과 JSON 응답 포함
- `build.gradle` — `Can-Retransform-Classes: true` manifest 추가

**실기동 검증 (2026-06-25)**:
- 엔진 다운 선기동: `meta log_off ... resumeReceiver=true` ✅
- 엔진 기동 후 RESUME 수신: `LOG_ON reason=RESUME_LOGGING retransformTransformed=18 failed=0` ✅
- 복구 후 trace 파일 생성 ✅

**난독화 keep 대상 변경**:
```
AgentHttpBridge.install(AgentRuntimeController)   ← 시그니처 변경
AgentHttpBridge.safeIntercept(Object, Object): boolean
AgentHttpBridge.safeEnterOrHandle(Object, Object): boolean
RuntimeMethodBridge.*
HttpRequestContextBridge.*
AgentRuntimeController.reloadMetaAndActivate(String, String)
AgentRuntimeController.markLogOff()
AgentRuntimeController.getServerCommitHash()
```

---

## 13-2. Step 1 구현안 확정 대기 (2026-07-02 세션)

### 세션 규칙
- **이 세션 한정 역할 교체**: Claude가 구현/수정 담당, GPT가 검토/딴지 담당 (오빠 구두 지시. work_protocol.md 문서는 원본 유지)
- **최우선 절대 규칙 재확정**: Claude가 허락 없이 반영 가능한 파일은 CLAUDE.md 하나뿐. 그 외 모든 파일은 "확정 반영" 문구 확인 후에만 적용. 대화 중 언급은 수정 지시가 아님.

### 작업 대상
`turtlepick/docs/작업지시문_20260625.md` **Step 1** — JPA Repository 호출을 trace node로 연결.
엔진은 완료 상태 (`repositoryMethods[]` 28개 응답 확인). agent 쪽만 미착수.

### 합의된 보정 스펙 (GPT 딴지 7개 + Claude 판정 완료)
1. `InterfaceMethodRegistry`에는 Step 1에서 **repositoryMethods[]만 적재** — `methods[]` 적재는 Step 3(MyBatis)에서 mapper package 기준 생긴 뒤
2. **TraceContext 없는 repository proxy 호출은 no-op** — root trace 생성 금지 (기동 시점 내부 호출 쓰레기 trace 방지)
3. `enterInterfaceMethod`는 Throwable 방어 (VirtualMachineError/ThreadDeath만 re-throw), 실패 시 0 반환, **miss 무로그**
4. **declaringClass 1차 miss는 정상 경로** — inherited method의 `Method.getDeclaringClass()`는 `CrudRepository`라 registry(owner=사용자 Repository) 1차 miss가 기본. 2차(proxy direct interfaces 순회)가 주 경로
5. proxy interfaces 순회는 **direct만, 재귀 없음** (Step 1 확정. Step 3에서 재평가) — Spring Data JDK proxy는 direct에 repository interface 항상 포함
6. hit 복수면 warn + null (조용히 첫 hit 선택 금지)
7. ASM hook은 **methodId local을 try region 진입 전 ICONST_0으로 선초기화** (verifier 방어), 모든 exit에서 `methodId > 0` 체크
8. Java 8 API만 (record/List.of/var 금지), shadowJar 후 classfile major version 52 확인
9. JDK/CGLIB transform hit 로그 추가 (`aop proxy hook installed target=...`). JPA는 JDK proxy가 핵심, CGLIB은 보험

### 구현안 핵심 결정 (상세안 대화창 출력 완료, 확정 대기)
- **신규 4**: `http/RepositoryMethodDto`, `state/InterfaceMethodRegistry`, `instrument/SpringAopProxyInvokeTransformer`(+visitor/adapter), `JsonCodecSupport.readStringArrayField()`
- **수정 7**: `MetaJsonCodec`(repositoryMethods 파싱), `MetaResponse`(필드 추가+logOff), `RuntimeMethodBridge`(installInterfaceMethodRegistry/enterInterfaceMethod), `AgentBootstrapService`(clear 4곳+replace), `BootstrapResult`(interfaceMethodCount), `AgentRuntimeController`(COMMIT_MISMATCH clear+로그), `AgentPremain`(조립+transformer 등록)
- hook 대상 두 곳 모두 arg0=proxy/arg1=Method/arg2=Object[] 동일 → 어댑터 하나 공용:
  - `JdkDynamicAopProxy.invoke`
  - `CglibAopProxy$DynamicAdvisedInterceptor.intercept`
- `SafeClassWriter`를 `SpringWebRequestTransformer` private inner에서 package-private으로 승격해 공유
- **retransform 불필요 판단**: 훅 바이트코드는 registry 무관 고정(런타임 volatile 조회), premain에서 Spring 로드 전 transformer 등록 → 최초 로드 시 weave. meta reload는 registry swap만으로 반영 (지시문 2-4보다 단순화 — GPT 이견 시 재논의)
- `enterInterfaceMethod` 체크 순서 = 성능 방어: registry empty → context null → 그 다음 suffix 문자열 생성
- repositoryMethods empty는 실패 아님(warn만). methods empty만 기존대로 bootstrap 실패
- exception exit의 args는 arg2(원본 대상 메서드 인자) 그대로 전달

### 검증 계획
1. shadowJar BUILD SUCCESSFUL + major version 52
2. 기동 로그: `aop proxy hook installed target=...JdkDynamicAopProxy` + `interfaceMethodCount=28`
3. `GET /board/{id}` (`/auth/login` 사용 금지) → trace node `m` == 엔진 DB `method_kind='REPOSITORY_INHERITED'`의 method_id
4. Controller→Service→Repository parent chain 정상
5. RELOAD_META 후 registry 재적재 + trace 유지
6. 회귀: 기존 trace RESOLVED 유지

### 2차 딴지 반영 델타 (2026-07-02, GPT 딴지 3개 판정 완료)
1. **registry key collision (수용)**: build 단계에서 중복 key는 map 제거 + `ambiguousKeys` 로컬 set으로 재삽입 차단 + build 시 warn 1회. lookup은 무로그 miss (핫패스 로그 폭탄 방지 — lookup-time warn은 부분 수용으로 제외, GPT 이견 시 재논의)
2. **SafeClassWriter 공통화 취소 (수용)**: `SpringAopProxyInvokeTransformer` 내 private 복사 (3번째 사본). 공통화는 Step 3 이후 별도 정리 항목
3. **repositoryMethods 필드 검증 (수용, 방식 조정)**: 이름 휴리스틱 대신 `params`/`runtimeParams` **필드 존재 검증** (`findFieldValueStart < 0` → parse failure) + `returnType` 필수 검증. 빈 배열은 통과(findAll/count 정상), 필드 누락은 bootstrap 실패 → LOG_OFF로 명시화

### 상태
- **✅ Step 1 구현 + 실기동 E2E 완료 (2026-07-02, "확정. 반영." 트리거 후 Claude 직접 구현)**

### 구현 결과 (2026-07-02)
**신규 5파일**: `http/RepositoryMethodDto`, `state/InterfaceMethodRegistry`, `instrument/SpringAopProxyInvokeTransformer`(private SafeClassWriter 복사 포함), `instrument/SpringAopProxyClassVisitor`, `instrument/SpringAopProxyInvokeAdapter`
**수정 8파일**: `JsonCodecSupport`(readStringArrayField), `MetaResponse`(repositoryMethods), `MetaJsonCodec`(파싱+필드존재검증), `RuntimeMethodBridge`(enterInterfaceMethod), `AgentBootstrapService`(clear 4곳+replace), `BootstrapResult`(interfaceMethodCount), `AgentRuntimeController`(주입+COMMIT_MISMATCH clear+로그), `AgentPremain`(조립+transformer 등록)

### E2E 검증 결과 (2026-07-02, 서버 PID 94916 / 엔진 8081)
- shadowJar BUILD SUCCESSFUL, 신규 클래스 5개 major version 52 ✅
- `aop proxy hook installed target=JdkDynamicAopProxy` + `CglibAopProxy$DynamicAdvisedInterceptor` 둘 다 weave ✅
- `agent state LOG_ON ... interfaceMethodCount=28` ✅
- `POST /auth/join` → trace node `methodId=426083339` == 엔진 DB `MemberRepository#save(Member):Member` REPOSITORY_INHERITED ✅
- parent chain: AuthController#join → MemberService#join → MemberRepository#save ✅
- `RESUME_LOGGING` reload → `interfaceMethodCount=28` 재적재, `retransformTransformed=18 failed=0`, reload 후 2차 join에서도 repository node 유지 ✅
- 회귀: `/board`, `/board/1`, `/auth/login` trace 정상, log_ready ACK 3회 연속 ✅

### E2E 중 확인된 스펙 동작 (버그 아님)
- `/board/1`(getDetail)은 `findByIdAndIsDeletedFalse` — **선언 쿼리 메서드**라 registry 미대상 → node 없음 (Step 1 스펙 정상. 선언 쿼리 메서드 trace는 후속 범위)
- inherited 호출을 HTTP로 유발하는 확실한 경로: `POST /auth/join` → `memberRepository.save()` (CSRF 토큰 필요), `/admin/members` → `findAll()` (관리자 인증 필요)

### 다음 작업 (Step 1 시점 기록 — 이후 진행은 13-3 참조)
- Step 2: 엔진 `extra-mapper-packages` config + `BusinessLayerScanner` 보강 (turtlepick 쪽)
- Step 3: kjspringweb-legacy `MyBatisMapperProxyTransformer` + 외장 Tomcat E2E

---

## 13-3. Step 3 진행 — MyBatis 대응 agent-core (2026-07-02 후반)

> Step 2(엔진 MyBatis mapper 발번)는 turtlepick 쪽에서 완료. 여기는 **agent-core(부트/레거시 공용 JAR)** 쪽 Step 3.
> **단일 코어 원칙**: agent-core는 JAR 1개. 부트/레거시가 사본을 갖는 게 아니라 실행 시 `-javaagent`로 같은 JAR 주입. servlet/spring/ibatis **import 0** (전부 ASM 문자열 매칭), javax+jakarta 동시 처리 → 서버 프레임워크 비종속. 타깃 부재 시 no-op이라 부트에 MyBatis 훅 얹어도 무해.

### 단위 1 완료 — declaredMethods lookup 경계 분리 (dormant)
MyBatis 훅 붙이기 전에 `methods[]` 기반 declared lookup 경로를 repository 경로와 분리.
- `InterfaceMethodRegistry`: `repositoryMethods` map / `declaredMethods` map **분리**
  - `replaceDeclaredMethods(List<MethodMapping>)` — `MethodSignatureParser.parse(fqcnMethod)` **재사용**(새 파서 금지)
  - runtime key는 **source-form 타입명**(`java.lang.String[]`, `int[]`, primitive keyword) — `Class.getName()` descriptor form **금지**(array가 `[Ljava...;`로 나와 엔진 `java...[]`와 불일치 → silent miss). `toSourceTypeName(Class)` 헬퍼로 array 재귀 처리
  - `lookupDeclared(Method)` — proxy 인자 없음. MyBatis는 `method.getDeclaringClass()`가 mapper interface 자신이라 declaringClass 직접 조회가 주 경로 (JPA와 반대)
  - collision: build 시 중복 key 제거 + warn 1회, lookup 무로그
- `RuntimeMethodBridge.enterDeclaredInterfaceMethod(Method)`: `int` 반환(miss/no-op/fail=0), **TraceContext null이면 no-op**, miss 무로그, Throwable 방어(VMError/ThreadDeath만 rethrow)
- lifecycle: bootstrap 성공 시 `declaredMethods` replace, clear는 두 map 다, `declaredMethodCount` 배선
- ⚠️ 리뷰 캐치: 필드 rename(`registry`→`repositoryMethods`)로 파라미터와 shadow 충돌 → `repositoryMethods = next`가 컴파일 안 됨 → `this.repositoryMethods = next`로 수정
- 상태: **dormant** — 어떤 transformer도 아직 `enterDeclaredInterfaceMethod` 미호출 → 부트/JPA 동작 구조적으로 불변. build/major52 확인

### 단위 2 완료(코드) — MyBatis MapperProxy 훅
- 신규: `instrument/MyBatisMapperProxyTransformer`, `MyBatisMapperProxyClassVisitor`, `MyBatisMapperProxyInvokeAdapter`
- 수정: `AgentPremain` (87번 `new MyBatisMapperProxyTransformer()` 무조건 등록)
- 훅 대상: `org/apache/ibatis/binding/MapperProxy.invoke(Object,Method,Object[])`
- 불변식: SpringAOP 훅=`enterInterfaceMethod`(repository만) / MyBatis 훅=`enterDeclaredInterfaceMethod`(declared만) — **경계 절대 공유 금지**(이중 trace 방지). loader null skip, `SafeClassWriter(reader,loader)`, `loadArg(1)`=Method / `loadArg(2)`=args, methodId try 전 0 선초기화, `methodId>0`만 exit
- descriptor는 `InvocationHandler.invoke` 계약 고정 → **MyBatis 버전 분기 넣지 말 것**
- 검증: shadowJar 성공, 신규 3클래스 major 52 ✅
- 상태: **코드 완료. legacy 외장 Tomcat 실기동 E2E 미실행 → Step 3 전체 완료 아님**

### 남은 것 = legacy E2E (kjspringweb-legacy/CLAUDE.md 4장 참조)
셋업(엔진 legacy config 기동 + legacy `turtlepick.properties` + Tomcat `-javaagent` 배선) 후 **3단 게이트**:
1. Controller node (외장 Tomcat javax FilterChain hook + HTTP context — **최대 미지수**)
2. Controller→Service
3. Controller→Service→Mapper
1칸 실패 시 Mapper 보지 말 것 (`enterDeclaredInterfaceMethod`는 context null이면 no-op).

### 부채 (hardening 때 정리)
- `SafeClassWriter` **4중 복사** (SpringWeb/ApplicationMethod/SpringAopProxy/MyBatis)
- proxy invoke adapter **near-dup 2개** (SpringAop/MyBatis — enter 호출 한 줄 빼고 동일)
- 난독화 keep 경계: 진입점 `AgentPremain.premain` + bridge 6메서드(`RuntimeMethodBridge.enter/exit/enterInterfaceMethod/enterDeclaredInterfaceMethod`, `HttpRequestContextBridge.safeExit`, `AgentHttpBridge.safeEnterOrHandle/safeIntercept`). self-reflection 0. MyBatis 훅은 `enterDeclaredInterfaceMethod` 재사용이라 keep 안 늘어남
- turtlepick-agent-core 신규 파일들 git **untracked** — 커밋 시 `git add` 필요

---

## 13-1. 다음 과제 (2026-06-25 기준)

### 서버 agent — 섹터 9 완료. 다음은 엔진 선행 후 진행

### 엔진 선행 후 서버 작업

| 작업 | 지시문 |
|------|--------|
| Repository inherited methodId 발번 계약 구현 | `turtlepick/docs/작업지시문_20260503.md` |
| Repository AOP 계측 (DAO node 추가) | 엔진 meta 확장 후 |
| Repository DAO args 캡처 | Repository AOP 완료 후 |

### 엔진 작업

| 순위 | 작업 |
|------|------|
| 1 | **선행: fqcn_method v2** — engine + agent 동시 변경 (원자 단위) |
| 2 | Repository inherited methodId 발번 + meta 응답 확장 (`작업지시문_20260503.md`) |
| 3 | 실파일 수거/파싱/DB저장/archive/delete (stub → 실구현) |

### fqcn_method v2 — agent 변경 포인트

- ASM descriptor에서 runtime signature 구성 시 return type 포함
- 기존: `{ownerFqcn}#{methodName}({paramTypes})`
- 변경: `{ownerFqcn}#{methodName}({paramTypes}):{returnType}`
- engine과 동시 변경 필수. 중간 상태에서 meta registry miss 발생함.

---

## 14. Trace 로그 포맷 설계 확정 (2026-05-03)

### 로그 사상

- 서버는 요청 흐름을 파일에만 기록. 엔진과 실시간 통신 없음
- 엔진이 주기적으로 파일 수거 → 분석/저장/도식화
- 서버 부담 최소, 요청 중 I/O는 파일 append만
- 에러는 운영 자산 — 별도 철학으로 전문 저장

### 파일 포맷 (확정)

줄 단위 JSON (NDJSON). 파일이 닫히지 않아도 쓰인 줄까지 파싱 가능.

**헤더 레코드 (파일 첫 줄, 1회만)**
```json
{"f":"h","v":1,"vfn":false,"c":"6d45d83...","ts":1777788119253}
```

| 키 | 의미 | 비고 |
|----|------|------|
| `f` | record type = `"h"` (header) | |
| `v` | 포맷 버전 | 현재 1 |
| `vfn` | verbose-field-names 여부 | false=compact(기본), true=풀네임 |
| `c` | commitHash | |
| `ts` | 파일 생성 시각 (epoch ms) | |

**정상/에러 trace 레코드 (요청 1건 = 1줄)**
```json
{"f":"t","ep":1266277122,"e":false,"n":[{"i":1,"p":0,"m":113932304,"st":0,"et":0}]}
```

| 키 | 의미 |
|----|------|
| `f` | record type = `"t"` (trace) |
| `ep` | endpointId |
| `e` | 에러 여부 (boolean) |
| `n` | nodes 배열 |

**node 구조 (object, 배열 아님 — 스키마 진화 대응)**

| 키 | 의미 |
|----|------|
| `i` | callId (요청 내 호출 인스턴스 증가값) |
| `p` | parentCallId (0이면 루트) |
| `m` | methodId |
| `st` | trace 시작 기준 start offset ms |
| `et` | trace 시작 기준 end offset ms |

에러 시 추가 예정 (다음 단위): `a`(args), `o`(result), `q`(SQL), `r`(query_result)

### verbose-field-names 옵션

- 설정: `turtlepick.agent.logging.verbose-field-names=false`
- Java 필드명: `verboseFieldNames`
- 헤더 키: `vfn`
- 기본값: `false` (compact) — **운영은 항상 false**
- `true`는 개발/눈검사 전용. 엔진 파싱 보장 범위 밖
- `vfn=true`일 때 full key 예시:
  ```json
  {"format":"header","version":1,"verboseFieldNames":true,"commitHash":"...","createdAt":...}
  {"format":"trace","endpointId":1266277122,"error":false,"nodes":[{"callId":1,"parentCallId":0,"methodId":113932304,"startOffsetMs":0,"endOffsetMs":0}]}
  ```

### 에러 메타 포맷 (Unit 4 확정)

에러 trace 레코드 추가 필드:

| 키 (compact) | 키 (verbose) | 의미 |
|-------------|-------------|------|
| `eci` | `errorCallId` | 최초 예외 발생 프레임 callId (first-write-wins) |
| `ec` | `exceptionClass` | 예외 클래스 FQCN |
| `em` | `exceptionMessage` | 예외 메시지 (truncate 미적용, 5단위에서 처리) |

`errorCallId == null`이면 `eci/ec/em` 생략. `e:false`이면 에러 필드 없음.

### stack trace 핵심 추출 방향 (5단위 예정)

- **방향**: root cause까지 `getCause()` 재귀 → 사용자 패키지 프레임만 필터
- compact: `stk`, verbose: `stackTrace`
- `turtlepick.properties`에 `base-package=com.kjweb` 설정 추가
- 최대 10개 프레임 (고정)
- exceptionMessage max 500자 truncate (고정, config 없이)

### 미확정/보류 사항

- 정상/에러 파일 분리: 현재는 `e` 플래그로 한 파일 내 구분. 파일 분리는 다음 단위
- args/result/SQL 캡처: Repository AOP 완료 후 별도 단위

### 현재 구현 vs 목표 포맷 차이

| 항목 | 현재 (verbose) | 목표 (운영) |
|------|----------------|-------------|
| commitHash | 없음 | 파일 헤더 1회 |
| 문자열 필드 | entryFqcnMethod 등 반복 | 제거 |
| node 구조 | object (f=fqcnMethod 포함) | object (f 제거) |
| 에러 여부 | 없음 | `e` 플래그 |
| record type | 없음 | `f` 플래그 |

---

## 15. TurtlePick Engine 연동 정보 (2026-03-28)

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

## 16. 섹터 9 구현 확정 (2026-05-05 완료)

### LOG_OFF 전환 + trace 차단

- `LogReadyNotifier.sendWithRetry()` 최종 실패 → `stateHolder.markLogOff()` + queue.clear() + return false
- `runLoop()` false 반환 시 break + finally `clearWorkerIfCurrent()` (worker 레퍼런스 null 처리)
- `RuntimeMethodBridge.enter()`: `context == null`(신규 trace)일 때만 LOG_OFF 체크 → no-op
  - 진행 중인 trace는 LOG_OFF 전환 이후에도 stack 정합성 유지
- `RuntimeMethodBridge.exitUnsafe()` root flush: `holder == null || holder.isLogOn()` 일 때만 write

### `/agent/resume` 구현 구조

```
[Tomcat ApplicationFilterChain#doFilter 앞단 선점]
  TomcatFilterChainInterceptTransformer  ← ASM COMPUTE_FRAMES + SafeClassWriter
    → AgentHttpBridge.safeIntercept(req, res): boolean  ← keep ABI
         not installed → return false (필터체인 정상 흐름)
         /agent/resume (POST, exact match) → AgentInternalRouter.handle() → return true → doFilter RETURN
         그 외 → return false (필터체인 정상 흐름, Spring Security 거침)

[DispatcherServlet#doDispatch 앞단 — 일반 요청]
  SpringWebRequestTransformer
    → AgentHttpBridge.safeEnterOrHandle(req, res): boolean
         /agent/resume → AgentInternalRouter.handle() → return true → doDispatch RETURN (도달 안 함)
         그 외 → HttpRequestContextBridge.safeEnter(req) → return false → 기존 흐름

AgentInternalRouter (package-private, 난독화 가능)
  install(): serverCommitHash → logReadyNotifier → stateHolder (마지막 대입 = volatile 가시성 플래그)
  isInstalled(): 3개 필드 모두 non-null 체크
  isInternalRequest(): POST + normalizeUri() + exact equals "/agent/resume"
  → ResumeHandler.handle(req, res)

ResumeHandler (package-private, 난독화 가능)
  command 검증 (RESUME_LOGGING 아니면 INVALID_COMMAND + LOG_OFF)
  commitHash optional 비교 (없으면 pass, 불일치 COMMIT_MISMATCH + LOG_OFF)
  성공: markLogOn() + logReadyNotifier.start() + LOG_ON 응답
```

### 응답 계약 (HTTP 200 고정)

| 상황 | body |
|------|------|
| LOG_ON 성공 | `{"state":"LOG_ON","serverCommitHash":"..."}` |
| commitHash 불일치 | `{"state":"LOG_OFF","reason":"COMMIT_MISMATCH","serverCommitHash":"..."}` |
| command 오류 | `{"state":"LOG_OFF","reason":"INVALID_COMMAND"}` |
| 내부 처리 실패 | `{"state":"LOG_OFF","reason":"RESUME_HANDLE_FAILED"}` |

### commitHash 처리 정책

- 엔진 resume body: `{"command":"RESUME_LOGGING","reason":"engine-recovered"}` — commitHash 없음
- 서버: commitHash optional — 없으면 무조건 LOG_ON, 있으면 서버 hash와 비교

### hijack 방지 (검증 완료)

- `/agent/resume` (정확 경로) → 에이전트 선점 ✓
- `/api/agent/resume` 등 다른 경로 → 필터체인 통과 → Spring Security 처리 (302)  ✓

### 난독화 keep 대상

```
AgentHttpBridge.install(AgentStateHolder, String, LogReadyNotifier)
AgentHttpBridge.safeIntercept(Object, Object): boolean
AgentHttpBridge.safeEnterOrHandle(Object, Object): boolean
RuntimeMethodBridge.*
HttpRequestContextBridge.*
```

---

## 17. 관련 프로젝트

| 프로젝트 | 역할 |
|----------|------|
| kjspringweb (이 레포) | TurtlePick 대상 서버 |
| TurtlePick | 비즉시성 모니터링 엔진 (로컬 기동 중) |
| kjmacro2 | Diablo2 Vision RPA 엔진 (별개 프로젝트) |

---

## 18. 작업 규칙 (2026-03-28)

- CLAUDE.md 수정: Claude 자율 허용
- 나머지 파일 수정: 금지 (오빠 명시적 지시 시에만)
- Claude 역할: GPT 제안 딴지/보완안 채팅 제시만
- 코드 수정 실행: GPT(Codex) 담당

## 19. 작업 프로토콜

`work_protocol.md` 참고 (GPT + Claude 핑퐁 방식 동일 적용)