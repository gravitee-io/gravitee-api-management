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
import { getEnvironmentPortalConfiguration, isCustomApiKeyEnabled, isSharedApiKeyEnabled } from './environmentPortal';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('environmentPortal', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue({});
    });

    it('detects shared API key feature flag', () => {
        expect(isSharedApiKeyEnabled({ plan: { security: { sharedApiKey: { enabled: true } } } })).toBe(true);
        expect(isSharedApiKeyEnabled({ plan: { security: { sharedApiKey: { enabled: false } } } })).toBe(false);
        expect(isSharedApiKeyEnabled(null)).toBe(false);
    });

    it('detects custom API keys only when the setting is explicitly enabled', () => {
        expect(isCustomApiKeyEnabled({ plan: { security: { customApiKey: { enabled: true } } } })).toBe(true);
        expect(isCustomApiKeyEnabled({ plan: { security: { customApiKey: { enabled: false } } } })).toBe(false);
        expect(isCustomApiKeyEnabled(null)).toBe(false);
    });

    it('loads apiScore.enabled from GET /portal', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce({ apiScore: { enabled: true } });
        const config = await getEnvironmentPortalConfiguration('env-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/portal');
        expect(config.apiScore?.enabled).toBe(true);
    });
});
