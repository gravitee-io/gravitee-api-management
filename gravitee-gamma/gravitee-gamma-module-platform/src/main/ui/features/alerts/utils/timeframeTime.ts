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

/** Classic API `beginHour` / `endHour`: seconds since midnight. */
export const OFFICE_START_SECONDS = 9 * 3600;
export const OFFICE_END_SECONDS = 18 * 3600;
export const END_OF_DAY_SECONDS = 24 * 3600 - 1;

function pad2(n: number): string {
    return String(n).padStart(2, '0');
}

export function secondsSinceMidnightToTimeInput(seconds: number): string {
    const clamped = Number.isFinite(seconds) ? Math.max(0, Math.min(END_OF_DAY_SECONDS, Math.floor(seconds))) : 0;
    const hours = Math.floor(clamped / 3600);
    const minutes = Math.floor((clamped % 3600) / 60);
    const secs = clamped % 60;
    return `${pad2(hours)}:${pad2(minutes)}:${pad2(secs)}`;
}

export function timeInputToSecondsSinceMidnight(value: string): number {
    const [hoursPart = '0', minutesPart = '0', secondsPart = '0'] = value.split(':');
    const hours = Number(hoursPart);
    const minutes = Number(minutesPart);
    const seconds = Number(secondsPart);
    if (![hours, minutes, seconds].every(n => Number.isFinite(n))) {
        return 0;
    }
    return Math.min(END_OF_DAY_SECONDS, hours * 3600 + minutes * 60 + seconds);
}

export function isOfficeHours(beginSeconds: number, endSeconds: number): boolean {
    return beginSeconds === OFFICE_START_SECONDS && endSeconds === OFFICE_END_SECONDS;
}
