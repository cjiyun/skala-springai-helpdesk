# SKALA HelpDesk AI 최종 점검

점검일: 2026-08-23

| 단계 | 결과 | 검증 근거 |
|---|---|---|
| 0. 기준선 | 통과 | `compileTestJava`, `scripts/test-non-llm.sh`, LLM 테스트 분리 |
| 1. API·도메인 계약 | 통과 | `Lab3ChatControllerTest`, `Lab3AdminControllerTest` |
| 2. 주문·티켓 Tool | 통과 | 주문·최신 티켓 조회, 소유권 검증, 기본 사유, PENDING 재사용, 동시 요청 1건, 승인 후 재접수 테스트 |
| 3. 인제스트·검색 | 통과 | `Lab2IngestServiceTest`, `Lab2SearchServiceTest`, `Lab3ChunkAdminControllerTest`; PGVector Compose 구성 확인 |
| 4. RAG·출처 | 통과 | `RagAdvisorTest` 강제 재실행 성공: 관련 문서 출처와 무관 질문 빈 출처 검증 |
| 5. JDBC 메모리 | 통과 | `JdbcChatMemoryRepositoryTest`: 20개 제한, 사용자·세션 격리, Tool 호출·응답 복원 |
| 6. 안전·감사 | 통과 | 동기·SSE 선차단, 정상 정책 질문 허용, 메모리·로그 비보존, 레드팀 10/10 |
| 7. 멀티턴 | 통과 | 티켓 상태 조회를 포함한 7개 시나리오를 유효한 API 키로 강제 재실행해 성공 |
| 8. SSE·Web UI | 통과 | 분할 `NO_EVIDENCE` 차단, 빈 출처, 마지막 sources, 취소, 실제 SSE 정상 종료; `WebUiTest` |
| 9. 폴백·멱등성 | 통과 | 장애 주입 8개: 성공·실패 계측, 인증·입력·쓰기 미재시도, 부분 스트림 혼합 금지 |
| 10. 성능·토큰·비용 | 통과 | 20회 성공 20·오류 0·폴백 0, P95 3.176453초, 평균 1024.00토큰, 예상 비용 $0.00469200 |
| 11. 최종 정리 | 통과 | 수정 후 비-LLM 67개 및 LLM 통합 9개 강제 재실행 성공; `git diff --check` 성공 |

## 최종 명령

```bash
./scripts/test-non-llm.sh

GRADLE_USER_HOME="$PWD/.gradle" ./gradlew --no-daemon \
  -Dorg.gradle.jvmargs= test \
  --rerun-tasks \
  --tests 'com.skala.lab0.myapp.lab3.Lab3MultiTurnScenarioTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.RagAdvisorTest' \
  --console=plain

SAMPLES=20 MAX_AVG_TOKENS=2000 ./scripts/measure-chat.sh
```

## 참고

- 2026-08-23 최종 점검에서 Docker Compose 구성 검증에 성공했고 PGVector 컨테이너가 실행 중임을 확인했다.
- 애플리케이션 시작과 변경 후 성능·예상 비용 측정에 성공했다. P95와 평균 토큰 기준을 모두 충족했다.
- 실제 SSE 호출에서 토큰 순차 전송, 마지막 `sources`, 정상 종료를 확인했다.
- 티켓 조회 시나리오를 포함한 LLM 통합 테스트 9개를 유효한 API 키와 `--rerun-tasks`로 실행해 모두 통과했다.
- IDE Problems 창은 CLI로 검증할 수 없으므로 IDE에서 마지막으로 확인한다.
- 기존 Lab1/Lab2 코드는 교육 단계 산출물이므로 임의 삭제하지 않았다.
- 제출용 보고서와 캡처 산출물은 애플리케이션 소스와 분리하여 관리한다.
