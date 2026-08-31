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
import { agentDetailBreadcrumbs, isAgentTab, pathForAgentTab } from './tabs';

describe('pathForAgentTab', () => {
    it('should use the agent path for overview and suffix for other tabs', () => {
        expect(pathForAgentTab('api-helpdesk', 'overview')).toBe('/catalog/api-helpdesk');
        expect(pathForAgentTab('api-helpdesk', 'docs')).toBe('/catalog/api-helpdesk/docs');
    });
});

describe('isAgentTab', () => {
    it('should accept known tabs only', () => {
        expect(isAgentTab('docs')).toBe(true);
        expect(isAgentTab('unknown')).toBe(false);
    });
});

describe('agentDetailBreadcrumbs', () => {
    it('should end on the agent name for overview', () => {
        expect(agentDetailBreadcrumbs('api-helpdesk', 'IT Helpdesk Agent', 'overview')).toEqual([
            { label: 'Home', to: '/' },
            { label: 'Catalog', to: '/catalog' },
            { label: 'IT Helpdesk Agent' },
        ]);
    });

    it('should append the tab label for documentation', () => {
        expect(agentDetailBreadcrumbs('api-helpdesk', 'IT Helpdesk Agent', 'docs')).toEqual([
            { label: 'Home', to: '/' },
            { label: 'Catalog', to: '/catalog' },
            { label: 'IT Helpdesk Agent', to: '/catalog/api-helpdesk' },
            { label: 'Documentation' },
        ]);
    });
});
