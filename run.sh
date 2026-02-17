#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# Proje köküne git (script nerede ise orası)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# mvnw varsa onu kullan (daha deterministik), yoksa sistem mvn
MVN="./mvnw"
if [[ ! -x "$MVN" ]]; then
  MVN="mvn"
fi

SKIP_TESTS=false
SPRING_PROFILES_ACTIVE=""

# Basit argümanlar:
#   ./run.sh --skip-tests
#   ./run.sh --profile dev
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --profile)
      SPRING_PROFILES_ACTIVE="$2"
      shift 2
      ;;
    *)
      echo "Bilinmeyen argüman: $1"
      exit 1
      ;;
  esac
done

echo ">>> Clean"
"$MVN" clean

echo ">>> Install"
if [[ "$SKIP_TESTS" == "true" ]]; then
  "$MVN" -DskipTests install
else
  "$MVN" install
fi

# Install sonrası en güvenli çalışma: üretilen JAR'ı koşmak
echo ">>> Run (jar)"
JAR_PATH="$(ls -1 target/*.jar 2>/dev/null | grep -vE 'original-|plain\.jar$' | head -n 1 || true)"

if [[ -z "$JAR_PATH" ]]; then
  echo "target altında runnable jar bulunamadı. spring-boot:run ile deniyorum..."
  if [[ -n "$SPRING_PROFILES_ACTIVE" ]]; then
    "$MVN" -Dspring-boot.run.profiles="$SPRING_PROFILES_ACTIVE" spring-boot:run
  else
    "$MVN" spring-boot:run
  fi
else
  if [[ -n "$SPRING_PROFILES_ACTIVE" ]]; then
    exec java -Dspring.profiles.active="$SPRING_PROFILES_ACTIVE" -jar "$JAR_PATH"
  else
    exec java -jar "$JAR_PATH"
  fi
fi
