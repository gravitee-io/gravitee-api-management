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
import { getApi, getApiPage, listApiPages } from './agent';
import { buildApi, buildPage, buildPagesResponse, TEST_PORTAL_API } from '../testing/factories';
import { trackPortalPath } from '../testing/helpers';

describe('getApi', () => {
    it('should get the agent from the portal API', async () => {
        const tracker = trackPortalPath('get', '/apis/api-helpdesk', buildApi());

        const api = await getApi('api-helpdesk');

        expect(api.name).toBe('IT Helpdesk Agent');
        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toBe(`${TEST_PORTAL_API}/apis/api-helpdesk`);
    });
});

describe('listApiPages', () => {
    it('should list all non-homepage pages', async () => {
        const tracker = trackPortalPath('get', '/apis/api-helpdesk/pages', buildPagesResponse([buildPage()]));

        const response = await listApiPages('api-helpdesk');

        expect(response.data).toHaveLength(1);
        expect(tracker.lastCall?.url).toContain('/apis/api-helpdesk/pages');
        expect(tracker.lastCall?.url).toContain('homepage=false');
        expect(tracker.lastCall?.url).toContain('size=-1');
    });
});

describe('getApiPage', () => {
    it('should get a page including content', async () => {
        const tracker = trackPortalPath('get', '/apis/api-helpdesk/pages/page-overview', buildPage());

        const page = await getApiPage('api-helpdesk', 'page-overview');

        expect(page.name).toBe('Overview');
        expect(tracker.lastCall?.url).toContain('include=content');
    });
});
