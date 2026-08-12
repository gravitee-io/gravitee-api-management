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

const SIZE_UNITS = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'] as const;

const MS_PER_SECOND = 1_000;
const MS_PER_MINUTE = 60 * MS_PER_SECOND;
const MS_PER_HOUR = 60 * MS_PER_MINUTE;
const MS_PER_DAY = 24 * MS_PER_HOUR;

/** Classic: InstanceDetailsMonitoringComponent.humanizeSize */
export function humanizeSize(bytes: number, precision = 1): string {
    if (typeof bytes !== 'number' || !Number.isFinite(bytes)) {
        return '-';
    }

    const unitIndex = bytes === 0 ? 0 : Math.floor(Math.log(bytes) / Math.log(1024));
    const valueInSelectedUnit = bytes / 1024 ** unitIndex;
    return `${valueInSelectedUnit.toFixed(precision)} ${SIZE_UNITS[unitIndex]}`;
}

/** Classic: InstanceDetailsMonitoringComponent.ratio */
export function ratio(value: number, value2: number): number | undefined {
    return value2 === 0 ? undefined : Math.round((value / value2) * 100);
}

/** Classic: InstanceDetailsMonitoringComponent.ratioLabel */
export function ratioLabel(value: number, value2: number): string {
    const computedRatio = ratio(value, value2);
    return computedRatio !== undefined ? `${computedRatio}%` : '-%';
}

function pluralize(count: number, singular: string, plural: string): string {
    return count === 1 ? singular : plural;
}

/** Positive uptime phrasing for Gamma (Classic uses moment duration humanize). */
export function humanizeUptime(uptimeMillis: number): string {
    if (typeof uptimeMillis !== 'number' || !Number.isFinite(uptimeMillis)) {
        return '-';
    }

    const totalSeconds = Math.floor(uptimeMillis / MS_PER_SECOND);
    if (totalSeconds < 60) {
        return 'a few seconds';
    }

    const days = Math.floor(uptimeMillis / MS_PER_DAY);
    const hours = Math.floor((uptimeMillis % MS_PER_DAY) / MS_PER_HOUR);
    const minutes = Math.floor((uptimeMillis % MS_PER_HOUR) / MS_PER_MINUTE);

    if (days > 0) {
        const dayLabel = `${days} ${pluralize(days, 'day', 'days')}`;
        if (hours > 0) {
            return `${dayLabel} ${hours} ${pluralize(hours, 'hour', 'hours')}`;
        }
        return dayLabel;
    }

    if (hours > 0) {
        const hourLabel = `${hours} ${pluralize(hours, 'hour', 'hours')}`;
        if (minutes > 0) {
            return `${hourLabel} ${minutes} ${pluralize(minutes, 'minute', 'minutes')}`;
        }
        return hourLabel;
    }

    return `${minutes} ${pluralize(minutes, 'minute', 'minutes')}`;
}
