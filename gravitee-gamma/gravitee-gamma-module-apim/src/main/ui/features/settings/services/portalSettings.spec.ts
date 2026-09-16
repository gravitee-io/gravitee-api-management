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
import { getEnvironmentPortalConfiguration, getEnvironmentPortalSettings } from './portalSettings';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('portalSettings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue({});
    });

    it('loads gateway settings from GET /settings', async () => {
        await getEnvironmentPortalSettings('env-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/settings');
    });

    it('loads apiScore.enabled from GET /portal', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce({ apiScore: { enabled: true } });
        const config = await getEnvironmentPortalConfiguration('env-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/portal');
        expect(config.apiScore?.enabled).toBe(true);
    });
});
