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
import { searchApplications } from './subscriptions';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const V1_ENV_BASE = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/environments/${TEST_CONFIG.environmentId}`;

describe('searchApplications', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('reads the API key mode from the v1 snake_case field', async () => {
        trackHandler('get', `${V1_ENV_BASE}/applications/_paged`, {
            data: [
                {
                    id: 'app-1',
                    name: 'Exclusive app',
                    type: 'SIMPLE',
                    api_key_mode: 'UNSPECIFIED',
                    owner: { displayName: 'admin' },
                },
            ],
            metadata: { pagination: { total: 1 } },
        });

        const page = await searchApplications('DEFAULT', 'Exclusive');

        expect(page.data[0]).toEqual(
            expect.objectContaining({
                id: 'app-1',
                apiKeyMode: 'UNSPECIFIED',
                primaryOwner: { displayName: 'admin' },
            }),
        );
    });
});
