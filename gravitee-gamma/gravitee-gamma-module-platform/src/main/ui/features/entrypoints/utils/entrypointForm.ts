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

import type { EntrypointMappingRow, EntrypointTarget } from '../types/entrypoint';

export const KAFKA_DOMAIN_PLACEHOLDER = '{apiHost}';
export const PORT_MIN = 1;
export const PORT_MAX = 65535;
/** Max length of URL (255) - reserved host prefix (63) - separator (1) + `{apiHost}` (10). Mirrors Classic console. */
const KAFKA_DOMAIN_MAX_LENGTH = 201;

export interface EntrypointFormValues {
    httpValue: string;
    tcpPort: string;
    kafkaDomain: string;
    kafkaPort: string;
}

export const EMPTY_ENTRYPOINT_FORM: EntrypointFormValues = {
    httpValue: '',
    tcpPort: '',
    kafkaDomain: '',
    kafkaPort: '',
};

export function isValidPort(raw: string): boolean {
    if (!raw.trim()) return false;
    if (!/^\d+$/.test(raw.trim())) return false;
    const port = Number(raw);
    return port >= PORT_MIN && port <= PORT_MAX;
}

/**
 * Mirrors Classic console `type="url"` on the HTTP entrypoint field: absolute http(s) URLs only.
 * Uses the URL constructor (same approach as Classic `urlValidator`).
 */
export function isValidEntrypointHttpUrl(raw: string): boolean {
    const value = raw.trim();
    if (!value) return false;
    try {
        const url = new URL(value);
        return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
        return false;
    }
}

export function isValidKafkaDomain(raw: string): boolean {
    const domain = raw.trim();
    if (!domain) return false;
    if (!domain.includes(KAFKA_DOMAIN_PLACEHOLDER)) return false;
    return domain.length <= KAFKA_DOMAIN_MAX_LENGTH;
}

export function isEntrypointFormValid(target: EntrypointTarget, form: EntrypointFormValues): boolean {
    if (target === 'HTTP') return isValidEntrypointHttpUrl(form.httpValue);
    if (target === 'TCP') return isValidPort(form.tcpPort);
    return isValidKafkaDomain(form.kafkaDomain) && isValidPort(form.kafkaPort);
}

/** Composes the backend `value` field from per-type form inputs (mirrors Classic console encoding). */
export function composeEntrypointValue(target: EntrypointTarget, form: EntrypointFormValues): string {
    if (target === 'HTTP') return form.httpValue.trim();
    if (target === 'TCP') return form.tcpPort.trim();
    return `${form.kafkaDomain.trim()}:${form.kafkaPort.trim()}`;
}

/** Decomposes the backend `value` field back into per-type form inputs, for editing an existing mapping. */
export function decomposeEntrypointValue(target: EntrypointTarget, value: string): EntrypointFormValues {
    if (target === 'HTTP') return { ...EMPTY_ENTRYPOINT_FORM, httpValue: value };
    if (target === 'TCP') return { ...EMPTY_ENTRYPOINT_FORM, tcpPort: value };
    const separatorIndex = value.lastIndexOf(':');
    if (separatorIndex === -1) return { ...EMPTY_ENTRYPOINT_FORM, kafkaDomain: value };
    return {
        ...EMPTY_ENTRYPOINT_FORM,
        kafkaDomain: value.slice(0, separatorIndex),
        kafkaPort: value.slice(separatorIndex + 1),
    };
}

function environmentsOverlap(a: string[], b: string[]): boolean {
    // An empty environment list means "all environments", which overlaps with any selection.
    if (a.length === 0 || b.length === 0) return true;
    return a.some(id => b.includes(id));
}

/** Best-effort client-side duplicate detection: same type + same composed value + overlapping environments. */
export function findDuplicateMapping(
    target: EntrypointTarget,
    value: string,
    environmentIds: string[],
    rows: EntrypointMappingRow[],
    excludeId?: string,
): EntrypointMappingRow | undefined {
    const normalizedValue = value.trim().toLowerCase();
    return rows.find(
        row =>
            row.id !== excludeId &&
            row.target === target &&
            row.value.trim().toLowerCase() === normalizedValue &&
            environmentsOverlap(row.environmentIds, environmentIds),
    );
}

export interface EntrypointFormSnapshot {
    form: EntrypointFormValues;
    tagKeys: string[];
    environmentIds: string[];
}

function sameStringSet(a: string[], b: string[]): boolean {
    return JSON.stringify([...a].sort()) === JSON.stringify([...b].sort());
}

export function isEntrypointFormDirty(
    target: EntrypointTarget,
    current: EntrypointFormSnapshot,
    initial: EntrypointFormSnapshot | null,
): boolean {
    if (initial === null) return true;
    const currentEncodedValue = composeEntrypointValue(target, current.form);
    const initialEncodedValue = composeEntrypointValue(target, initial.form);
    if (currentEncodedValue !== initialEncodedValue) return true;
    if (!sameStringSet(current.tagKeys, initial.tagKeys)) return true;
    if (!sameStringSet(current.environmentIds, initial.environmentIds)) return true;
    return false;
}
