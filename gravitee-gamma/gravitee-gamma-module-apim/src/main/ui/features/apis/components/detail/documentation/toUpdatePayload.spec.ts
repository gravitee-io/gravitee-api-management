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
import { toUpdatePayload } from './toUpdatePayload';
import type {
    PortalNavigationFolder,
    PortalNavigationItemSource,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../../types/apiDocumentation';

const SOURCE: PortalNavigationItemSource = {
    type: 'github-fetcher',
    configuration: { owner: 'gravitee-io', repository: 'docs', filepath: 'petstore.yaml' },
    useAutoFetch: true,
    fetchCron: '0 */10 * * * *',
    lastFetchedAt: '2026-10-01T00:00:00.000Z',
};

const BASE = {
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    area: 'TOP_NAVBAR',
    order: 2,
    published: true,
    visibility: 'PUBLIC',
} as const;

const SYNCED_PAGE: PortalNavigationPage = {
    ...BASE,
    id: 'petstore',
    title: 'Petstore',
    type: 'PAGE',
    parentId: 'guides',
    rootId: 'guides',
    portalPageContentId: 'content-1',
    source: SOURCE,
};

const SYNCED_FOLDER: PortalNavigationFolder = { ...BASE, id: 'specs', title: 'Specs', type: 'FOLDER', rootId: 'specs', source: SOURCE };

const LINK: PortalNavigationLink = {
    ...BASE,
    id: 'status',
    title: 'Status',
    type: 'LINK',
    rootId: 'status',
    url: 'https://status.example.com',
};

describe('toUpdatePayload', () => {
    it('sends every field of a page back unchanged when nothing changes', () => {
        expect(toUpdatePayload(SYNCED_PAGE)).toEqual({
            type: 'PAGE',
            title: 'Petstore',
            order: 2,
            published: true,
            visibility: 'PUBLIC',
            parentId: 'guides',
            source: SOURCE,
        });
    });

    it("keeps a page's external source when its title and access change", () => {
        expect(toUpdatePayload(SYNCED_PAGE, { title: 'Pet store', visibility: 'PRIVATE' })).toEqual({
            type: 'PAGE',
            title: 'Pet store',
            order: 2,
            published: true,
            visibility: 'PRIVATE',
            parentId: 'guides',
            source: SOURCE,
        });
    });

    it("keeps a folder's external source when it is hidden", () => {
        expect(toUpdatePayload(SYNCED_FOLDER, { published: false })).toEqual({
            type: 'FOLDER',
            title: 'Specs',
            order: 2,
            published: false,
            visibility: 'PUBLIC',
            parentId: undefined,
            source: SOURCE,
        });
    });

    it("keeps a link's address when it is moved", () => {
        expect(toUpdatePayload(LINK, { parentId: 'guides', order: 0 })).toEqual({
            type: 'LINK',
            title: 'Status',
            order: 0,
            published: true,
            visibility: 'PUBLIC',
            parentId: 'guides',
            url: 'https://status.example.com',
        });
    });
});
