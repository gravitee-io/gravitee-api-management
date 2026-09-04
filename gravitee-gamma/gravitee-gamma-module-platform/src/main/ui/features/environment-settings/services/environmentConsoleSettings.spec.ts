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

import { getEnvironmentConsoleSettings, resetEnvironmentBrandedSenders, saveEnvironmentConsoleSettings } from './environmentConsoleSettings';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../../organization-settings/types/consoleSettings';

jest.mock('../../../shared/api/apimClient', () => ({ apimFetchJsonV1Env: jest.fn() }));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('environment console settings service', () => {
    afterEach(() => jest.clearAllMocks());

    it('GETs the environment-scoped /settings', async () => {
        const response: ConsoleSettings = { management: { title: 'Console' } };
        mockApimFetchJsonV1Env.mockResolvedValue(response);

        const result = await getEnvironmentConsoleSettings('DEFAULT');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/settings');
        expect(result).toEqual(response);
    });

    it('POSTs the full entity to the environment-scoped /settings', async () => {
        const payload: ConsoleSettings = { email: { password: PASSWORD_SENTINEL } };
        mockApimFetchJsonV1Env.mockResolvedValue(payload);

        const result = await saveEnvironmentConsoleSettings('DEFAULT', payload);

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/settings', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
        expect(result).toEqual(payload);
    });

    it('POSTs to the branded-senders reset resource', async () => {
        const response: ConsoleSettings = { email: { brandedSendersInherited: true } };
        mockApimFetchJsonV1Env.mockResolvedValue(response);

        const result = await resetEnvironmentBrandedSenders('DEFAULT');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/settings/email/branded-senders/reset', {
            method: 'POST',
        });
        expect(result).toEqual(response);
    });
});
