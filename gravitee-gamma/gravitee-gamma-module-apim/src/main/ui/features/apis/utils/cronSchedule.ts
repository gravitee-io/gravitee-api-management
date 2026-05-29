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
export const CRON_PRESETS = [
    { label: 'Every 5 minutes', value: '0 */5 * * * *' },
    { label: 'Every 10 minutes', value: '0 */10 * * * *' },
    { label: 'Every 30 minutes', value: '0 */30 * * * *' },
    { label: 'Every hour', value: '0 0 * * * *' },
    { label: 'Every 6 hours', value: '0 0 */6 * * *' },
    { label: 'Every day at midnight', value: '0 0 0 * * *' },
] as const;

const CRON_FIELD = /^(\*|[0-9,\-*/]+)$/;

/** Quartz day-of-week: 0 and 7 = Sunday, 1 = Monday, …, 6 = Saturday. */
const DOW_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'] as const;

const MONTH_NAMES = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
] as const;

export function validateCron(value: string): string | null {
    const parts = value.trim().split(/\s+/);
    if (parts.length !== 6) return 'Must have exactly 6 fields: sec min hr dom mon dow';
    for (const part of parts) {
        if (!CRON_FIELD.test(part)) return `Invalid cron field: "${part}"`;
    }
    return null;
}

/** Human-readable summary for a valid 6-field Quartz cron (sec min hr dom mon dow). */
export function describeCronSchedule(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed || validateCron(trimmed)) return null;

    const preset = CRON_PRESETS.find(p => p.value === trimmed);
    if (preset) return preset.label;

    return describeCustomCron(trimmed.split(/\s+/));
}

/** Human-readable text for 6-field Quartz patterns (no third-party cron parser). */
function describeCustomCron(parts: string[]): string | null {
    if (parts.length !== 6) return null;
    const [sec, min, hr, dom, mon, dow] = parts;

    const timePart = describeTimePattern(sec, min, hr);
    const calendarPart = describeCalendarConstraints(dom, mon, dow);

    if (timePart) {
        return calendarPart ? `${timePart}${calendarPart}` : timePart;
    }

    return describeCronFields(parts);
}

function describeTimePattern(sec: string, min: string, hr: string): string | null {
    const everyNMinutes = min.match(/^\*\/(\d+)$/);
    if (sec === '0' && everyNMinutes && hr === '*') {
        const n = everyNMinutes[1];
        return `Every ${n} minute${n === '1' ? '' : 's'}`;
    }

    const everyNHours = hr.match(/^\*\/(\d+)$/);
    if (sec === '0' && min === '0' && everyNHours) {
        const n = everyNHours[1];
        return `Every ${n} hour${n === '1' ? '' : 's'}`;
    }

    if (sec === '0' && min === '0' && hr === '*') {
        return 'Every hour';
    }

    if (sec === '0' && min === '0' && hr === '0') {
        return 'Every day at midnight';
    }

    if (sec === '0' && /^\d+$/.test(min) && /^\d+$/.test(hr)) {
        return `Every day at ${hr.padStart(2, '0')}:${min.padStart(2, '0')}`;
    }

    const everyNSeconds = sec.match(/^\*\/(\d+)$/);
    if (everyNSeconds && min === '*' && hr === '*') {
        const n = everyNSeconds[1];
        return `Every ${n} second${n === '1' ? '' : 's'}`;
    }

    if (sec === '*' && min === '*' && hr === '*') {
        return 'Every second';
    }

    return null;
}

function describeCalendarConstraints(dom: string, mon: string, dow: string): string | null {
    const bits: string[] = [];

    if (dom !== '*') {
        bits.push(describeDom(dom));
    }
    if (mon !== '*') {
        bits.push(describeMon(mon));
    }
    if (dow !== '*') {
        bits.push(describeDow(dow));
    }

    if (bits.length === 0) return null;
    return `, ${bits.join(', ')}`;
}

function describeDom(dom: string): string {
    if (/^\d+$/.test(dom)) return `on day ${dom} of the month`;
    if (dom.includes('-')) return `on days ${dom} of the month`;
    return `on day-of-month ${dom}`;
}

function describeMon(mon: string): string {
    if (/^\d+$/.test(mon)) {
        const idx = parseInt(mon, 10) - 1;
        if (idx >= 0 && idx < 12) return `in ${MONTH_NAMES[idx]}`;
    }
    return `in month ${mon}`;
}

function describeDow(dow: string): string {
    const name = dowName(dow);
    if (name) return `on ${name}`;
    if (dow.includes('-')) return `on weekdays ${dow}`;
    if (dow.includes(',')) return `on days of week ${dow}`;
    return `on day-of-week ${dow}`;
}

function dowName(dow: string): string | null {
    if (/^\d+$/.test(dow)) {
        const n = parseInt(dow, 10);
        if (n === 0 || n === 7) return DOW_NAMES[0];
        if (n >= 1 && n <= 6) return DOW_NAMES[n];
    }
    const upper = dow.toUpperCase();
    const aliases: Record<string, (typeof DOW_NAMES)[number]> = {
        SUN: DOW_NAMES[0],
        MON: DOW_NAMES[1],
        TUE: DOW_NAMES[2],
        WED: DOW_NAMES[3],
        THU: DOW_NAMES[4],
        FRI: DOW_NAMES[5],
        SAT: DOW_NAMES[6],
    };
    return aliases[upper] ?? null;
}

/** Fallback: describe each cron field when no compact time pattern matches. */
function describeCronFields(parts: string[]): string {
    const labels = ['second', 'minute', 'hour', 'day of month', 'month', 'day of week'] as const;
    return parts.map((part, i) => describeCronField(part, labels[i])).join(', ');
}

function describeCronField(part: string, label: string): string {
    if (part === '*') return `every ${label}`;
    const step = part.match(/^\*\/(\d+)$/);
    if (step) return `every ${step[1]} ${label}s`;
    if (part.includes(',')) return `${label} ${part}`;
    if (part.includes('-')) return `${label} ${part}`;
    return `${label} ${part}`;
}
