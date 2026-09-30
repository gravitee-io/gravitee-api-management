/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Matches java.time.Duration.parse grammar: P[nD]T[nH][nM][n.nS] — no years, months, or weeks. */
const ISO8601_DURATION = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/;

function toNumber(value: string | undefined): number {
    return value ? Number(value) : 0;
}

/** Parses ISO-8601 durations used by API logging sampling (e.g. PT1S). Returns null when invalid or shorter than one second. */
export function parseIso8601DurationSeconds(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
        return null;
    }

    const match = ISO8601_DURATION.exec(trimmed);
    if (!match) {
        return null;
    }

    const days = toNumber(match[1]);
    const hours = toNumber(match[2]);
    const minutes = toNumber(match[3]);
    const seconds = toNumber(match[4]);

    if (days + hours + minutes + seconds === 0) {
        return null;
    }

    const totalSeconds = seconds + minutes * 60 + hours * 3600 + days * 86_400;
    if (totalSeconds < 1) {
        return null;
    }

    return totalSeconds;
}

export function isValidIso8601Duration(value: string): boolean {
    return parseIso8601DurationSeconds(value) !== null;
}

/** Calendar periods Classic accepts via moment but java.time.Duration does not (P1Y, P1M, P1W). */
const CLASSIC_MOMENT_CALENDAR_PERIOD = /^P(\d+)(Y|M|W)$/;

/**
 * Seconds for temporal default-vs-limit compare. Returns null for calendar periods Classic accepts but this
 * grammar rejects, so compare is skipped and only the format error is shown. Other unparseable values coerce
 * to 0 (e.g. PT1SS) so format and compare errors can appear together like Classic on malformed input.
 */
export function parseIso8601DurationSecondsForCompare(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
        return null;
    }
    if (CLASSIC_MOMENT_CALENDAR_PERIOD.test(trimmed)) {
        return null;
    }
    return parseIso8601DurationSeconds(trimmed) ?? 0;
}
