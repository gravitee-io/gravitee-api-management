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

import { buildInformationRows } from './buildInformationRows';
import type { Instance } from '../types/instance';

const BASE: Instance = {
    id: 'gw-1',
    event: 'event-1',
    hostname: 'apim-gateway',
    ip: '10.0.0.22',
    port: '8082',
    version: '4.13.0',
    state: 'STARTED',
    started_at: Date.parse('2026-05-07T09:43:26Z'),
    last_heartbeat_at: Date.parse('2026-05-07T10:57:51Z'),
};

describe('buildInformationRows', () => {
    it('builds core information rows from the instance', () => {
        const rows = buildInformationRows(BASE);
        expect(rows.map(r => r.type)).toEqual(['Hostname', 'IP', 'Port', 'State', 'Version', 'Started at', 'Last heartbeat at']);
        expect(rows.find(r => r.type === 'State')).toMatchObject({ value: 'STARTED', tone: 'success' });
        expect(rows.find(r => r.type === 'Hostname')?.value).toBe('apim-gateway');
    });

    it('appends optional fields when present', () => {
        const rows = buildInformationRows({
            ...BASE,
            operating_system_name: 'Linux',
            tags: ['external', 'partner'],
            tenant: 'europe',
            organizations_hrids: ['DEFAULT'],
            environments_hrids: ['default'],
            stopped_at: Date.parse('2026-05-07T12:00:00Z'),
        });
        expect(rows.map(r => r.type)).toEqual(
            expect.arrayContaining(['OS', 'Sharding tags', 'Tenant', 'Organizations', 'Environments', 'Stopped at']),
        );
        expect(rows.find(r => r.type === 'Sharding tags')?.value).toBe('external, partner');
    });

    it('marks STOPPED state with danger tone', () => {
        const rows = buildInformationRows({ ...BASE, state: 'STOPPED' });
        expect(rows.find(r => r.type === 'State')).toMatchObject({ value: 'STOPPED', tone: 'danger', icon: 'state-stopped' });
    });
});
