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

import { mapInstanceListItem, shortInstanceVersion } from './mapInstanceListItem';
import type { InstanceListItem } from '../types/instance';

describe('shortInstanceVersion', () => {
    it('truncates at the first parenthesis like classic', () => {
        expect(shortInstanceVersion('4.12.13 (build: 1959495) revision#abc')).toBe('4.12.13');
    });

    it('returns the version unchanged when there is no build suffix', () => {
        expect(shortInstanceVersion('4.12.13')).toBe('4.12.13');
    });

    it('handles null/undefined', () => {
        expect(shortInstanceVersion(null)).toBe('');
        expect(shortInstanceVersion(undefined)).toBe('');
    });
});

describe('mapInstanceListItem', () => {
    const base: InstanceListItem = {
        event: 'event-1',
        id: 'gateway-1',
        started_at: 1,
        last_heartbeat_at: 1_704_000_000_000,
        hostname: 'apim-gateway',
        ip: '172.18.0.3',
        port: '8082',
        version: '4.12.13 (build: 1)',
        tags: ['prod', 'eu'],
        tenant: 'tenant-a',
        state: 'STARTED',
        operating_system_name: 'Linux',
    };

    it('maps classic list fields and uses event id for navigation', () => {
        const row = mapInstanceListItem(base);
        expect(row.id).toBe('event-1');
        expect(row.hostname).toBe('apim-gateway');
        expect(row.version).toBe('4.12.13');
        expect(row.state).toBe('STARTED');
        expect(row.os).toBe('Linux');
        expect(row.ip).toBe('172.18.0.3');
        expect(row.port).toBe('8082');
        expect(row.tenant).toBe('tenant-a');
        expect(row.tags).toEqual(['prod', 'eu']);
        expect(row.lastHeartbeat).toEqual(new Date(1_704_000_000_000));
    });

    it('falls back to instance id when event is missing', () => {
        const row = mapInstanceListItem({ ...base, event: '' });
        expect(row.id).toBe('gateway-1');
    });
});
