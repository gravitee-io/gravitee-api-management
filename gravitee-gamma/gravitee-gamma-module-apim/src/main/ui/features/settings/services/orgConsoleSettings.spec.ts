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
import { fetchOrgConsoleSettings } from './orgConsoleSettings';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const ORG_BASE = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}`;

describe('fetchOrgConsoleSettings', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('reads the org-scoped console configuration', async () => {
        const tracker = trackHandler('get', `${ORG_BASE}/console`, { federation: { enabled: true } });

        const settings = await fetchOrgConsoleSettings();

        expect(settings.federation?.enabled).toBe(true);
        expect(tracker.callCount).toBe(1);
        expect(new URL(tracker.lastCall!.url).pathname).toBe('/management/organizations/DEFAULT/console');
    });
});
