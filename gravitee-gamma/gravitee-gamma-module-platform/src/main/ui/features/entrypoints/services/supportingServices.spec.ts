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
import { listOrgEnvironments } from './environments';
import { listOrgGroups } from './groups';
import { getPortalSettingsByEnvironmentId, savePortalSettingsByEnvironmentId } from './portalSettings';
import { listOrgTags } from './tags';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('entrypoint supporting services', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue([]);
    });

    it('listOrgEnvironments calls GET /environments', async () => {
        await listOrgEnvironments();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments');
    });

    it('getPortalSettingsByEnvironmentId calls GET /environments/:id/settings', async () => {
        await getPortalSettingsByEnvironmentId('env-1');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments/env-1/settings');
    });

    it('URL-encodes the environment id in portal settings path', async () => {
        await getPortalSettingsByEnvironmentId('env with spaces');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments/env%20with%20spaces/settings');
    });

    it('savePortalSettingsByEnvironmentId calls POST /environments/:id/settings', async () => {
        const settings = { portal: { entrypoint: 'https://api.example.com', tcpPort: 4082 } };
        await savePortalSettingsByEnvironmentId('env-1', settings);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments/env-1/settings', {
            method: 'POST',
            body: JSON.stringify(settings),
        });
    });

    it('URL-encodes the environment id when saving portal settings', async () => {
        const settings = { portal: { entrypoint: 'https://api.example.com' } };
        await savePortalSettingsByEnvironmentId('env with spaces', settings);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments/env%20with%20spaces/settings', {
            method: 'POST',
            body: JSON.stringify(settings),
        });
    });

    it('listOrgTags calls GET /configuration/tags', async () => {
        await listOrgTags();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/tags');
    });

    it('listOrgGroups calls GET /groups', async () => {
        await listOrgGroups();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/groups');
    });
});
