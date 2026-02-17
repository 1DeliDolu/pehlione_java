#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PUBLIC_SPEC="api-contract/openapi-public.json"
ADMIN_SPEC="api-contract/openapi-admin.json"

if [[ ! -f "$PUBLIC_SPEC" || ! -f "$ADMIN_SPEC" ]]; then
  echo "OpenAPI snapshot files are missing under api-contract/."
  echo "Run scripts/update-openapi-contract.sh first."
  exit 1
fi

rm -rf clients/ts-public clients/ts-admin

npx openapi-generator-cli generate \
  -i "$PUBLIC_SPEC" \
  -g typescript-fetch \
  -o clients/ts-public \
  --additional-properties=supportsES6=true,npmName=@pehlione/api-public,typescriptThreePlus=true

npx openapi-generator-cli generate \
  -i "$ADMIN_SPEC" \
  -g typescript-fetch \
  -o clients/ts-admin \
  --additional-properties=supportsES6=true,npmName=@pehlione/api-admin,typescriptThreePlus=true
