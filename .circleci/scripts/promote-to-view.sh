#!/usr/bin/env bash
#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Copy of src/scripts/promote-to-view.sh from gravitee-io/gravitee-circleci-orb 6.13.0: APIM does not run the
# orb's publish job. Keep both in step.
#
# Promotes the release just deployed to a view of the Azure Artifacts feed. Customers are given read access to that
# view only, so it must hold what used to go to Maven Central and nothing more:
#   - releases only, never a snapshot;
#   - only from a public repository (private and internal ones are never promoted);
#   - only the modules deployed to the feed, and none that carries a zip: a view holds whole package versions, so
#     promoting a plugin would expose its distribution zip with its jar.
#
# A promotion cannot be undone version by version (only by deleting the whole view), hence the checks above and
# the verification below: each promoted module must resolve through the view before the step succeeds.
#
# Fatal on any error: no scheduled job catches up a release left out of the view.
# Writes one line per module to $OUT/report.tsv: "<groupId:artifactId:version>\t<decision>\t<reason>".
#
# Sourced with PROMOTE_LIB_ONLY=1, it only defines its functions, for the tests.
set -Eeuo pipefail
export LC_ALL=C

VIEW="${PROMOTE_VIEW:-public}"
DRY_RUN="${PROMOTE_DRY_RUN:-false}"
SETTINGS="${MAVEN_SETTINGS:-.gravitee.settings.xml}"
MAVEN_PROFILES="${MAVEN_PROFILES:-gio-artifactory-release,gio-release}"
OUT="${OUT:-/tmp/promote-to-view}"
FEED_MAVEN="${FEED_MAVEN:-https://pkgs.dev.azure.com/graviteeio/packages/_packaging/gravitee}"
FEED_API="${FEED_API:-https://pkgs.dev.azure.com/graviteeio/packages/_apis/packaging/feeds/gravitee/maven}"
EXEC_PLUGIN="org.codehaus.mojo:exec-maven-plugin:3.5.0"
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-120}"
BATCH_SIZE=100

# --- Pure functions (tested) -------------------------------------------------------------------------------------

# Path of a file of a module in the Maven layout.
maven_path() { # <groupId:artifactId:version> <extension>
  local g a v
  IFS=: read -r g a v <<< "$1"
  echo "${g//.//}/$a/$v/$a-$v.$2"
}

# Whether a repository may have its releases promoted, from its GitHub visibility. Prints the reason when it may not.
repository_allows() { # <visibility>
  case "$1" in
    public) return 0 ;;
    private | internal) echo "$1 repository"; return 1 ;;
    *) echo "unknown visibility '$1'"; return 2 ;;
  esac
}

# Decision for one module, from what the feed holds: "promote", or "skip" followed by the reason.
module_decision() { # <version> <pom on the feed: yes|no> <zip on the feed: yes|no>
  if [[ "$1" == *-SNAPSHOT ]]; then echo "skip snapshot"
  elif [ "$2" != yes ]; then echo "skip not deployed to the feed"
  elif [ "$3" = yes ]; then echo "skip carries a zip"
  else echo "promote"
  fi
}

# JSON body of a promotion of the given modules.
batch_body() { # <view> <groupId:artifactId:version>...
  local view=$1
  shift
  printf '%s\n' "$@" | jq -R 'split(":") | {group: .[0], artifact: .[1], version: .[2]}' \
    | jq -s --arg view "$view" '{data: {viewId: $view}, operation: "promote", packages: .}'
}

# --- Remote calls --------------------------------------------------------------------------------------------------

feed_curl() {
  curl -sS -u "ci:${AZURE_ARTIFACTS_PAT}" "$@"
}

# Whether a file exists on the feed or one of its views ("gravitee" or "gravitee@<view>").
exists() { # <feed URL> <path>
  local code
  code=$(feed_curl -o /dev/null -I -w '%{http_code}' "$1/maven/v1/$2")
  case "$code" in
    200) return 0 ;;
    404) return 1 ;;
    *) echo "HEAD $1/maven/v1/$2 answered HTTP $code" >&2; exit 1 ;;
  esac
}

repository_visibility() {
  curl -sSf \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}" \
    | jq -r '.visibility'
}

# Every module of the reactor, as groupId:artifactId:version, with the profiles the deploy used.
reactor_modules() {
  # shellcheck disable=SC2016 # the expressions are for Maven to interpolate, in each module
  mvn -q -B -s "$SETTINGS" -P "$MAVEN_PROFILES" "$EXEC_PLUGIN:exec" \
    -Dexec.executable=echo -Dexec.args='${project.groupId}:${project.artifactId}:${project.version}'
}

promote() { # <groupId:artifactId:version>...
  local response code
  response=$(mktemp)
  code=$(feed_curl -o "$response" -w '%{http_code}' -H "Content-Type: application/json" -X POST \
    "$FEED_API/packagesbatch?api-version=7.1-preview.1" --data "$(batch_body "$VIEW" "$@")")
  if [ "$code" != 202 ] && [ "$code" != 200 ]; then
    echo "The promotion to @$VIEW answered HTTP $code: $(jq -r '.message // empty' "$response" 2>/dev/null)" >&2
    exit 1
  fi
}

# Waits until every module resolves through the view.
verify() { # <groupId:artifactId:version>...
  local deadline=$((SECONDS + VERIFY_TIMEOUT)) gav missing
  while :; do
    missing=()
    for gav in "$@"; do
      exists "$FEED_MAVEN@$VIEW" "$(maven_path "$gav" pom)" || missing+=("$gav")
    done
    [ "${#missing[@]}" -eq 0 ] && return 0
    if [ "$SECONDS" -ge "$deadline" ]; then
      echo "Still missing from @$VIEW after ${VERIFY_TIMEOUT}s: ${missing[*]}" >&2
      exit 1
    fi
    sleep 5
  done
}

# --- Main ----------------------------------------------------------------------------------------------------------

main() {
  mkdir -p "$OUT"
  : > "$OUT/report.tsv"

  local visibility reason
  visibility=$(repository_visibility)
  if ! reason=$(repository_allows "$visibility"); then
    [ "$visibility" = private ] || [ "$visibility" = internal ] || { echo "$reason" >&2; exit 1; }
    echo "Nothing promoted to @$VIEW: $reason."
    return 0
  fi

  # Captured first, so that a failing Maven fails the step instead of yielding an empty list.
  local modules
  modules=$(reactor_modules | sort -u)
  [ -n "$modules" ] || { echo "Maven listed no module." >&2; exit 1; }

  local gav version pom zip decision
  local -a selected=()
  while read -r gav; do
    version=${gav##*:}
    pom=no; zip=no
    if [[ "$version" != *-SNAPSHOT ]]; then
      if exists "$FEED_MAVEN" "$(maven_path "$gav" pom)"; then pom=yes; fi
      if [ "$pom" = yes ] && exists "$FEED_MAVEN" "$(maven_path "$gav" zip)"; then zip=yes; fi
    fi
    decision=$(module_decision "$version" "$pom" "$zip")
    if [ "$decision" = promote ]; then
      printf '%s\tpromote\t\n' "$gav" >> "$OUT/report.tsv"
      selected+=("$gav")
    else
      printf '%s\tskip\t%s\n' "$gav" "${decision#skip }" >> "$OUT/report.tsv"
    fi
  done <<< "$modules"

  cat "$OUT/report.tsv"
  if [ "${#selected[@]}" -eq 0 ]; then
    echo "Nothing to promote to @$VIEW."
    return 0
  fi
  if [ "$DRY_RUN" = true ]; then
    echo "Dry run: ${#selected[@]} module(s) would be promoted to @$VIEW."
    return 0
  fi

  local i
  for ((i = 0; i < ${#selected[@]}; i += BATCH_SIZE)); do
    promote "${selected[@]:i:BATCH_SIZE}"
  done
  verify "${selected[@]}"
  echo "${#selected[@]} module(s) promoted to @$VIEW, each one checked through the view."
}

[ -n "${PROMOTE_LIB_ONLY:-}" ] || main
