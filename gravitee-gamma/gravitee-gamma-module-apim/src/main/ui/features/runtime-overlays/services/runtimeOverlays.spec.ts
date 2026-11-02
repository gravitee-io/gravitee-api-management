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
import { type RuntimeOverlay, liveStatus } from './runtimeOverlays';

const overlay = (status: RuntimeOverlay['status']): RuntimeOverlay => ({
    id: 'overlay-id',
    kind: 'demo',
    apiId: 'api-id',
    startAt: 1_000,
    endAt: 61_000,
    status,
    payload: {},
});

describe('liveStatus', () => {
    it('should keep an active overlay active before its end', () => {
        expect(liveStatus(overlay('ACTIVE'), 60_999)).toBe('ACTIVE');
    });

    it('should show an active overlay as expired once its end has passed, before the next poll', () => {
        expect(liveStatus(overlay('ACTIVE'), 61_000)).toBe('EXPIRED');
    });

    it('should keep a stopped overlay stopped', () => {
        expect(liveStatus(overlay('STOPPED'), 0)).toBe('STOPPED');
    });
});
