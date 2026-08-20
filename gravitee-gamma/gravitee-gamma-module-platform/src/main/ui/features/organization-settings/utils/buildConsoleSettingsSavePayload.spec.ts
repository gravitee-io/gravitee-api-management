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

import { buildConsoleSettingsSavePayload } from './buildConsoleSettingsSavePayload';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../types/consoleSettings';

const CURRENT: ConsoleSettings = {
    management: {
        title: 'Gravitee.io Management',
        url: 'https://apim.example.com',
        support: { enabled: true },
        userCreation: { enabled: true },
        automaticValidation: { enabled: true },
    },
    scheduler: { tasks: 10, notifications: 10 },
    cors: { allowOrigin: ['https://console.example.com'], allowMethods: ['GET'], maxAge: 1728000 },
    email: {
        enabled: true,
        host: 'smtp.example.com',
        port: 587,
        password: PASSWORD_SENTINEL,
        from: 'noreply@example.com',
        subject: '[gravitee] %s',
        properties: { auth: true, startTlsEnable: true },
        brandedSenders: [{ domains: ['partners.example.com'], from: 'Partners <partners@example.com>', subject: '[Partners] %s' }],
    },
    authentication: { localLogin: { enabled: true } },
    trialInstance: { enabled: false },
};

describe('buildConsoleSettingsSavePayload', () => {
    it('overlays only management and scheduler and keeps cors and email', () => {
        const payload = buildConsoleSettingsSavePayload(CURRENT, 'management', {
            management: { title: 'Acme Console', url: CURRENT.management?.url, support: { enabled: false } },
            scheduler: { tasks: 30, notifications: 10 },
        });

        expect(payload.management?.title).toBe('Acme Console');
        expect(payload.management?.support?.enabled).toBe(false);
        expect(payload.scheduler?.tasks).toBe(30);
        expect(payload.cors).toEqual(CURRENT.cors);
        expect(payload.email).toEqual(CURRENT.email);
        expect(payload.authentication).toEqual(CURRENT.authentication);
    });

    it('overlays only cors', () => {
        const payload = buildConsoleSettingsSavePayload(CURRENT, 'cors', {
            cors: { allowOrigin: ['*'], allowMethods: ['GET', 'POST'], maxAge: 60 },
        });

        expect(payload.cors?.allowOrigin).toEqual(['*']);
        expect(payload.management).toEqual(CURRENT.management);
        expect(payload.email).toEqual(CURRENT.email);
    });

    it('keeps the fetched password when the draft is still the sentinel', () => {
        const payload = buildConsoleSettingsSavePayload(CURRENT, 'email', {
            email: { ...CURRENT.email, host: 'smtp.acme.com', password: PASSWORD_SENTINEL },
        });

        expect(payload.email?.host).toBe('smtp.acme.com');
        expect(payload.email?.password).toBe(PASSWORD_SENTINEL);
        expect(payload.cors).toEqual(CURRENT.cors);
    });

    it('sends a new password when the user replaced the sentinel', () => {
        const payload = buildConsoleSettingsSavePayload(CURRENT, 'email', {
            email: { ...CURRENT.email, password: 'new-secret' },
        });

        expect(payload.email?.password).toBe('new-secret');
    });

    it('keeps the fetched password when the draft password was cleared to an empty string', () => {
        const payload = buildConsoleSettingsSavePayload(CURRENT, 'email', {
            email: { ...CURRENT.email, password: '' },
        });

        expect(payload.email?.password).toBe(CURRENT.email?.password);
    });

    it('does not overlay email on a trial instance', () => {
        const trial: ConsoleSettings = { ...CURRENT, trialInstance: { enabled: true } };
        const payload = buildConsoleSettingsSavePayload(trial, 'email', {
            email: { enabled: false, host: 'should-not-save.example.com' },
        });

        expect(payload.email).toEqual(CURRENT.email);
    });
});
