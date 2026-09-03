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

import { buildPortalSettingsSectionPayload } from './buildPortalSettingsSectionPayload';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

const CURRENT: PortalSettings = {
    company: { name: 'Acme Corp' },
    cors: { allowOrigin: ['https://portal.example.com'], allowMethods: ['GET'], maxAge: 1728000 },
    plan: { security: { keyless: { enabled: true } }, validation: { enabled: true } },
};

describe('buildPortalSettingsSectionPayload', () => {
    it('overlays only cors and keeps plan and company', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'cors', {
            cors: { allowOrigin: ['*'], allowMethods: ['GET', 'POST'], maxAge: 60 },
        });

        expect(payload.cors?.allowOrigin).toEqual(['*']);
        expect(payload.cors?.allowMethods).toEqual(['GET', 'POST']);
        expect(payload.cors?.maxAge).toBe(60);
        expect(payload.plan).toEqual(CURRENT.plan);
        expect(payload.company).toEqual(CURRENT.company);
    });

    it('keeps existing cors fields that the overlay does not set', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'cors', {
            cors: { maxAge: 60 },
        });

        expect(payload.cors?.allowOrigin).toEqual(['https://portal.example.com']);
        expect(payload.cors?.allowMethods).toEqual(['GET']);
        expect(payload.cors?.maxAge).toBe(60);
    });
});
