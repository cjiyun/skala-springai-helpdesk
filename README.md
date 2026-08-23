# skala-springai-helpdesk
An AI-powered helpdesk built with Spring AI for order support and policy-based Q&A

## 실행

필수 환경변수:

```bash
export OPENAI_API_KEY='...'
# 선택: 기본값은 user/admin
export LAB3_USER_PASSWORD='user'
export LAB3_ADMIN_PASSWORD='admin'
```

PGVector와 애플리케이션을 순서대로 실행합니다.

```bash
docker compose -f src/main/resources/docker-compose.yml up -d
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew bootRun --console=plain
```

`Started MyappApplication`이 출력되면 `http://127.0.0.1:8080`에서 요청을 받습니다.
기본 상담 계정은 `user1/user`입니다. 종료 후 DB 컨테이너는 다음 명령으로 내립니다.

브라우저에서 `http://127.0.0.1:8080/`을 열면 SSE 채팅 화면을 사용할 수 있습니다.
화면에서 사용할 기본 개발용 계정은 `user1/user`이며 애플리케이션 코드가 자격증명을 저장하지 않습니다.
Basic 인증은 운영 환경에서 반드시 HTTPS와 함께 사용해야 합니다.

```bash
docker compose -f src/main/resources/docker-compose.yml down
```

## 테스트

API 키 없이 컴파일과 단위·DB·안전·SSE·폴백 테스트를 실행할 수 있습니다.

```bash
./scripts/test-non-llm.sh
```

실제 RAG·멀티턴·메트릭 통합 테스트에는 유효한 `OPENAI_API_KEY`가 필요합니다.

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew test \
  --rerun-tasks \
  --tests 'com.skala.lab0.myapp.lab3.Lab3MultiTurnScenarioTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.RagAdvisorTest' \
  --console=plain
```

## RAG 검색 설정

Lab3의 `helpdesk.rag.top-k`는 고정값이 아니라 조정 가능한 검색 폭입니다. 현재 기본값은
`2`, 유사도 임계값은 `0.29`입니다. 실제 재인제스트 환경에서 관련 질문 점수(`0.334`)는
유지하고 일반적인 무관 질문의 최대 점수(`0.283`)는 제외하도록 조정했습니다. 이 설정에서 RAG·멀티턴 테스트와 성능·토큰 기준을
통과했으므로 과제 자료의 예시값 `5` 대신 유지합니다. 더 넓은 검색이 필요한 환경에서는
`--helpdesk.rag.top-k=5`로 실행해 같은 테스트와 측정 스크립트를 다시 수행할 수 있습니다.
세부 근거와 Lab2 실험과의 구분은 `src/test/resources/search-experiments.md`에 기록했습니다.

## 성능·토큰 측정

애플리케이션 실행 후 최소 20회 비스트리밍 요청의 성공·오류·폴백 수, P95 지연,
평균 토큰과 설정 단가 기반 예상 비용을 측정합니다. P95가 5초를 초과하거나 평균 토큰이 2,000을 초과하면
스크립트가 실패 코드로 종료됩니다.

```bash
SAMPLES=20 MAX_AVG_TOKENS=2000 ./scripts/measure-chat.sh
```

환경별 측정 결과는 아래 형식으로 기록합니다.

```text
측정일시:
모델:
환경:
samples=20 success=... error=... fallback=... p95_seconds=... avg_tokens=... estimated_cost_usd=...
```

최근 측정 결과:

```text
측정일시: 2026-08-23
모델: gpt-4o-mini
환경: WSL2, 로컬 Spring Boot + Docker PGVector
samples=20 success=20 error=0 fallback=0 p95_seconds=3.176453 avg_tokens=1024.00 estimated_cost_usd=0.00469200
판정: P95 5초 이하, 평균 토큰 2,000 이하 — 통과
```

Actuator의 `ai.tokens`, `ai.tokens.per.request`, `ai.latency`, `ai.fallback.calls`,
`ai.fallback.outcomes`, `ai.cost.estimated.usd`에서 모델별 토큰·지연·오류·폴백·비용을
조회할 수 있습니다. 예상 비용은 응답의 입력/출력 토큰에 설정 단가를 곱한 값이며 실제
청구액과 다를 수 있습니다. 기본 단가는 100만 토큰당 `gpt-4o-mini` 입력 $0.15·출력
$0.60, `gpt-4.1-mini` 입력 $0.40·출력 $1.60이고 `helpdesk.cost.*`로 변경할 수 있습니다.
