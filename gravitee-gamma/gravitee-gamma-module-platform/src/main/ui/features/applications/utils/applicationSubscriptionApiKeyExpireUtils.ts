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
export function parseDatetimeLocalValue(value: string): Date | null {
    const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value);
    if (!match) {
        return null;
    }

    const [, yearValue, monthValue, dayValue, hourValue, minuteValue] = match;
    const year = Number(yearValue);
    const month = Number(monthValue);
    const day = Number(dayValue);
    const hour = Number(hourValue);
    const minute = Number(minuteValue);
    const date = new Date(year, month - 1, day, hour, minute);

    if (
        date.getFullYear() !== year ||
        date.getMonth() !== month - 1 ||
        date.getDate() !== day ||
        date.getHours() !== hour ||
        date.getMinutes() !== minute
    ) {
        return null;
    }

    return date;
}

export function isAfterMinCandidate(value: string, minMs: number): boolean {
    const date = parseDatetimeLocalValue(value);
    return Boolean(date && date.getTime() > minMs);
}

export function canSubmitApiKeyExpirationChange(dirty: boolean, value: string, minMs: number): boolean {
    return dirty && value !== '' && isAfterMinCandidate(value, minMs);
}
