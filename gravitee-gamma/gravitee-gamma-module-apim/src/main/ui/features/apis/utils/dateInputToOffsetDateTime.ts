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

/** Converts an HTML date input value (`YYYY-MM-DD`) to an OffsetDateTime ISO string for Management API v2. */
export function dateInputToOffsetDateTime(dateInput: string): string {
    const trimmed = dateInput.trim();
    if (!trimmed) {
        throw new Error('Date is required');
    }
    // Noon UTC avoids accidental day-shift from local timezone when only a calendar date is chosen.
    return new Date(`${trimmed}T12:00:00.000Z`).toISOString();
}

/** Local calendar day (`YYYY-MM-DD`) for an `<input type="date">`. The UTC date prefix is the previous day for evening instants east of UTC. */
export function localCalendarDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export function offsetDateTimeToDateInput(value: string | undefined | null): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return localCalendarDateInput(date);
}

export function localDateInputToday(): string {
    return localCalendarDateInput(new Date());
}
