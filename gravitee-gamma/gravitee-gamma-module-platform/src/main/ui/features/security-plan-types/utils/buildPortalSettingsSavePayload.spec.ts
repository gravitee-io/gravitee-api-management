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

import { buildPortalSettingsSavePayload } from './buildPortalSettingsSavePayload';
import type { PortalSettings } from '../services/portalSettings';

const CURRENT_SETTINGS: PortalSettings = {
    company: { name: 'Acme Corp' },
    cors: { allowOrigin: ['https://portal.example.com'] },
    plan: {
        security: {
            keyless: { enabled: true },
            apikey: { enabled: true },
            customApiKey: { enabled: true },
            customApiKeyReuse: { enabled: true },
            sharedApiKey: { enabled: true },
            oauth2: { enabled: true },
            jwt: { enabled: true },
            push: { enabled: true },
            mtls: { enabled: true },
        },
        validation: { enabled: true },
    },
};

const UPDATED_STATE = {
    keyless: true,
    apikey: true,
    customApiKey: true,
    customApiKeyReuse: true,
    sharedApiKey: true,
    oauth2: true,
    jwt: true,
    push: false,
    mtls: true,
};

describe('buildPortalSettingsSavePayload', () => {
    it('merges updated plan security into the full settings document', () => {
        const payload = buildPortalSettingsSavePayload(CURRENT_SETTINGS, UPDATED_STATE);

        expect(payload.company).toEqual({ name: 'Acme Corp' });
        expect(payload.cors).toEqual({ allowOrigin: ['https://portal.example.com'] });
        expect(payload.plan?.validation).toEqual({ enabled: true });
        expect(payload.plan?.security).toEqual({
            keyless: { enabled: true },
            apikey: { enabled: true },
            customApiKey: { enabled: true },
            customApiKeyReuse: { enabled: true },
            sharedApiKey: { enabled: true },
            oauth2: { enabled: true },
            jwt: { enabled: true },
            push: { enabled: false },
            mtls: { enabled: true },
        });
    });

    it('does not send a partial document with only plan.security', () => {
        const payload = buildPortalSettingsSavePayload(CURRENT_SETTINGS, UPDATED_STATE);

        expect(Object.keys(payload).sort()).toEqual(['company', 'cors', 'plan']);
    });
});
