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

import { buildSmtpEmailPatch, buildSmtpFieldReadonly, buildSmtpFormState, canEnvironmentResetBrandedSenders } from './smtpFormState';
import type { SmtpFormState } from '../components/SmtpSection';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../types/consoleSettings';

const SETTINGS: ConsoleSettings = {
    email: {
        enabled: true,
        host: 'smtp.example.com',
        port: 587,
        username: 'admin',
        password: PASSWORD_SENTINEL,
        protocol: 'smtp',
        subject: '[gravitee] %s',
        from: 'noreply@example.com',
        properties: { auth: true, startTlsEnable: true, sslTrust: '' },
        brandedSenders: [],
        brandedSendersInherited: false,
    },
    metadata: { readonly: ['email.host'] },
};

const LOCAL: SmtpFormState = {
    enabled: true,
    host: 'localhost',
    port: '1025',
    username: '',
    password: PASSWORD_SENTINEL,
    protocol: 'smtp',
    subject: '[Gravitee.io] %s',
    from: 'noreply@gravitee.local',
    auth: false,
    startTlsEnable: false,
    sslTrust: '',
    brandedSenders: [],
};

describe('smtpFormState', () => {
    it('builds form state from console settings', () => {
        expect(buildSmtpFormState(SETTINGS)).toEqual({
            enabled: true,
            host: 'smtp.example.com',
            port: '587',
            username: 'admin',
            password: PASSWORD_SENTINEL,
            protocol: 'smtp',
            subject: '[gravitee] %s',
            from: 'noreply@example.com',
            auth: true,
            startTlsEnable: true,
            sslTrust: '',
            brandedSenders: [],
        });
    });

    it('maps metadata readonly keys to field flags', () => {
        expect(buildSmtpFieldReadonly(SETTINGS)).toEqual({
            enabled: false,
            host: true,
            port: false,
            username: false,
            password: false,
            protocol: false,
            subject: false,
            from: false,
            auth: false,
            startTlsEnable: false,
            sslTrust: false,
            brandedSenders: false,
        });
    });

    it('builds the email patch for save payloads', () => {
        expect(buildSmtpEmailPatch(LOCAL)).toEqual({
            enabled: true,
            host: 'localhost',
            port: 1025,
            username: '',
            password: PASSWORD_SENTINEL,
            protocol: 'smtp',
            subject: '[Gravitee.io] %s',
            from: 'noreply@gravitee.local',
            brandedSenders: [],
            properties: { auth: false, startTlsEnable: false, sslTrust: '' },
        });
    });

    it('offers branded-senders reset only for environment overrides', () => {
        const readonly = buildSmtpFieldReadonly(SETTINGS);
        expect(canEnvironmentResetBrandedSenders({ canEdit: true, localState: LOCAL, readonly, settings: SETTINGS })).toBe(true);
        expect(
            canEnvironmentResetBrandedSenders({
                canEdit: true,
                localState: LOCAL,
                readonly,
                settings: { ...SETTINGS, email: { ...SETTINGS.email!, brandedSendersInherited: true } },
            }),
        ).toBe(false);
        expect(canEnvironmentResetBrandedSenders({ canEdit: false, localState: LOCAL, readonly, settings: SETTINGS })).toBe(false);
    });
});
