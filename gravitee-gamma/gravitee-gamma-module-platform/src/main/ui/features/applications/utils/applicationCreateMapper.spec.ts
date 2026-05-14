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
    defaultGrantTypesForType,
    isMandatoryGrantType,
    isRegisterApplicationFormValid,
    mapDraftToCreateRequest,
    rowsToAdditionalClientMetadata,
} from './applicationCreateMapper';
import { APPLICATION_TYPES_FIXTURE } from '../fixtures/applicationTypes.fixture';
import type { RegisterApplicationDraft } from '../types/applicationCreate';

const BASE_DRAFT: RegisterApplicationDraft = {
    name: 'My App',
    description: 'Description',
    domain: '',
    typeId: 'simple',
    groups: [],
    appType: '',
    appClientId: '',
    grantTypes: [],
    redirectUris: [],
    clientCertificate: '',
    additionalClientMetadata: null,
};

describe('isMandatoryGrantType', () => {
    it('returns false for default-only grant types (SPA)', () => {
        const spaType = APPLICATION_TYPES_FIXTURE[1]!;

        expect(defaultGrantTypesForType(spaType)).toContain('authorization_code');
        expect(isMandatoryGrantType(spaType, 'authorization_code')).toBe(false);
    });

    it('returns true only for mandatory grant types', () => {
        const webType = APPLICATION_TYPES_FIXTURE[2]!;

        expect(isMandatoryGrantType(webType, 'authorization_code')).toBe(true);
        expect(isMandatoryGrantType(webType, 'refresh_token')).toBe(false);
    });
});

describe('isRegisterApplicationFormValid', () => {
    it('requires name and description', () => {
        expect(isRegisterApplicationFormValid({ ...BASE_DRAFT, name: '' }, APPLICATION_TYPES_FIXTURE[0])).toBe(false);
        expect(isRegisterApplicationFormValid({ ...BASE_DRAFT, description: '  ' }, APPLICATION_TYPES_FIXTURE[0])).toBe(false);
    });

    it('is valid for simple type when general fields are filled', () => {
        expect(isRegisterApplicationFormValid(BASE_DRAFT, APPLICATION_TYPES_FIXTURE[0])).toBe(true);
    });

    it('requires at least one group when userGroup.required.enabled is on', () => {
        const simpleType = APPLICATION_TYPES_FIXTURE[0]!;

        expect(isRegisterApplicationFormValid(BASE_DRAFT, simpleType, { requireUserGroups: true })).toBe(false);
        expect(isRegisterApplicationFormValid({ ...BASE_DRAFT, groups: ['group-1'] }, simpleType, { requireUserGroups: true })).toBe(true);
    });

    it('does not require groups when userGroup.required.enabled is off', () => {
        expect(isRegisterApplicationFormValid(BASE_DRAFT, APPLICATION_TYPES_FIXTURE[0], { requireUserGroups: false })).toBe(true);
    });

    it('requires grant types and redirect URIs for oauth types', () => {
        const webType = APPLICATION_TYPES_FIXTURE[2]!;

        expect(isRegisterApplicationFormValid({ ...BASE_DRAFT, typeId: 'web', grantTypes: [], redirectUris: [] }, webType)).toBe(false);

        expect(
            isRegisterApplicationFormValid(
                {
                    ...BASE_DRAFT,
                    typeId: 'web',
                    grantTypes: ['authorization_code'],
                    redirectUris: ['https://app.example.com/callback'],
                },
                webType,
            ),
        ).toBe(true);

        expect(
            isRegisterApplicationFormValid(
                {
                    ...BASE_DRAFT,
                    typeId: 'web',
                    grantTypes: ['authorization_code'],
                    redirectUris: ['https://a.example.com/callback', 'https://b.example.com/callback'],
                },
                webType,
            ),
        ).toBe(true);
    });
});

describe('rowsToAdditionalClientMetadata', () => {
    it('returns null when all rows are empty', () => {
        expect(rowsToAdditionalClientMetadata([{ key: '', value: '' }])).toBeNull();
    });

    it('builds a record from non-empty rows', () => {
        expect(
            rowsToAdditionalClientMetadata([
                { key: 'policy_uri', value: 'https://example.com/policy' },
                { key: '', value: '' },
            ]),
        ).toEqual({ policy_uri: 'https://example.com/policy' });
    });
});

describe('mapDraftToCreateRequest', () => {
    it('maps redirect URIs array to oauth settings', () => {
        const webType = APPLICATION_TYPES_FIXTURE[2]!;
        const request = mapDraftToCreateRequest(
            {
                ...BASE_DRAFT,
                typeId: 'web',
                grantTypes: ['authorization_code'],
                redirectUris: ['https://a.example.com/callback', 'https://b.example.com/callback'],
            },
            webType,
        );

        expect(request.settings?.oauth?.redirect_uris).toEqual(['https://a.example.com/callback', 'https://b.example.com/callback']);
    });

    it('includes additional_client_metadata when provided', () => {
        const webType = APPLICATION_TYPES_FIXTURE[2]!;
        const request = mapDraftToCreateRequest(
            {
                ...BASE_DRAFT,
                typeId: 'web',
                grantTypes: ['authorization_code'],
                redirectUris: ['https://app.example.com/callback'],
                additionalClientMetadata: {
                    policy_uri: 'https://example.com/policy',
                    tos_uri: 'https://example.com/tos',
                },
            },
            webType,
        );

        expect(request.settings?.oauth?.additional_client_metadata).toEqual({
            policy_uri: 'https://example.com/policy',
            tos_uri: 'https://example.com/tos',
        });
    });

    it('omits additional_client_metadata when empty', () => {
        const webType = APPLICATION_TYPES_FIXTURE[2]!;
        const request = mapDraftToCreateRequest(
            {
                ...BASE_DRAFT,
                typeId: 'web',
                grantTypes: ['authorization_code'],
                redirectUris: ['https://app.example.com/callback'],
                additionalClientMetadata: null,
            },
            webType,
        );

        expect(request.settings?.oauth?.additional_client_metadata).toBeUndefined();
    });
});
