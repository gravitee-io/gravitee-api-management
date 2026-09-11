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
    claimMappingsToDictionary,
    dictionaryToClaimMappings,
    validateProviderForm,
    type ProviderForm,
    type StoreForm,
} from './validateProviderForm';

const EMPTY_STORE: StoreForm = {
    type: 'NONE',
    pathOrContent: 'PATH',
    password: '',
    path: '',
    content: '',
    alias: '',
    keyPassword: '',
};

function validForm(overrides: Partial<ProviderForm> = {}): ProviderForm {
    return {
        name: 'Okta DCR',
        description: '',
        discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
        initial_access_token_type: 'CLIENT_CREDENTIALS',
        client_id: 'id',
        client_secret: 'secret',
        scopes: [],
        software_id: '',
        initial_access_token: '',
        renew_client_secret_support: false,
        renew_client_secret_method: '',
        renew_client_secret_endpoint: '',
        claim_mappings: [],
        trust_store: { ...EMPTY_STORE },
        key_store: { ...EMPTY_STORE },
        ...overrides,
    };
}

describe('validateProviderForm', () => {
    it('requires name length between 3 and 50', () => {
        expect(validateProviderForm(validForm({ name: '' })).name).toBe('This field is required.');
        expect(validateProviderForm(validForm({ name: 'ab' })).name).toBe('Name has to be more than 3 characters long');
        expect(validateProviderForm(validForm({ name: 'a'.repeat(51) })).name).toBe('Name has to be less than 50 characters long.');
        expect(validateProviderForm(validForm({ name: 'Okta' })).name).toBeUndefined();
    });

    it('requires client credentials fields only for CLIENT_CREDENTIALS', () => {
        const missing = validateProviderForm(
            validForm({ initial_access_token_type: 'CLIENT_CREDENTIALS', client_id: '', client_secret: '' }),
        );
        expect(missing.client_id).toBe('This field is required.');
        expect(missing.client_secret).toBe('This field is required.');
        expect(missing.initial_access_token).toBeUndefined();

        const tokenType = validateProviderForm(
            validForm({
                initial_access_token_type: 'INITIAL_ACCESS_TOKEN',
                client_id: '',
                client_secret: '',
                initial_access_token: 'tok',
            }),
        );
        expect(tokenType.client_id).toBeUndefined();
        expect(tokenType.client_secret).toBeUndefined();
        expect(tokenType.initial_access_token).toBeUndefined();
    });

    it('requires initial access token for INITIAL_ACCESS_TOKEN', () => {
        const errors = validateProviderForm(validForm({ initial_access_token_type: 'INITIAL_ACCESS_TOKEN', initial_access_token: '' }));
        expect(errors.initial_access_token).toBe('This field is required.');
    });

    it('requires trust and key store password and path when type is not NONE', () => {
        const errors = validateProviderForm(
            validForm({
                trust_store: { ...EMPTY_STORE, type: 'JKS', pathOrContent: 'PATH', password: '', path: '' },
                key_store: { ...EMPTY_STORE, type: 'PKCS12', pathOrContent: 'CONTENT', password: '', content: '' },
            }),
        );
        expect(errors.trust_password).toBe('Password is required');
        expect(errors.trust_path).toBe('This field is required.');
        expect(errors.key_password).toBe('Password is required');
        expect(errors.key_content).toBe('This field is required.');
    });

    it('rejects duplicate and blank claim mapping rows', () => {
        expect(
            validateProviderForm(
                validForm({
                    claim_mappings: [
                        { key: 'a', value: 'x' },
                        { key: 'a', value: 'y' },
                    ],
                }),
            ).claim_mappings,
        ).toBe('Claim names must be unique');
        expect(validateProviderForm(validForm({ claim_mappings: [{ key: 'a', value: '' }] })).claim_mappings).toBe(
            'Every mapping needs both a claim name and a registration request field',
        );
        expect(
            validateProviderForm(validForm({ claim_mappings: [{ key: 'org_id', value: 'metadata.organization' }] })).claim_mappings,
        ).toBeUndefined();
    });
});

describe('claim mapping conversion', () => {
    it('round-trips Classic Record<string, string> claim_mappings', () => {
        const record = { org_id: 'metadata.organization' };
        expect(claimMappingsToDictionary(dictionaryToClaimMappings(record))).toEqual(record);
        expect(claimMappingsToDictionary([])).toEqual({});
    });
});
