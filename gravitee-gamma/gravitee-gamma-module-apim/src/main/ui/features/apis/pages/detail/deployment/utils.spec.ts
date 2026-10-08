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
import { chronologicalEvents, hideCompareRollback } from './utils';
import type { ApiEvent } from '../../../types';

function event(id: string, createdAt: string): ApiEvent {
    return { id, createdAt, payload: '{}', initiator: { id: 'u', displayName: 'Admin' }, properties: { DEPLOYMENT_NUMBER: id } };
}

describe('chronologicalEvents', () => {
    it('puts the older event first when the newer row was ticked first', () => {
        const older = event('old', '2026-01-01T00:00:00Z');
        const newer = event('new', '2026-02-01T00:00:00Z');
        expect(chronologicalEvents([newer, older])).toEqual([older, newer]);
    });
});

describe('hideCompareRollback', () => {
    const live = event('live', '2026-02-01T00:00:00Z');
    const older = event('old', '2026-01-01T00:00:00Z');

    it('hides rollback for the live version only when the API is in sync', () => {
        const inSync = { liveEventId: 'live', needsRedeploy: false, isNative: false };
        expect(hideCompareRollback(live, inSync)).toBe(true);
        expect(hideCompareRollback(older, inSync)).toBe(false);
        const outOfSync = { ...inSync, needsRedeploy: true };
        expect(hideCompareRollback(live, outOfSync)).toBe(false);
    });

    it('hides rollback on a native API', () => {
        expect(hideCompareRollback(older, { liveEventId: 'live', needsRedeploy: true, isNative: true })).toBe(true);
    });
});
