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

import { buildPrimaryOwnerModeSavePayload } from './buildPrimaryOwnerModeSavePayload';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

const CURRENT: PortalSettings = {
    company: { name: 'Acme' },
    plan: { security: { apikey: { enabled: true } } },
    api: { labelsDictionary: ['internal'], primaryOwnerMode: 'HYBRID' },
    apiProduct: { primaryOwnerMode: 'HYBRID' },
};

describe('buildPrimaryOwnerModeSavePayload', () => {
    it('overlays both primary owner modes and keeps other portal settings', () => {
        const payload = buildPrimaryOwnerModeSavePayload(CURRENT, { api: 'USER', apiProduct: 'GROUP' });

        expect(payload.api).toEqual({ labelsDictionary: ['internal'], primaryOwnerMode: 'USER' });
        expect(payload.apiProduct).toEqual({ primaryOwnerMode: 'GROUP' });
        expect(payload.plan).toEqual(CURRENT.plan);
        expect(payload.company).toEqual(CURRENT.company);
    });

    it('creates api and apiProduct objects when they are missing', () => {
        const payload = buildPrimaryOwnerModeSavePayload({ cors: { maxAge: 60 } }, { api: 'GROUP', apiProduct: 'USER' });

        expect(payload.api).toEqual({ primaryOwnerMode: 'GROUP' });
        expect(payload.apiProduct).toEqual({ primaryOwnerMode: 'USER' });
        expect(payload.cors).toEqual({ maxAge: 60 });
    });
});
