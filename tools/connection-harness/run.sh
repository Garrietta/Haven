#!/usr/bin/env bash
# Connection-engine harness (reliability round, 2026-11-26).
#
# Compiles the REAL data-layer sources (HaClient, ConnectionPlanner,
# TokenExchange, ...) against tiny Android / media3 stubs and a
# scriptable fake OkHttp, then runs EngineTest: endpoint fallback,
# stale-socket isolation, make-before-break switch-back, auth
# rejection, offline pause, ping/pong, REST 401 renewal, Bearer
# scoping, todo payloads. No Android SDK or device needed — just a
# JDK, a kotlinc distribution, JUnit 4 + Hamcrest, and an org.json jar.
#
#   KOTLINC=/path/to/kotlinc JUNIT_CP=/path/junit-4.13.2.jar:/path/hamcrest-core-1.3.jar \
#   JSON_JAR=/path/json-20240303.jar tools/connection-harness/run.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
D="$ROOT/app/src/main/java/com/garrietta/haven/data"
: "${KOTLINC:?set KOTLINC to a kotlinc distribution directory}"
: "${JUNIT_CP:?set JUNIT_CP to junit + hamcrest jars}"
: "${JSON_JAR:?set JSON_JAR to an org.json jar}"
OUT="$(mktemp -d)"
CO="$KOTLINC/lib/kotlinx-coroutines-core-jvm.jar"
"$KOTLINC/bin/kotlinc" -jvm-target 17 -cp "$CO:$JSON_JAR" -d "$OUT/main" \
  "$HERE"/stubs/*.kt \
  "$D/HaClient.kt" "$D/ConnectionPlanner.kt" "$D/TokenExchange.kt" "$D/Round16Logic.kt" \
  "$D/Models.kt" "$D/CameraStream.kt" "$D/DeviceSearch.kt" "$D/HomePrefs.kt" \
  "$D/NotificationRules.kt" "$D/OptimisticStateGuard.kt"
"$KOTLINC/bin/kotlinc" -jvm-target 17 -cp "$OUT/main:$CO:$JSON_JAR:$JUNIT_CP" -d "$OUT/test" \
  "$HERE/EngineTest.kt" "$ROOT/app/src/test/java/com/garrietta/haven/data/ConnectionPlannerTest.kt"
java -cp "$OUT/test:$OUT/main:$KOTLINC/lib/kotlin-stdlib.jar:$CO:$JSON_JAR:$JUNIT_CP" \
  org.junit.runner.JUnitCore com.garrietta.haven.data.EngineTest com.garrietta.haven.data.ConnectionPlannerTest
