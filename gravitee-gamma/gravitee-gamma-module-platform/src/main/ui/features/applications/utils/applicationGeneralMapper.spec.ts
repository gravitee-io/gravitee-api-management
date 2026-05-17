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
    buildUpdatePayload,
    formFromApplication,
    hasApplicationGeneralValidationErrors,
    isApplicationGeneralFormDirty,
    validateApplicationGeneralForm,
} from './applicationGeneralMapper';
import type { ApplicationListItem } from '../types/application';

const baseApplication: ApplicationListItem = {
    id: 'app-1',
    name: 'My App',
    description: 'Desc',
    status: 'ACTIVE',
    type: 'SIMPLE',
    created_at: 1,
    updated_at: 1,
    settings: { app: { client_id: 'client-1' } },
};

describe('applicationGeneralMapper', () => {
    it('maps application to form', () => {
        const form = formFromApplication(baseApplication);
        expect(form.name).toBe('My App');
        expect(form.simpleClientId).toBe('client-1');
    });

    it('validates required fields', () => {
        const errors = validateApplicationGeneralForm({
            ...formFromApplication(baseApplication),
            name: '',
            description: '',
        });
        expect(hasApplicationGeneralValidationErrors(errors)).toBe(true);
        expect(errors.name).toBeDefined();
        expect(errors.description).toBeDefined();
    });

    it('detects dirty state with explicit field comparison', () => {
        const saved = formFromApplication(baseApplication);
        const unchanged = { ...saved };
        const renamed = { ...saved, name: 'Other' };
        const reorderedGrantTypes = { ...saved, grantTypes: [...saved.grantTypes].reverse() };

        expect(isApplicationGeneralFormDirty(unchanged, saved)).toBe(false);
        expect(isApplicationGeneralFormDirty(renamed, saved)).toBe(true);
        expect(isApplicationGeneralFormDirty(reorderedGrantTypes, saved)).toBe(false);
    });

    it('builds simple application update payload', () => {
        const form = { ...formFromApplication(baseApplication), name: 'Updated', simpleClientId: 'new-client' };
        const payload = buildUpdatePayload(baseApplication, form, undefined);
        expect(payload.name).toBe('Updated');
        expect(payload.settings?.app?.client_id).toBe('new-client');
    });

    it('parses redirect URIs for oauth applications', () => {
        const oauthApp: ApplicationListItem = {
            ...baseApplication,
            type: 'WEB',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['authorization_code'],
                    redirect_uris: [],
                },
            },
        };
        const form = {
            ...formFromApplication(oauthApp),
            redirectUrisText: 'https://a.example\nhttps://b.example\n',
        };
        const payload = buildUpdatePayload(oauthApp, form, {
            id: 'WEB',
            name: 'Web',
            allowed_grant_types: [],
            mandatory_grant_types: [],
            default_grant_types: [],
            requires_redirect_uris: true,
        });
        expect(payload.settings?.oauth?.redirect_uris).toEqual(['https://a.example', 'https://b.example']);
    });
});
