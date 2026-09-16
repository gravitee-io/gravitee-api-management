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

import { buildPortalCorsSavePayload } from './buildPortalCorsSavePayload';
import type { PortalSettings } from '../services/portalSettings';

const CURRENT: PortalSettings = {
    cors: { allowOrigin: ['https://portal.example.com'], maxAge: 1728000 },
    plan: { security: { apikey: { enabled: true } } },
    company: { name: 'Acme' },
};

describe('buildPortalCorsSavePayload', () => {
    it('overlays cors and keeps other portal settings', () => {
        const payload = buildPortalCorsSavePayload(CURRENT, {
            allowOrigin: ['*'],
            allowMethods: ['GET', 'POST'],
            allowHeaders: ['Authorization'],
            exposedHeaders: ['ETag'],
            maxAge: 60,
        });

        expect(payload.cors).toEqual({
            allowOrigin: ['*'],
            allowMethods: ['GET', 'POST'],
            allowHeaders: ['Authorization'],
            exposedHeaders: ['ETag'],
            maxAge: 60,
        });
        expect(payload.plan).toEqual(CURRENT.plan);
        expect(payload.company).toEqual(CURRENT.company);
    });
});
