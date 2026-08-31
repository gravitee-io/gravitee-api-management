/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { buildCatalogSearchPath, matchesLabel, matchesProtocol, searchApis } from './catalog';
import { buildApi, buildApisResponse, TEST_PORTAL_API } from '../testing/factories';
import { trackPortalPath } from '../testing/helpers';

describe('buildCatalogSearchPath', () => {
    it('should include pagination and omit empty filters', () => {
        expect(buildCatalogSearchPath({ page: 2, size: 12 })).toBe('/apis/_search?page=2&size=12');
    });

    it('should include query and category when set', () => {
        expect(buildCatalogSearchPath({ page: 1, size: 12, query: 'ticket triage', category: 'it' })).toBe(
            '/apis/_search?page=1&size=12&q=ticket+triage&category=it',
        );
    });
});

describe('searchApis', () => {
    it('should post to the portal search endpoint', async () => {
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));

        const response = await searchApis({ page: 1, size: 12, query: 'helpdesk', category: 'it' });

        expect(response.data).toHaveLength(1);
        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toContain('q=helpdesk');
        expect(tracker.lastCall?.url).toContain('category=it');
        expect(tracker.lastCall?.url).toContain('page=1');
        expect(tracker.lastCall?.url).toContain(`${TEST_PORTAL_API}/apis/_search`);
    });
});

describe('catalog client filters', () => {
    it('should match protocol only when a protocol is selected', () => {
        expect(matchesProtocol('A2A_PROXY', '')).toBe(true);
        expect(matchesProtocol('A2A_PROXY', 'A2A_PROXY')).toBe(true);
        expect(matchesProtocol('MCP_PROXY', 'A2A_PROXY')).toBe(false);
    });

    it('should match label only when a label is selected', () => {
        expect(matchesLabel(['ops'], '')).toBe(true);
        expect(matchesLabel(['ops', 'it'], 'ops')).toBe(true);
        expect(matchesLabel(['it'], 'ops')).toBe(false);
        expect(matchesLabel(undefined, 'ops')).toBe(false);
    });
});
