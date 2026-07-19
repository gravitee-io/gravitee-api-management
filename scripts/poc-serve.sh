#!/usr/bin/env bash
#
# Copyright (C) 2026 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Starts the offline Gamma Console POC stack:
#   - mock management/gamma API
#   - portal-gamma Module Federation remote (:4103)
#   - APIM Module Federation remote (:3001)
#   - gamma-console host (:4200)
#
# Remotes are started one at a time so concurrent `nx serve` processes do not
# deadlock on Nx daemon / project-graph construction.
#
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export POC_MOCK_PORT="${POC_MOCK_PORT:-18083}"
export POC_MODE=true
export NX_TUI=false

PIDS=()
STOPPING=0

port_pids() {
    local port="$1"
    lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true
}

is_listening() {
    local port="$1"
    [[ -n "$(port_pids "${port}")" ]]
}

free_port() {
    local port="$1"
    local pids
    pids="$(port_pids "${port}")"
    if [[ -z "${pids}" ]]; then
        return 0
    fi

    echo "Port ${port} is in use — stopping existing listener(s)..."
    # shellcheck disable=SC2086
    kill ${pids} 2>/dev/null || true

    local i
    for i in $(seq 1 20); do
        if ! is_listening "${port}"; then
            return 0
        fi
        sleep 0.25
    done

    pids="$(port_pids "${port}")"
    if [[ -n "${pids}" ]]; then
        echo "Port ${port} still busy — force killing..."
        # shellcheck disable=SC2086
        kill -KILL ${pids} 2>/dev/null || true
        sleep 0.5
    fi

    if is_listening "${port}"; then
        echo "ERROR: could not free port ${port}" >&2
        return 1
    fi
}

wait_for_port() {
    local port="$1"
    local label="$2"
    local timeout_s="${3:-180}"
    local i

    echo "Waiting for ${label} on :${port}..."
    for i in $(seq 1 "${timeout_s}"); do
        if is_listening "${port}"; then
            echo "${label} is ready on :${port}"
            return 0
        fi
        sleep 1
    done

    echo "ERROR: ${label} did not open port ${port} within ${timeout_s}s" >&2
    return 1
}

kill_tree() {
    local pid="$1"
    local children
    children="$(pgrep -P "${pid}" 2>/dev/null || true)"
    for child in ${children}; do
        kill_tree "${child}"
    done
    kill -TERM "${pid}" 2>/dev/null || true
}

cleanup() {
    if [[ "${STOPPING}" -eq 1 ]]; then
        return
    fi
    STOPPING=1
    trap - INT TERM EXIT

    echo ""
    echo "Stopping POC stack..."

    for pid in "${PIDS[@]}"; do
        kill_tree "${pid}"
    done

    sleep 0.5

    for pid in "${PIDS[@]}"; do
        kill -KILL "${pid}" 2>/dev/null || true
    done

    wait 2>/dev/null || true
}

on_signal() {
    cleanup
    exit 130
}

trap on_signal INT TERM
trap cleanup EXIT

start_nx_serve() {
    local project="$1"
    local label="$2"
    echo "Starting ${label}..."
    (
        cd "${ROOT}"
        exec yarn nx serve "${project}"
    ) &
    PIDS+=($!)
}

free_port "${POC_MOCK_PORT}" || exit 1
free_port 4103 || exit 1
free_port 3001 || exit 1
free_port 4200 || exit 1

echo "Warming Nx daemon (avoids parallel graph-construction deadlocks)..."
yarn nx daemon --start >/dev/null 2>&1 || true
# Touch the graph once in the foreground before spawning parallel serves.
yarn nx show projects >/dev/null

echo "Starting POC mock API on :${POC_MOCK_PORT}..."
node poc-mock/server.mjs &
PIDS+=($!)
wait_for_port "${POC_MOCK_PORT}" "POC mock API" 30 || exit 1

# Start remotes sequentially and wait until each is listening before the next.
# Concurrent `nx serve` processes often hang with:
#   "Waiting for graph construction in another process to complete"
#   "Failed to start plugin worker"
start_nx_serve portal-gamma "portal-gamma"
wait_for_port 4103 "portal-gamma" 180 || exit 1

start_nx_serve gravitee-gamma-module-apim "APIM module"
wait_for_port 3001 "APIM module" 180 || exit 1

echo "Starting gamma-console on :4200..."
echo "Open http://localhost:4200 — press Ctrl+C to stop all services."

# Foreground (no exec) so EXIT/INT traps still clean up remotes + mock.
yarn nx serve gamma-console
console_exit=$?

cleanup
exit "${console_exit}"
