#!/usr/bin/env bash
set -euo pipefail

base_url="${HELPDESK_URL:-http://127.0.0.1:8080}"
samples="${SAMPLES:-20}"
user="${HELPDESK_USER:-user1}"
password="${HELPDESK_USER_PASSWORD:-user}"
max_tokens="${MAX_AVG_TOKENS:-2000}"

case "$samples" in *[!0-9]*|0) echo "SAMPLES must be a positive integer" >&2; exit 2;; esac
command -v curl >/dev/null
command -v python3 >/dev/null

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
times="$work_dir/times"
success=0
error=0
run_id="$(date +%s)-$$"

metric() {
  curl -fsS "$base_url/actuator/metrics/$1" 2>/dev/null || printf '{"measurements":[]}'
}

fallback_before="$(metric ai.fallback.calls | python3 -c '
import json,sys
d=json.load(sys.stdin)
print(next((x["value"] for x in d.get("measurements",[]) if x["statistic"]=="COUNT"),0))')"
cost_before="$(metric ai.cost.estimated.usd | python3 -c '
import json,sys
d=json.load(sys.stdin)
print(next((x["value"] for x in d.get("measurements",[]) if x["statistic"]=="COUNT"),0))')"
read -r token_count_before token_total_before < <(metric ai.tokens.per.request | python3 -c '
import json,sys
d=json.load(sys.stdin); m={x["statistic"]:x["value"] for x in d.get("measurements",[])}
print(m.get("COUNT",0),m.get("TOTAL",0))')

for i in $(seq 1 "$samples"); do
  result="$(curl -sS -u "$user:$password" -o "$work_dir/body-$i" -w '%{http_code} %{time_total}' \
    -H 'Content-Type: application/json' \
    -d "{\"sessionId\":\"performance-$run_id-$i\",\"question\":\"반품 규정 알려줘\"}" \
    "$base_url/api/chat" || printf '000 0')"
  code="${result%% *}"
  elapsed="${result##* }"
  if [[ "$code" == 2* ]]; then
    printf '%s\n' "$elapsed" >> "$times"
    success=$((success + 1))
  else
    error=$((error + 1))
  fi
done

if (( success == 0 )); then
  echo "No successful requests (errors=$error)" >&2
  exit 1
fi

p95="$(sort -n "$times" | awk -v n="$success" 'NR == int((n * 95 + 99) / 100) { print; exit }')"
read -r token_count_after token_total_after < <(metric ai.tokens.per.request | python3 -c '
import json,sys
d=json.load(sys.stdin); m={x["statistic"]:x["value"] for x in d.get("measurements",[])}
print(m.get("COUNT",0),m.get("TOTAL",0))')
avg_tokens="$(awk -v c1="$token_count_before" -v c2="$token_count_after" \
  -v t1="$token_total_before" -v t2="$token_total_after" \
  'BEGIN { requests=c2-c1; print (requests > 0 ? (t2-t1)/requests : 0) }')"
fallback_after="$(metric ai.fallback.calls | python3 -c '
import json,sys
d=json.load(sys.stdin)
print(next((x["value"] for x in d.get("measurements",[]) if x["statistic"]=="COUNT"),0))')"
fallbacks="$(awk -v a="$fallback_after" -v b="$fallback_before" 'BEGIN { print a-b }')"
cost_after="$(metric ai.cost.estimated.usd | python3 -c '
import json,sys
d=json.load(sys.stdin)
print(next((x["value"] for x in d.get("measurements",[]) if x["statistic"]=="COUNT"),0))')"
estimated_cost="$(awk -v a="$cost_after" -v b="$cost_before" 'BEGIN { print a-b }')"

printf 'samples=%s success=%s error=%s fallback=%s p95_seconds=%s avg_tokens=%.2f estimated_cost_usd=%.8f\n' \
  "$samples" "$success" "$error" "$fallbacks" "$p95" "$avg_tokens" "$estimated_cost"

awk -v p95="$p95" -v tokens="$avg_tokens" -v limit="$max_tokens" \
  'BEGIN { exit !((p95 + 0) <= 5 && (tokens + 0) <= (limit + 0)) }'
