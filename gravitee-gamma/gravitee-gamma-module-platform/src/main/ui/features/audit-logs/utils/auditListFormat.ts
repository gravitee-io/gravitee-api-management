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

import type { AuditDatePreset, AuditEntity, AuditLogRow } from '../types/auditLog';

const DAY_MS = 24 * 60 * 60 * 1000;

export function auditTimestamp(createdAt: number | string): number {
    return typeof createdAt === 'number' ? createdAt : Date.parse(createdAt);
}

export function metadataName(metadata: Record<string, unknown>, key: string): string | undefined {
    const value = metadata[key];
    return typeof value === 'string' && value.length > 0 ? value : undefined;
}

export function formatAuditTargets(
    properties: Record<string, string> | undefined,
    metadata: Record<string, unknown>,
): Array<{ key: string; value: string }> {
    return Object.entries(properties ?? {}).map(([key, value]) => ({
        key,
        value: metadataName(metadata, `${key}:${value}:name`) ?? value,
    }));
}

export function formatAuditTargetText(targets: Array<{ key: string; value: string }>): string {
    return targets.map(target => `${target.key}: ${target.value}`).join('; ');
}

export function toAuditLogRow(audit: AuditEntity, metadata: Record<string, unknown>): AuditLogRow {
    return {
        id: audit.id,
        createdAt: auditTimestamp(audit.createdAt),
        user: metadataName(metadata, `USER:${audit.user}:name`) ?? audit.user,
        referenceType: audit.referenceType,
        reference: metadataName(metadata, `${audit.referenceType}:${audit.referenceId}:name`) ?? audit.referenceId,
        event: audit.event,
        targets: formatAuditTargets(audit.properties, metadata),
        patch: audit.patch ?? '',
    };
}

export function prettyPrintPatch(patch: string): string {
    try {
        return JSON.stringify(JSON.parse(patch), null, 2);
    } catch {
        return patch;
    }
}

export function endOfDayMs(date: Date): number {
    const end = new Date(date);
    end.setHours(23, 59, 59, 999);
    return end.getTime();
}

/**
 * Relative presets are computed from `now`. Snapshot `now` (or memoize the result)
 * when wiring this into a query key — calling it on every render with Date.now()
 * will change from/to each time and retrigger fetches forever.
 */
export function resolveAuditDateRange(
    preset: AuditDatePreset,
    customRange: { from?: Date; to?: Date } | undefined,
    now = Date.now(),
): { from?: number; to?: number } {
    if (preset === '24h') {
        return { from: now - DAY_MS, to: now };
    }
    if (preset === '7d') {
        return { from: now - 7 * DAY_MS, to: now };
    }
    if (preset === '30d') {
        return { from: now - 30 * DAY_MS, to: now };
    }
    if (preset === '90d') {
        return { from: now - 90 * DAY_MS, to: now };
    }
    if (preset === 'custom') {
        return {
            from: customRange?.from ? customRange.from.getTime() : undefined,
            to: customRange?.to ? endOfDayMs(customRange.to) : undefined,
        };
    }
    return {};
}
