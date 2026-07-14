#!/usr/bin/env bash
#
# AI Product POC — build AIM + APIM + Docker images, optionally push to ACR.
#
# Usage:
#   cp build.conf.example build.conf   # optional overrides
#   ./build-and-push.sh                # full local build
#   ./build-and-push.sh --push         # build + docker push to ACR
#   ./build-and-push.sh --docker-only  # skip Maven, build images from existing target/
#   ./build-and-push.sh --pack         # also create ai-product-poc.tar for SEs
#
# Prerequisites:
#   JDK 21, Maven 3.9+, Node 22 + Yarn 4 (corepack), Docker
#   docker login graviteeio.azurecr.io
#   Internal Maven repo access (EE plugins: token-ratelimit, etc.)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APIM_DIR="${APIM_DIR:-$(cd "$SCRIPT_DIR/.." && pwd)}"

# shellcheck source=/dev/null
[[ -f "$SCRIPT_DIR/build.conf" ]] && source "$SCRIPT_DIR/build.conf"

AIM_DIR="${AIM_DIR:-$HOME/IdeaProjects/gravitee-plugins/gravitee-gamma-module-aim}"
LLM_PROXY_DIR="${LLM_PROXY_DIR:-$HOME/IdeaProjects/gravitee-reactor-llm-proxy}"
APIM_BRANCH="${APIM_BRANCH:-poc-ai-products}"
AIM_BRANCH="${AIM_BRANCH:-poc/ai-products}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-graviteeio.azurecr.io}"
DOCKER_TAG="${DOCKER_TAG:-ai-product-poc}"
AIM_VERSION="${AIM_VERSION:-1.0.0-poc-ai-products-SNAPSHOT}"

DO_PUSH=0
DO_PACK=0
DOCKER_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --push) DO_PUSH=1 ;;
    --pack) DO_PACK=1 ;;
    --docker-only) DOCKER_ONLY=1 ;;
    -h|--help)
      sed -n '2,20p' "$0"
      exit 0
      ;;
    *) echo "Unknown arg: $arg (try --help)" >&2; exit 1 ;;
  esac
done

say() { printf '\n\033[1m▸ %s\033[0m\n' "$*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"
}

checkout_branch() {
  local dir="$1" branch="$2"
  [[ -d "$dir/.git" ]] || die "Not a git repo: $dir"
  local current
  current="$(git -C "$dir" branch --show-current 2>/dev/null || true)"
  if [[ "$current" != "$branch" ]]; then
    say "Checking out $branch in $(basename "$dir")"
    git -C "$dir" checkout "$branch"
  fi
}

# AIM compiles against gravitee-gamma-definition-model from ~/.m2. A stale local
# 4.13.0-SNAPSHOT.jar (e.g. from `mvn install` of APIM's definition module) lacks
# io.gravitee.gamma.definition.entityid and breaks the plugin build.
refresh_aim_gamma_definition() {
  local repo="${HOME}/.m2/repository/io/gravitee/gamma/definition/gravitee-gamma-definition-model/4.13.0-SNAPSHOT"
  local pinned="4.13.0-20260630.155342-284"
  local good="${repo}/gravitee-gamma-definition-model-${pinned}.jar"
  if [[ ! -f "$good" ]]; then
    say "Fetching gamma-definition-model ${pinned} for AIM compile"
    mvn -q dependency:get -Dartifact=io.gravitee.gamma.definition:gravitee-gamma-definition-model:${pinned}
  fi
  cp -f "$good" "${repo}/gravitee-gamma-definition-model-4.13.0-SNAPSHOT.jar"
}

build_aim() {
  say "Building AIM ($AIM_VERSION) with AI Products UI"
  checkout_branch "$AIM_DIR" "$AIM_BRANCH"
  cd "$AIM_DIR"
  corepack enable 2>/dev/null || true
  yarn install --immutable 2>/dev/null || yarn install
  yarn build
  refresh_aim_gamma_definition
  mvn install -Dmaven.test.skip=true -Dskip.ui.build=true -q
  local zip
  zip="$(ls -1 target/gravitee-gamma-module-aim-*.zip | tail -1)"
  say "AIM plugin: $zip"
}

build_llm_proxy() {
  say "Building gravitee-reactor-llm-proxy plugins"
  [[ -d "$LLM_PROXY_DIR" ]] || die "LLM proxy repo not found: $LLM_PROXY_DIR"
  cd "$LLM_PROXY_DIR"
  mvn clean package -Dmaven.test.skip=true -Dskip.validation -q
}

build_apim() {
  say "Building APIM ($APIM_BRANCH) with AIM $AIM_VERSION"
  checkout_branch "$APIM_DIR" "$APIM_BRANCH"
  cd "$APIM_DIR"
  # Orphan test classes (e.g. AiWorkspace*) can survive in jdbc target and break default-jdbc-test.
  rm -rf gravitee-apim-repository/gravitee-apim-repository-jdbc/target/test-classes/io/gravitee/repository/management/AiWorkspace*.class 2>/dev/null || true
  mvn clean install \
    -Dmaven.test.skip=true \
    -DskipTests=true \
    -Dskip.validation=true \
    -Dgravitee-gamma-module-aim.version="$AIM_VERSION" \
    -Dtest='!FlowRepositoryPKTest,!TableConstraintsTest,!FlowRepositoryTest,!AiWorkspaceComponentRepositoryTest' \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -q
}

build_docker() {
  say "Building Docker images → $DOCKER_REGISTRY (tag: $DOCKER_TAG)"
  cd "$APIM_DIR"

  local gw_dist="$APIM_DIR/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target"
  local api_dist="$APIM_DIR/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target"

  [[ -d "$gw_dist/distribution" ]] || die "Gateway distribution missing — run APIM build first"
  [[ -d "$api_dist/distribution" ]] || die "Management API distribution missing — run APIM build first"

  copy_poc_plugins() {
    local dest="$1"
    local with_reactor="${2:-0}"
    mkdir -p "$dest"

    if [[ -d "$LLM_PROXY_DIR" ]]; then
      if [[ "$with_reactor" -eq 1 ]]; then
        cp "$LLM_PROXY_DIR"/gravitee-reactor-llm-proxy/target/gravitee-reactor-llm-proxy-*.zip "$dest/" 2>/dev/null || true
      fi
      cp "$LLM_PROXY_DIR"/gravitee-entrypoint-llm-proxy/target/gravitee-entrypoint-llm-proxy-*.zip "$dest/" 2>/dev/null || true
      cp "$LLM_PROXY_DIR"/gravitee-endpoint-llm-proxy/target/gravitee-endpoint-llm-proxy-*.zip "$dest/" 2>/dev/null || true
    fi

    local token_rl
    token_rl="$(ls -1 "$HOME/.m2/repository/com/graviteesource/policy/gravitee-policy-token-ratelimit"/*/gravitee-policy-token-ratelimit-*.zip 2>/dev/null | tail -1)"
    [[ -n "$token_rl" ]] && cp "$token_rl" "$dest/"
  }

  copy_poc_plugins "$gw_dist/distribution/plugins" 1
  copy_poc_plugins "$api_dist/distribution/plugins" 0
  say "Baked LLM proxy + token-ratelimit plugins into gateway and management-api images"

  # Bake POC AIM plugin (AI Products UI) into management-api image when present in ~/.m2 or AIM target.
  local aim_zip=""
  aim_zip="$(ls -1 "$AIM_DIR"/target/gravitee-gamma-module-aim-*.zip 2>/dev/null | tail -1)"
  if [[ -z "$aim_zip" ]]; then
    aim_zip="$(ls -1 "$HOME/.m2/repository/com/graviteesource/gamma/module/gravitee-gamma-module-aim/${AIM_VERSION}/gravitee-gamma-module-aim-${AIM_VERSION}.zip" 2>/dev/null | tail -1)"
  fi
  if [[ -n "$aim_zip" ]]; then
    cp "$aim_zip" "$api_dist/distribution/plugins/"
    say "Baked AIM into management-api distribution: $(basename "$aim_zip")"
  else
    say "WARN: AIM zip not found — rebuild AIM or use management-api image from ACR"
  fi

  docker build \
    -t "$DOCKER_REGISTRY/apim-gateway:$DOCKER_TAG" \
    -f gravitee-apim-gateway/docker/Dockerfile \
    "$gw_dist"

  docker build \
    -t "$DOCKER_REGISTRY/apim-management-api:$DOCKER_TAG" \
    -f gravitee-apim-rest-api/docker/Dockerfile \
    "$api_dist"

  say "Built:"
  docker images --format '  {{.Repository}}:{{.Tag}}  ({{.Size}})' \
    | grep -E "${DOCKER_REGISTRY}/apim-(gateway|management-api):${DOCKER_TAG}" || true
}

push_docker() {
  say "Pushing to $DOCKER_REGISTRY (tag: $DOCKER_TAG)"
  docker push "$DOCKER_REGISTRY/apim-gateway:$DOCKER_TAG"
  docker push "$DOCKER_REGISTRY/apim-management-api:$DOCKER_TAG"
}

sync_stack_plugin() {
  # stack/plugins is mounted on gateway only — gateway plugins (not AIM; AIM is baked into management-api).
  mkdir -p "$SCRIPT_DIR/stack/plugins"
  rm -f "$SCRIPT_DIR/stack/plugins"/gravitee-gamma-module-aim-*.zip 2>/dev/null || true
  rm -rf "$SCRIPT_DIR/stack/plugins/.work" 2>/dev/null || true

  if [[ -d "$LLM_PROXY_DIR" ]]; then
    cp "$LLM_PROXY_DIR"/gravitee-reactor-llm-proxy/target/gravitee-reactor-llm-proxy-*.zip "$SCRIPT_DIR/stack/plugins/" 2>/dev/null || true
    cp "$LLM_PROXY_DIR"/gravitee-entrypoint-llm-proxy/target/gravitee-entrypoint-llm-proxy-*.zip "$SCRIPT_DIR/stack/plugins/" 2>/dev/null || true
    cp "$LLM_PROXY_DIR"/gravitee-endpoint-llm-proxy/target/gravitee-endpoint-llm-proxy-*.zip "$SCRIPT_DIR/stack/plugins/" 2>/dev/null || true
  fi
  local token_rl
  token_rl="$(ls -1 "$HOME/.m2/repository/com/graviteesource/policy/gravitee-policy-token-ratelimit"/*/gravitee-policy-token-ratelimit-*.zip 2>/dev/null | tail -1)"
  [[ -n "$token_rl" ]] && cp "$token_rl" "$SCRIPT_DIR/stack/plugins/"
  say "Synced gateway plugins to stack/plugins/ (LLM proxy + token-ratelimit)"
}

pack_tar() {
  say "Packing ai-product-poc.tar"
  local out="$SCRIPT_DIR/ai-product-poc.tar"
  tar -cvf "$out" -C "$SCRIPT_DIR" stack
  say "Created $out ($(du -h "$out" | cut -f1))"
}

# ── main ──────────────────────────────────────────────────────────────────────

require_cmd docker
require_cmd mvn
require_cmd java

if [[ "$DOCKER_ONLY" -eq 0 ]]; then
  [[ "${SKIP_AIM:-0}" -eq 0 ]] && build_aim
  [[ "${SKIP_LLM_PROXY:-0}" -eq 0 ]] && build_llm_proxy
  [[ "${SKIP_APIM:-0}" -eq 0 ]] && build_apim
  sync_stack_plugin
fi

[[ "${SKIP_DOCKER:-0}" -eq 0 ]] && build_docker

if [[ "$DO_PUSH" -eq 1 ]]; then
  push_docker
  say "Teammates can run:"
  echo "  cd ai-poc-demo/stack"
  echo "  export POC_IMAGE_TAG=$DOCKER_TAG"
  echo "  docker compose up -d"
  echo "  → http://localhost:8085"
fi

[[ "$DO_PACK" -eq 1 ]] && pack_tar

say "Done."
