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
import { buildApplicationRegistrationSavePayload } from './buildApplicationRegistrationSavePayload';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

const CURRENT: PortalSettings = {
    company: { name: 'Acme' },
    cors: { allowOrigin: ['https://portal.example.com'] },
    plan: {
        security: {
            jwt: { enabled: true },
            oauth2: { enabled: false },
        },
        validation: { enabled: true },
    },
    application: {
        registration: { enabled: false },
        types: {
            simple: { enabled: true },
            browser: { enabled: true },
            web: { enabled: true },
            native: { enabled: true },
            backend_to_backend: { enabled: true },
        },
    },
};

describe('buildApplicationRegistrationSavePayload', () => {
    it('replaces only application and preserves plan.security and other slices', () => {
        const nextApplication = {
            registration: { enabled: true },
            types: {
                simple: { enabled: false },
                browser: { enabled: true },
                web: { enabled: true },
                native: { enabled: true },
                backend_to_backend: { enabled: true },
            },
        };

        const payload = buildApplicationRegistrationSavePayload(CURRENT, nextApplication);

        expect(payload.company).toEqual({ name: 'Acme' });
        expect(payload.cors).toEqual({ allowOrigin: ['https://portal.example.com'] });
        expect(payload.plan?.security).toEqual({ jwt: { enabled: true }, oauth2: { enabled: false } });
        expect(payload.plan?.validation).toEqual({ enabled: true });
        expect(payload.application).toEqual(nextApplication);
    });

    it('posts a full document, not an application-only patch', () => {
        const payload = buildApplicationRegistrationSavePayload(CURRENT, CURRENT.application ?? {});
        expect(Object.keys(payload).sort()).toEqual(['application', 'company', 'cors', 'plan']);
    });
});
