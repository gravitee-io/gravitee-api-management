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

import { getIntegration } from './integrationDetail';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('integration detail service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    it('gets the integration by id for the environment', async () => {
        await getIntegration('env-1', 'int-1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/int-1');
    });

    it('percent-encodes reserved characters in the integration id', async () => {
        await getIntegration('env-1', 'a/b c');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c');
    });
});
