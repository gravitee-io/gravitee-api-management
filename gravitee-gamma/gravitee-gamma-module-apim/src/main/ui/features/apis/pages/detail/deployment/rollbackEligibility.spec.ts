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
import { canRollbackTo, isNativeApi } from './utils';
import type { ApiDetailDto, ApiEvent } from '../../../types';

const event = (id: string): ApiEvent => ({
    id,
    createdAt: '2026-10-01T10:00:00Z',
    payload: '{}',
    initiator: { id: 'u1', displayName: 'admin' },
    properties: { DEPLOYMENT_NUMBER: '1' },
});

describe('isNativeApi', () => {
    it('recognizes a native API as the management API returns it, a V4 API of type NATIVE', () => {
        expect(isNativeApi({ definitionVersion: 'V4', type: 'NATIVE' } as ApiDetailDto)).toBe(true);
    });

    it('does not treat other V4 APIs as native', () => {
        expect(isNativeApi({ definitionVersion: 'V4', type: 'PROXY' } as ApiDetailDto)).toBe(false);
        expect(isNativeApi(null)).toBe(false);
    });
});

describe('canRollbackTo', () => {
    const live = event('live');
    const old = event('old');

    it('allows rolling back to an older version', () => {
        expect(canRollbackTo(old, { liveEventId: live.id, isNative: false, needsRedeploy: false })).toBe(true);
    });

    it('does not offer the version already in use', () => {
        expect(canRollbackTo(live, { liveEventId: live.id, isNative: false, needsRedeploy: false })).toBe(false);
    });

    it('offers the version in use when the API has undeployed changes to discard', () => {
        expect(canRollbackTo(live, { liveEventId: live.id, isNative: false, needsRedeploy: true })).toBe(true);
    });

    it('never offers a rollback for a native API', () => {
        expect(canRollbackTo(old, { liveEventId: live.id, isNative: true, needsRedeploy: true })).toBe(false);
    });
});
