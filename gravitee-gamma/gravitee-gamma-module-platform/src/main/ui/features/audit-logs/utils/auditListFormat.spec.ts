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

import {
    endOfDayMs,
    formatAuditTargetText,
    formatAuditTargets,
    metadataName,
    resolveAuditDateRange,
    toAuditLogRow,
} from './auditListFormat';
import type { AuditEntity } from '../types/auditLog';

describe('auditListFormat', () => {
    const audit: AuditEntity = {
        id: 'a-1',
        referenceId: 'env-1',
        referenceType: 'ENVIRONMENT',
        user: 'user-1',
        createdAt: 1_700_000_000_000,
        event: 'ENVIRONMENT_UPDATED',
        properties: { USER: 'user-2', ROLE: 'role-1' },
        patch: '[{"op":"replace","path":"/name","value":"Prod"}]',
    };

    const metadata = {
        'USER:user-1:name': 'Ada Lovelace',
        'ENVIRONMENT:env-1:name': 'Production',
        'USER:user-2:name': 'Grace Hopper',
        'ROLE:role-1:name': 'ADMIN',
    };

    it('resolves user, reference, and targets from metadata with raw fallbacks', () => {
        const row = toAuditLogRow(audit, metadata);
        expect(row.user).toBe('Ada Lovelace');
        expect(row.reference).toBe('Production');
        expect(row.targets).toEqual([
            { key: 'USER', value: 'Grace Hopper' },
            { key: 'ROLE', value: 'ADMIN' },
        ]);
        expect(formatAuditTargetText(row.targets)).toBe('USER: Grace Hopper; ROLE: ADMIN');
    });

    it('falls back to ids when metadata is missing', () => {
        const row = toAuditLogRow(audit, {});
        expect(row.user).toBe('user-1');
        expect(row.reference).toBe('env-1');
        expect(formatAuditTargets(audit.properties, {})).toEqual([
            { key: 'USER', value: 'user-2' },
            { key: 'ROLE', value: 'role-1' },
        ]);
        expect(metadataName({}, 'USER:user-1:name')).toBeUndefined();
    });

    it('normalises a null referenceType, which the API emits for unresolved audits', () => {
        const row = toAuditLogRow({ ...audit, referenceType: null }, metadata);
        expect(row.referenceType).toBe('');
        expect(row.reference).toBe('env-1');
    });

    it('maps date presets to from/to epoch milliseconds', () => {
        const now = 1_800_000_000_000;
        expect(resolveAuditDateRange('24h', undefined, now)).toEqual({ from: now - 24 * 60 * 60 * 1000, to: now });
        expect(resolveAuditDateRange('7d', undefined, now).from).toBe(now - 7 * 24 * 60 * 60 * 1000);
        expect(resolveAuditDateRange('', undefined, now)).toEqual({});
    });

    it('uses start-of-range and end-of-day for a custom picker range', () => {
        const from = new Date('2026-08-01T08:00:00');
        const to = new Date('2026-08-03T08:00:00');
        const range = resolveAuditDateRange('custom', { from, to });
        expect(range.from).toBe(from.getTime());
        expect(range.to).toBe(endOfDayMs(to));
    });
});
