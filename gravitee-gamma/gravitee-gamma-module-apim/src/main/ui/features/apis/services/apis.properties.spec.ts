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

import { updateApiProperties } from './apis';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import type { Property } from '../types';

const API_PATH = `${TEST_V2_BASE}/apis/api-1`;

describe('updateApiProperties', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('applies the change to the properties of the freshly read API, so a property synced since the page loaded is kept', async () => {
        trackHandler('get', API_PATH, {
            id: 'api-1',
            name: 'My API',
            properties: [
                { key: 'backend.timeout', value: '5000' },
                { key: 'synced.host', value: 'http://source', dynamic: true },
            ],
        });
        const put = trackHandler('put', API_PATH, {});

        await updateApiProperties('DEFAULT', 'api-1', current =>
            current.map(property => (property.key === 'backend.timeout' ? { ...property, value: '8000' } : property)),
        );

        expect(put.callCount).toBe(1);
        expect(put.lastCall?.body).toEqual({
            id: 'api-1',
            name: 'My API',
            properties: [
                { key: 'backend.timeout', value: '8000' },
                { key: 'synced.host', value: 'http://source', dynamic: true },
            ],
        });
    });

    it('hands the change an empty list when the API has no property yet', async () => {
        trackHandler('get', API_PATH, { id: 'api-1', name: 'My API' });
        const put = trackHandler('put', API_PATH, {});

        await updateApiProperties('DEFAULT', 'api-1', current => [...current, { key: 'first', value: '1' }]);

        expect(put.lastCall?.body).toEqual({ id: 'api-1', name: 'My API', properties: [{ key: 'first', value: '1' }] });
    });

    it('sends nothing when the change rejects, for instance because its property was removed by a sync', async () => {
        trackHandler('get', API_PATH, { id: 'api-1', name: 'My API', properties: [] });
        const put = trackHandler('put', API_PATH, {});
        const changeOfMissingProperty = (current: Property[]) => {
            if (!current.some(property => property.key === 'gone')) throw new Error('Property "gone" no longer exists');
            return current;
        };

        await expect(updateApiProperties('DEFAULT', 'api-1', changeOfMissingProperty)).rejects.toThrow('Property "gone" no longer exists');

        expect(put.callCount).toBe(0);
    });
});
