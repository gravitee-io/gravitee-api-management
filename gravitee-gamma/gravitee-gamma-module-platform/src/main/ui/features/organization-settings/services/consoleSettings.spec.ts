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

import { getOrgConsoleSettings, saveOrgConsoleSettings } from './consoleSettings';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../types/consoleSettings';

jest.mock('../../../shared/api/apimClient', () => ({ apimFetchJsonOrg: jest.fn() }));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('org console settings service', () => {
    afterEach(() => jest.clearAllMocks());

    it('GETs /settings', async () => {
        const response: ConsoleSettings = { management: { title: 'Console' } };
        mockApimFetchJsonOrg.mockResolvedValue(response);

        const result = await getOrgConsoleSettings();

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/settings');
        expect(result).toEqual(response);
    });

    it('POSTs the full entity to /settings', async () => {
        const payload: ConsoleSettings = { email: { password: PASSWORD_SENTINEL } };
        mockApimFetchJsonOrg.mockResolvedValue(payload);

        const result = await saveOrgConsoleSettings(payload);

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/settings', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
        expect(result).toEqual(payload);
    });
});
