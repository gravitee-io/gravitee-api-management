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
import { duplicateApi, exportApiDefinition } from './apis';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackBlobGet, trackHandler } from '../../../testing/helpers';

const EXPORT_DEFINITION_PATH = `${TEST_V2_BASE}/apis/:apiId/_export/definition`;
const DUPLICATE_PATH = `${TEST_V2_BASE}/apis/:apiId/_duplicate`;

describe('apis general actions', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('exportApiDefinition calls v2 export with excludeAdditionalData', async () => {
        const tracker = trackBlobGet(EXPORT_DEFINITION_PATH, { id: 'api-1', name: 'My API' });

        await exportApiDefinition('DEFAULT', 'api-1', ['members']);

        expect(tracker.callCount).toBe(1);
        const url = new URL(tracker.lastCall!.url);
        expect(url.pathname).toContain('/apis/api-1/_export/definition');
        expect(url.searchParams.get('excludeAdditionalData')).toBe('members');
    });

    it('duplicateApi posts to v2 duplicate endpoint', async () => {
        const tracker = trackHandler('post', DUPLICATE_PATH, { id: 'api-2', name: 'copy' });

        await duplicateApi('DEFAULT', 'api-1', {
            version: 'v2',
            contextPath: '/duplicate',
            filteredFields: ['MEMBERS'],
        });

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({
            version: 'v2',
            contextPath: '/duplicate',
            filteredFields: ['MEMBERS'],
        });
    });
});
