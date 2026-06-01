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
import { APPLICATION_TYPES_FIXTURE } from '../fixtures/applicationTypes.fixture';
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
        expect(form.redirectUris).toEqual([]);
        expect(form.additionalClientMetadata).toBeNull();
    });

    it('maps oauth settings including additional client metadata', () => {
        const oauthApp: ApplicationListItem = {
            ...baseApplication,
            type: 'WEB',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['authorization_code'],
                    redirect_uris: ['https://app.example/callback'],
                    additional_client_metadata: { audience: 'api' },
                },
            },
        };

        const form = formFromApplication(oauthApp);

        expect(form.redirectUris).toEqual(['https://app.example/callback']);
        expect(form.additionalClientMetadata).toEqual({ audience: 'api' });
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

    it('requires at least one grant type for OAuth applications such as SPA', () => {
        const spaApp: ApplicationListItem = {
            ...baseApplication,
            type: 'BROWSER',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['authorization_code'],
                    redirect_uris: ['https://app.example/callback'],
                },
            },
        };
        const spaType = APPLICATION_TYPES_FIXTURE.find(type => type.id === 'browser')!;
        const form = { ...formFromApplication(spaApp), grantTypes: [] };

        const errors = validateApplicationGeneralForm(form, { isOAuthApplication: true, typeConfig: spaType });

        expect(errors.grantTypes).toBe('Allowed grant types is required.');
        expect(hasApplicationGeneralValidationErrors(errors)).toBe(true);
    });

    it('does not require mandatory grant types on general save (console parity)', () => {
        const webApp: ApplicationListItem = {
            ...baseApplication,
            type: 'WEB',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['refresh_token'],
                    redirect_uris: ['https://app.example/callback'],
                },
            },
        };
        const webType = APPLICATION_TYPES_FIXTURE.find(type => type.id === 'web')!;
        const form = formFromApplication(webApp);

        const errors = validateApplicationGeneralForm(form, { isOAuthApplication: true, typeConfig: webType });

        expect(errors.grantTypes).toBeUndefined();
        expect(hasApplicationGeneralValidationErrors(errors)).toBe(false);
    });

    it('rejects duplicate additional client metadata keys', () => {
        const form = formFromApplication({
            ...baseApplication,
            type: 'BROWSER',
            settings: { oauth: { client_id: 'id', grant_types: ['authorization_code'] } },
        });

        const errors = validateApplicationGeneralForm(form, {
            isOAuthApplication: true,
            metadataHasDuplicateKeys: true,
        });

        expect(errors.additionalClientMetadata).toBe('Keys must be unique');
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

    it('detects metadata and redirect URI changes', () => {
        const saved = formFromApplication({
            ...baseApplication,
            type: 'WEB',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['authorization_code'],
                    redirect_uris: ['https://a.example'],
                    additional_client_metadata: { foo: 'bar' },
                },
            },
        });

        expect(isApplicationGeneralFormDirty({ ...saved, redirectUris: ['https://b.example'] }, saved)).toBe(true);
        expect(isApplicationGeneralFormDirty({ ...saved, additionalClientMetadata: { foo: 'baz' } }, saved)).toBe(true);
    });

    it('builds simple application update payload', () => {
        const form = { ...formFromApplication(baseApplication), name: 'Updated', simpleClientId: 'new-client' };
        const payload = buildUpdatePayload(baseApplication, form, undefined);
        expect(payload.name).toBe('Updated');
        expect(payload.settings?.app?.client_id).toBe('new-client');
    });

    it('always includes both images so updating one does not clear the other', () => {
        const app: ApplicationListItem = {
            ...baseApplication,
            picture: 'data:image/png;base64,pic',
            background: 'data:image/png;base64,bg',
        };
        const form = {
            ...formFromApplication(app),
            picture: 'data:image/png;base64,new-pic',
        };
        const payload = buildUpdatePayload(app, form, undefined);
        expect(payload.picture).toBe('data:image/png;base64,new-pic');
        expect(payload.background).toBe('data:image/png;base64,bg');
    });

    it('sends null only for explicitly removed images', () => {
        const app: ApplicationListItem = {
            ...baseApplication,
            picture: 'data:image/png;base64,pic',
            background: 'data:image/png;base64,bg',
        };
        const form = {
            ...formFromApplication(app),
            picture: null,
            pictureRemoved: true,
        };
        const payload = buildUpdatePayload(app, form, undefined);
        expect(payload.picture).toBeNull();
        expect(payload.background).toBe('data:image/png;base64,bg');
    });

    it('includes redirect URIs and additional client metadata in oauth update payload', () => {
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
            redirectUris: ['https://a.example', 'https://b.example'],
            additionalClientMetadata: { scope: 'openid' },
        };
        const payload = buildUpdatePayload(oauthApp, form, {
            id: 'web',
            name: 'Web',
            allowed_grant_types: [],
            mandatory_grant_types: [],
            default_grant_types: [],
            requires_redirect_uris: true,
        });
        expect(payload.settings?.oauth?.redirect_uris).toEqual(['https://a.example', 'https://b.example']);
        expect(payload.settings?.oauth?.additional_client_metadata).toEqual({ scope: 'openid' });
    });

    it('sends empty metadata object when cleared', () => {
        const oauthApp: ApplicationListItem = {
            ...baseApplication,
            type: 'WEB',
            settings: {
                oauth: {
                    client_id: 'oauth-id',
                    grant_types: ['authorization_code'],
                    additional_client_metadata: { legacy: 'value' },
                },
            },
        };
        const form = { ...formFromApplication(oauthApp), additionalClientMetadata: null };
        const payload = buildUpdatePayload(oauthApp, form, undefined);
        expect(payload.settings?.oauth?.additional_client_metadata).toEqual({});
    });
});
