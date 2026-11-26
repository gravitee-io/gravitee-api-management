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
import { toDatetimeLocalValue } from '../../../../../../shared/time';

export function currentDatetimeLocalMinute(now = new Date()): string {
    return toDatetimeLocalValue(now);
}

export function offsetToDatetimeLocal(value: string | undefined | null): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return toDatetimeLocalValue(date);
}

export function datetimeLocalToIso(value: string): string {
    return new Date(value).toISOString();
}

// datetime-local is minute precision, so the current minute is still allowed.
export function isBeforeCurrentMinute(value: string, now = new Date()): boolean {
    const trimmed = value.trim();
    if (!trimmed) return false;
    const chosen = new Date(trimmed);
    if (Number.isNaN(chosen.getTime())) return true;
    const minuteStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), now.getMinutes());
    return chosen.getTime() < minuteStart.getTime();
}

export function isEndOnOrBeforeStart(start: string, end: string): boolean {
    if (!start.trim() || !end.trim()) return false;
    const startDate = new Date(start);
    const endDate = new Date(end);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return true;
    return endDate.getTime() <= startDate.getTime();
}

export function endDatetimeLocalMin(start: string, now = new Date()): string {
    const current = currentDatetimeLocalMinute(now);
    if (!start || start < current) return current;
    return start;
}
