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
import type { BlockNoteDocument, PageContent, PortalNavigationItem } from '../types';
import { PETSTORE_OPENAPI_SPEC } from '../../editor/services/openapi.service';

export function createPlaceholderDocument(title: string): BlockNoteDocument {
    return [
        {
            type: 'heading',
            props: { level: 1 },
            content: [{ type: 'text', text: title, styles: {} }],
            children: [],
        },
        {
            type: 'paragraph',
            content: [{ type: 'text', text: 'Welcome to this page.', styles: {} }],
            children: [],
        },
    ];
}

function scopedNavId(portalId: string, key: string): string {
    return `${portalId}-${key}`;
}

export function createDummyNavigation(portalId: string): PortalNavigationItem[] {
    const guidesId = scopedNavId(portalId, 'nav-guides');

    return [
        {
            id: scopedNavId(portalId, 'nav-getting-started'),
            portalId,
            title: 'Getting Started',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: 'getting-started-nav001',
        },
        {
            id: scopedNavId(portalId, 'nav-api-reference'),
            portalId,
            title: 'API Reference',
            type: 'PAGE',
            parentId: null,
            order: 1,
            slug: 'api-reference-nav002',
        },
        {
            id: guidesId,
            portalId,
            title: 'Guides',
            type: 'FOLDER',
            parentId: null,
            order: 2,
            slug: 'guides-nav003',
        },
        {
            id: scopedNavId(portalId, 'nav-quick-start'),
            portalId,
            title: 'Quick Start',
            type: 'PAGE',
            parentId: guidesId,
            order: 0,
            slug: 'quick-start-nav004',
        },
        {
            id: scopedNavId(portalId, 'nav-authentication'),
            portalId,
            title: 'Authentication',
            type: 'PAGE',
            parentId: guidesId,
            order: 1,
            slug: 'authentication-nav005',
        },
        {
            id: scopedNavId(portalId, 'nav-petstore-openapi'),
            portalId,
            title: 'Petstore API',
            type: 'PAGE',
            contentType: 'OPENAPI',
            renderer: 'swagger',
            specSource: { type: 'INLINE', content: PETSTORE_OPENAPI_SPEC },
            parentId: null,
            order: 3,
            slug: 'petstore-api-nav006',
        },
        {
            id: scopedNavId(portalId, 'footer-docs'),
            portalId,
            title: 'Documentation',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'documentation-footer001',
            url: 'https://docs.example.com',
            area: 'FOOTER',
        },
        {
            id: scopedNavId(portalId, 'footer-support'),
            portalId,
            title: 'Support',
            type: 'LINK',
            parentId: null,
            order: 1,
            slug: 'support-footer002',
            url: 'https://support.example.com',
            area: 'FOOTER',
        },
        {
            id: scopedNavId(portalId, 'menu-profile'),
            portalId,
            title: 'Profile',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'profile-menu001',
            url: '/profile',
            area: 'USER_MENU',
        },
        {
            id: scopedNavId(portalId, 'menu-logout'),
            portalId,
            title: 'Log out',
            type: 'LINK',
            parentId: null,
            order: 1,
            slug: 'log-out-menu002',
            url: '/logout',
            area: 'USER_MENU',
        },
    ];
}

export function createDummyPageContents(portalId: string, navItems: readonly PortalNavigationItem[]): PageContent[] {
    return navItems
        .filter((item): item is PortalNavigationItem & { type: 'PAGE' } => item.type === 'PAGE')
        .map(item => {
            if (item.contentType === 'OPENAPI') {
                return {
                    id: `page-content-${item.id}`,
                    portalId,
                    navigationItemId: item.id,
                    contentType: 'OPENAPI',
                    renderer: item.renderer,
                    specContent:
                        item.specSource.type === 'INLINE' ? item.specSource.content : PETSTORE_OPENAPI_SPEC,
                };
            }

            return {
                id: `page-content-${item.id}`,
                portalId,
                navigationItemId: item.id,
                contentType: 'BLOCK',
                document: createPlaceholderDocument(item.title),
            };
        });
}
