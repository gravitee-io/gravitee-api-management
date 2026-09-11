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
    createClientRegistrationProvider,
    deleteClientRegistrationProvider,
    getClientRegistrationProvider,
    listClientRegistrationProviders,
    updateClientRegistrationProvider,
} from './clientRegistrationProviders';
import { apimFetchJsonV1Env, ApimApiError } from '../../../shared/api/apimClient';
import {
    CLIENT_REGISTRATION_PROVIDER_UPDATE_FIELDS,
    type ClientRegistrationProvider,
    type ClientRegistrationProviderWrite,
} from '../types/clientRegistrationProvider';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
    ApimApiError: jest.requireActual('../../../shared/api/apimClient').ApimApiError,
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const WRITE: ClientRegistrationProviderWrite = {
    name: 'Okta DCR',
    description: 'Prod IdP',
    discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
    initial_access_token_type: 'CLIENT_CREDENTIALS',
    client_id: 'dcr-client',
    client_secret: 'secret',
    scopes: ['dcr'],
    initial_access_token: '',
    renew_client_secret_support: false,
    renew_client_secret_endpoint: '',
    renew_client_secret_method: 'POST',
    software_id: 'tmpl',
    claim_mappings: { org_id: 'metadata.organization' },
    trust_store: { type: 'NONE' },
    key_store: { type: 'NONE' },
};

const SAVED: ClientRegistrationProvider = { id: 'prov-1', ...WRITE };

describe('clientRegistrationProviders service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    it('list calls GET on Classic ClientRegistrationProvidersService.list path', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue([SAVED]);
        const result = await listClientRegistrationProviders('env-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/applications/registration/providers');
        expect(result).toEqual([SAVED]);
    });

    it('get calls GET .../providers/{id}', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(SAVED);
        await getClientRegistrationProvider('env-1', 'prov-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/applications/registration/providers/prov-1');
    });

    it('URL-encodes the provider id', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(SAVED);
        await getClientRegistrationProvider('env-1', 'id with spaces');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
            'env-1',
            '/configuration/applications/registration/providers/id%20with%20spaces',
        );
    });

    it('create POSTs the full provider object like Classic create()', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(SAVED);
        await createClientRegistrationProvider('env-1', WRITE);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/applications/registration/providers', {
            method: 'POST',
            body: JSON.stringify(WRITE),
        });
        expect(JSON.parse(String(mockApimFetchJsonV1Env.mock.calls[0]?.[2]?.body))).not.toHaveProperty('id');
    });

    it('propagates 403 from create (missing apim-dcr-registration)', async () => {
        mockApimFetchJsonV1Env.mockRejectedValue(new ApimApiError(403, 'Feature not allowed'));
        await expect(createClientRegistrationProvider('env-1', WRITE)).rejects.toMatchObject({ status: 403 });
    });

    it('propagates 400 from create (second provider)', async () => {
        mockApimFetchJsonV1Env.mockRejectedValue(new ApimApiError(400, 'A client registration provider already exists'));
        await expect(createClientRegistrationProvider('env-1', WRITE)).rejects.toMatchObject({ status: 400 });
    });

    it('propagates 404 from get (other environment)', async () => {
        mockApimFetchJsonV1Env.mockRejectedValue(new ApimApiError(404, 'Not found'));
        await expect(getClientRegistrationProvider('env-other', 'prov-1')).rejects.toMatchObject({ status: 404 });
    });

    it('update PUTs Classic whitelist fields and omits id from the body', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(SAVED);
        await updateClientRegistrationProvider('env-1', 'prov-1', WRITE);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/applications/registration/providers/prov-1', {
            method: 'PUT',
            body: expect.any(String),
        });
        const body = JSON.parse(String(mockApimFetchJsonV1Env.mock.calls[0]?.[2]?.body)) as Record<string, unknown>;
        expect(Object.keys(body)).toEqual([...CLIENT_REGISTRATION_PROVIDER_UPDATE_FIELDS]);
        expect(body).not.toHaveProperty('id');
        expect(body.claim_mappings).toEqual({ org_id: 'metadata.organization' });
        expect(body.trust_store).toEqual({ type: 'NONE' });
        expect(body.key_store).toEqual({ type: 'NONE' });
    });

    it('delete calls DELETE .../providers/{id} with no body', async () => {
        await deleteClientRegistrationProvider('env-1', 'prov-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/applications/registration/providers/prov-1', {
            method: 'DELETE',
        });
    });
});
