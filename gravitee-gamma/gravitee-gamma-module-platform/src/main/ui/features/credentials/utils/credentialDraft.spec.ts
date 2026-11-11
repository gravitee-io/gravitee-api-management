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
import {
    draftFromCredential,
    emptyCredentialDraft,
    rotationValues,
    toCreateCredentialPayload,
    toUpdateCredentialPayload,
    validateCredentialDraft,
    type CredentialDraft,
} from './credentialDraft';
import { CREDENTIAL_STRATEGY_DEFINITIONS } from './credentialStrategies';
import type { Credential } from '../types/credential';

const NOW = new Date('2026-11-11T12:00:00');

function draft(overrides: Partial<CredentialDraft> = {}): CredentialDraft {
    return { ...emptyCredentialDraft(), name: 'OpenAI', ...overrides };
}

function localMidnight(date: string): string {
    return new Date(`${date}T00:00:00`).toISOString();
}

describe('credential strategies', () => {
    it.each([
        ['BEARER', ['token'], []],
        ['API_KEY', ['value'], ['headerName']],
        ['OAUTH2', ['clientId', 'clientSecret'], ['tokenUrl', 'scopes', 'audience']],
        ['BASIC', ['username', 'password'], []],
        ['SERVICE_ACCOUNT', ['serviceAccountKey'], []],
    ] as const)('gives %s the secret fields and settings of the API', (strategy, secrets, settings) => {
        const definition = CREDENTIAL_STRATEGY_DEFINITIONS[strategy];

        expect(definition.secrets.map(field => field.key)).toEqual(secrets);
        expect(definition.settings.map(field => field.key)).toEqual(settings);
    });
});

describe('validateCredentialDraft', () => {
    it('accepts a complete bearer credential', () => {
        expect(validateCredentialDraft(draft({ secrets: { token: 'sk-1' } }), { secretsRequired: true, now: NOW })).toEqual({});
    });

    it('requires a name of at most 256 characters', () => {
        const options = { secretsRequired: false, now: NOW };

        expect(validateCredentialDraft(draft({ name: ' ' }), options).name).toBe('Name is required.');
        expect(validateCredentialDraft(draft({ name: 'a'.repeat(257) }), options).name).toBe('Name must be at most 256 characters.');
    });

    it('requires every secret field when secrets are required', () => {
        const errors = validateCredentialDraft(draft({ strategy: 'BASIC', secrets: { username: 'bob' } }), {
            secretsRequired: true,
            now: NOW,
        });

        expect(errors).toEqual({ 'secret.password': 'Password is required.' });
    });

    it('lets the secret fields stay empty when they are not required', () => {
        expect(validateCredentialDraft(draft({ strategy: 'BASIC' }), { secretsRequired: false, now: NOW })).toEqual({});
    });

    it('requires a valid header name for an API key', () => {
        const options = { secretsRequired: false, now: NOW };

        expect(validateCredentialDraft(draft({ strategy: 'API_KEY' }), options)).toEqual({
            'setting.headerName': 'Header name is required.',
        });
        expect(validateCredentialDraft(draft({ strategy: 'API_KEY', settings: { headerName: 'X Api' } }), options)).toEqual({
            'setting.headerName': 'Header name must be a valid HTTP header name.',
        });
    });

    it('requires an http or https token URL for OAuth2', () => {
        const errors = validateCredentialDraft(draft({ strategy: 'OAUTH2', settings: { tokenUrl: 'ftp://idp/token' } }), {
            secretsRequired: false,
            now: NOW,
        });

        expect(errors).toEqual({ 'setting.tokenUrl': 'Token URL must be an http or https URL.' });
    });

    it('refuses an expiry in the past', () => {
        const errors = validateCredentialDraft(draft({ expiresAt: '2026-11-10' }), { secretsRequired: false, now: NOW });

        expect(errors).toEqual({ expiresAt: 'The expiry date must be today or later.' });
    });
});

describe('toCreateCredentialPayload', () => {
    it('sends typed values as values for Gravitee to store', () => {
        expect(toCreateCredentialPayload(draft({ secrets: { token: 'sk-1' } }))).toEqual({
            name: 'OpenAI',
            strategy: 'BEARER',
            configuration: {},
            values: { token: 'sk-1' },
        });
    });

    it('sends trimmed references in reference mode', () => {
        const payload = toCreateCredentialPayload(draft({ mode: 'reference', secrets: { token: ' /vault/openai:token ' } }));

        expect(payload.references).toEqual({ token: '/vault/openai:token' });
        expect(payload.values).toBeUndefined();
    });

    it('splits the OAuth2 scopes and leaves out empty optional settings', () => {
        const payload = toCreateCredentialPayload(
            draft({
                strategy: 'OAUTH2',
                settings: { tokenUrl: 'https://idp/token', scopes: 'read, write ,', audience: ' ' },
                secrets: { clientId: 'id', clientSecret: 'secret' },
            }),
        );

        expect(payload.configuration).toEqual({ tokenUrl: 'https://idp/token', scopes: ['read', 'write'] });
    });

    it('sends the service and the expiry only when set', () => {
        expect(toCreateCredentialPayload(draft({ serviceId: ' openai ', expiresAt: '2027-03-01' }))).toMatchObject({
            serviceId: 'openai',
            expiresAt: localMidnight('2027-03-01'),
        });
        expect(toCreateCredentialPayload(draft())).not.toHaveProperty('serviceId');
        expect(toCreateCredentialPayload(draft())).not.toHaveProperty('expiresAt');
    });
});

describe('editing a credential', () => {
    const managed: Credential = {
        id: 'cred-1',
        name: 'OpenAI',
        strategy: 'OAUTH2',
        serviceId: 'openai',
        expiresAt: '2027-03-01T10:30:00Z',
        configuration: { tokenUrl: 'https://idp/token', scopes: ['read', 'write'] },
        secretReferences: {
            clientId: { reference: '/gravitee/credential.cred-1.clientId:value', managed: true },
            clientSecret: { reference: '/gravitee/credential.cred-1.clientSecret:value', managed: true },
        },
        managed: true,
    };
    const referenced: Credential = {
        id: 'cred-2',
        name: 'Vault token',
        strategy: 'BEARER',
        secretReferences: { token: { reference: '/vault/secret/openai:token', managed: false } },
        managed: false,
    };

    it('starts a managed credential with blank values and its settings', () => {
        const edited = draftFromCredential(managed);

        expect(edited).toMatchObject({
            mode: 'value',
            serviceId: 'openai',
            settings: { tokenUrl: 'https://idp/token', scopes: 'read, write' },
            secrets: {},
        });
    });

    it('starts a reference credential with its references', () => {
        expect(draftFromCredential(referenced)).toMatchObject({ mode: 'reference', secrets: { token: '/vault/secret/openai:token' } });
    });

    it('keeps the references and the exact expiry of a managed credential', () => {
        const payload = toUpdateCredentialPayload({ ...draftFromCredential(managed), name: 'OpenAI prod' }, managed);

        expect(payload).toEqual({
            name: 'OpenAI prod',
            serviceId: 'openai',
            expiresAt: '2027-03-01T10:30:00Z',
            configuration: { tokenUrl: 'https://idp/token', scopes: ['read', 'write'] },
        });
    });

    it('sends the edited references of a reference credential', () => {
        const payload = toUpdateCredentialPayload(
            { ...draftFromCredential(referenced), secrets: { token: '/vault/secret/new:token' } },
            referenced,
        );

        expect(payload.references).toEqual({ token: '/vault/secret/new:token' });
    });

    it('rotates only the values the user typed', () => {
        expect(rotationValues({ ...draftFromCredential(managed), secrets: { clientId: '', clientSecret: 'new-secret' } })).toEqual({
            clientSecret: 'new-secret',
        });
        expect(rotationValues(draftFromCredential(managed))).toBeUndefined();
    });
});
