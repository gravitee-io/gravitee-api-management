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

import { createTenant, deleteTenant, listTenants, updateTenant } from './tenants';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NewTenantPayload, UpdateTenantPayload } from '../types/tenant';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('tenants service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue([]);
    });

    it('calls GET on the organization configuration/tenants resource', async () => {
        await listTenants();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tenants');
    });

    it('POSTs a new tenant wrapped in an array', async () => {
        const payload: NewTenantPayload = { name: 'US East', key: 'us-east', description: 'Virginia' };
        await createTenant(payload);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tenants', {
            method: 'POST',
            body: JSON.stringify([payload]),
        });
    });

    it('PUTs an updated tenant wrapped in an array', async () => {
        const payload: UpdateTenantPayload = { key: 'us-east', name: 'US East', description: 'Updated' };
        await updateTenant(payload);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tenants', {
            method: 'PUT',
            body: JSON.stringify([payload]),
        });
    });

    it('DELETEs a tenant by key', async () => {
        await deleteTenant('us-east');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tenants/us-east', {
            method: 'DELETE',
        });
    });

    it('encodes special characters in the delete path key', async () => {
        await deleteTenant('us/east');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tenants/us%2Feast', {
            method: 'DELETE',
        });
    });
});
