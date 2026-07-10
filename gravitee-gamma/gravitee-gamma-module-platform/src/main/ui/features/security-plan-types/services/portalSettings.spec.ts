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
import { getPortalSettings, savePortalSettings } from './portalSettings';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({ apimFetchJsonV1Env: jest.fn() }));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('portalSettings service', () => {
    afterEach(() => jest.clearAllMocks());

    describe('getPortalSettings', () => {
        it('calls the settings endpoint for the given environment', async () => {
            const mockResponse = { plan: { security: { apikey: { enabled: true } } } };
            mockApimFetchJsonV1Env.mockResolvedValue(mockResponse);

            const result = await getPortalSettings('env-1');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/settings');
            expect(result).toEqual(mockResponse);
        });
    });

    describe('savePortalSettings', () => {
        it('posts the settings payload to the environment settings endpoint', async () => {
            const payload = { plan: { security: { jwt: { enabled: false } } } };
            const mockResponse = { plan: { security: { jwt: { enabled: false } } } };
            mockApimFetchJsonV1Env.mockResolvedValue(mockResponse);

            const result = await savePortalSettings('env-2', payload);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-2', '/settings', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
            expect(result).toEqual(mockResponse);
        });
    });
});
