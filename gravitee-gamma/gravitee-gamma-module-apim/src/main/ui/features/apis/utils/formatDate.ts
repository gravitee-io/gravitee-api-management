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

function parts(value: string): { d: Date; day: string; month: string; year: number } {
    const d = new Date(value);
    return {
        d,
        day: d.getDate().toString().padStart(2, '0'),
        month: d.toLocaleString('en-US', { month: 'short' }),
        year: d.getFullYear(),
    };
}

export function formatDate(value: string | undefined): string {
    if (!value) return '—';
    const { day, month, year } = parts(value);
    return `${day} ${month} ${year}`;
}

export function formatDateTime(value: string | undefined): string {
    if (!value) return '—';
    const { d, day, month, year } = parts(value);
    const hh = d.getHours().toString().padStart(2, '0');
    const mm = d.getMinutes().toString().padStart(2, '0');
    const ss = d.getSeconds().toString().padStart(2, '0');
    return `${day} ${month} ${year} ${hh}:${mm}:${ss}`;
}
