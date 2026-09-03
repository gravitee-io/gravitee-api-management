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
import { PASSWORD_SENTINEL } from '../../organization-settings/types/consoleSettings';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

const CURRENT: PortalSettings = {
    company: { name: 'Acme Corp' },
    cors: { allowOrigin: ['https://portal.example.com'], allowMethods: ['GET'], maxAge: 1728000 },
    email: {
        enabled: true,
        host: 'smtp.example.com',
        password: PASSWORD_SENTINEL,
        from: 'noreply@example.com',
        properties: { auth: true },
        brandedSenders: [{ domains: ['partners.example.com'], from: 'Partners <partners@example.com>', subject: '[Partners] %s' }],
    },
    logging: {
        maxDurationMillis: 15000,
        audit: { enabled: false, trail: { enabled: false } },
        user: { displayed: false },
        messageSampling: { count: { default: 100, limit: 10 } },
    },
    plan: { security: { keyless: { enabled: true } }, validation: { enabled: true } },
};

describe('buildPortalSettingsSectionPayload', () => {
    it('overlays only cors and keeps plan, company, and email', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'cors', {
            cors: { allowOrigin: ['*'], allowMethods: ['GET', 'POST'], maxAge: 60 },
        });

        expect(payload.cors?.allowOrigin).toEqual(['*']);
        expect(payload.cors?.allowMethods).toEqual(['GET', 'POST']);
        expect(payload.cors?.maxAge).toBe(60);
        expect(payload.plan).toEqual(CURRENT.plan);
        expect(payload.company).toEqual(CURRENT.company);
        expect(payload.email).toEqual(CURRENT.email);
        expect(payload.logging).toEqual(CURRENT.logging);
    });

    it('keeps existing cors fields that the overlay does not set', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'cors', {
            cors: { maxAge: 60 },
        });

        expect(payload.cors?.allowOrigin).toEqual(['https://portal.example.com']);
        expect(payload.cors?.allowMethods).toEqual(['GET']);
        expect(payload.cors?.maxAge).toBe(60);
    });

    it('overlays only email and keeps the password sentinel', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'email', {
            email: { ...CURRENT.email, host: 'smtp.acme.com', password: PASSWORD_SENTINEL },
        });

        expect(payload.email?.host).toBe('smtp.acme.com');
        expect(payload.email?.password).toBe(PASSWORD_SENTINEL);
        expect(payload.cors).toEqual(CURRENT.cors);
        expect(payload.plan).toEqual(CURRENT.plan);
        expect(payload.logging).toEqual(CURRENT.logging);
    });

    it('sends a new password when the user replaced the sentinel', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'email', {
            email: { ...CURRENT.email, password: 'new-secret' },
        });

        expect(payload.email?.password).toBe('new-secret');
    });

    it('keeps the fetched password when the draft password was cleared', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'email', {
            email: { ...CURRENT.email, password: '' },
        });

        expect(payload.email?.password).toBe(CURRENT.email?.password);
    });

    it('merges email properties instead of replacing the whole map', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'email', {
            email: { properties: { startTlsEnable: true } },
        });

        expect(payload.email?.properties).toEqual({ auth: true, startTlsEnable: true });
        expect(payload.email?.host).toBe('smtp.example.com');
    });

    it('overlays only logging and keeps cors and email', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'logging', {
            logging: { maxDurationMillis: 20000, audit: { enabled: true } },
        });

        expect(payload.logging?.maxDurationMillis).toBe(20000);
        expect(payload.logging?.audit?.enabled).toBe(true);
        expect(payload.logging?.audit?.trail).toEqual(CURRENT.logging?.audit?.trail);
        expect(payload.logging?.messageSampling).toEqual(CURRENT.logging?.messageSampling);
        expect(payload.cors).toEqual(CURRENT.cors);
        expect(payload.email).toEqual(CURRENT.email);
    });

    it('overlays a complete logging sampling block without dropping sibling pairs', () => {
        const payload = buildPortalSettingsSectionPayload(CURRENT, 'logging', {
            logging: {
                messageSampling: {
                    probabilistic: { default: 0.02, limit: 0.4 },
                    count: { default: 80, limit: 10 },
                },
            },
        });

        expect(payload.logging?.messageSampling?.probabilistic).toEqual({ default: 0.02, limit: 0.4 });
        expect(payload.logging?.messageSampling?.count).toEqual({ default: 80, limit: 10 });
        expect(payload.logging?.maxDurationMillis).toBe(15000);
    });
});
