#!/usr/bin/env bash
set -euo pipefail

gradle_args=()
if [[ "${RERUN_TASKS:-0}" == "1" ]]; then
  gradle_args+=(--rerun-tasks)
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.gradle}" ./gradlew --no-daemon -Dorg.gradle.jvmargs= \
  compileTestJava test "${gradle_args[@]}" \
  --tests 'com.skala.lab0.myapp.MyappApplicationTests' \
  --tests 'com.skala.lab0.myapp.rag.service.Lab2IngestServiceTest' \
  --tests 'com.skala.lab0.myapp.rag.service.Lab2SearchServiceTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.RagAdvisorContextTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.AdvisorOrderTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.SafetyAdvisorTest' \
  --tests 'com.skala.lab0.myapp.lab3.advisor.TokenMeterAdvisorTest' \
  --tests 'com.skala.lab0.myapp.lab3.redteam.RedTeamDefenseTest' \
  --tests 'com.skala.lab0.myapp.lab3.chat.*' \
  --tests 'com.skala.lab0.myapp.lab3.ticket.*' \
  --tests 'com.skala.lab0.myapp.lab3.tools.*' \
  --tests 'com.skala.lab0.myapp.lab3.web.*' \
  --console=plain
