#!/usr/bin/env bash
set -euo pipefail

gradle_args=()
if [[ "${RERUN_TASKS:-0}" == "1" ]]; then
  gradle_args+=(--rerun-tasks)
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.gradle}" ./gradlew --no-daemon -Dorg.gradle.jvmargs= \
  compileTestJava test "${gradle_args[@]}" \
  --tests 'com.skala.helpdesk.HelpDeskApplicationTests' \
  --tests 'com.skala.helpdesk.rag.IngestServiceTest' \
  --tests 'com.skala.helpdesk.rag.SearchServiceTest' \
  --tests 'com.skala.helpdesk.advisor.RagAdvisorContextTest' \
  --tests 'com.skala.helpdesk.advisor.AdvisorOrderTest' \
  --tests 'com.skala.helpdesk.advisor.SafetyAdvisorTest' \
  --tests 'com.skala.helpdesk.advisor.TokenMeterAdvisorTest' \
  --tests 'com.skala.helpdesk.redteam.RedTeamDefenseTest' \
  --tests 'com.skala.helpdesk.chat.*' \
  --tests 'com.skala.helpdesk.service.*' \
  --tests 'com.skala.helpdesk.tools.*' \
  --tests 'com.skala.helpdesk.web.*' \
  --console=plain
