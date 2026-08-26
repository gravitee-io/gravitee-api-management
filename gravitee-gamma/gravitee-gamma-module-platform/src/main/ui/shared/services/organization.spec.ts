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
import { getOrganization, updateOrganization } from './organization';
import { apimFetchJsonOrg } from '../api/apimClient';

jest.mock('../api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('platform policies service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue({ id: 'DEFAULT' });
    });

    it('reads the platform flows from the organization entity', async () => {
        await getOrganization();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('');
    });

    it('PUTs the whole organization entity back', async () => {
        const organization = { id: 'DEFAULT', name: 'Gravitee', flowMode: 'BEST_MATCH' as const, flows: [] };
        await updateOrganization(organization);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('', { method: 'PUT', body: JSON.stringify(organization) });
    });
});
