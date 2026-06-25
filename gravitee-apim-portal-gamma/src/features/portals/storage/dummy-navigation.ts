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

export function createDummyNavigation(portalId: string): PortalNavigationItem[] {
    return [
        {
            id: 'nav-getting-started',
            portalId,
            title: 'Getting Started',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: 'getting-started-nav001',
        },
        {
            id: 'nav-api-reference',
            portalId,
            title: 'API Reference',
            type: 'PAGE',
            parentId: null,
            order: 1,
            slug: 'api-reference-nav002',
        },
        {
            id: 'nav-guides',
            portalId,
            title: 'Guides',
            type: 'FOLDER',
            parentId: null,
            order: 2,
            slug: 'guides-nav003',
        },
        {
            id: 'nav-quick-start',
            portalId,
            title: 'Quick Start',
            type: 'PAGE',
            parentId: 'nav-guides',
            order: 0,
            slug: 'quick-start-nav004',
        },
        {
            id: 'nav-authentication',
            portalId,
            title: 'Authentication',
            type: 'PAGE',
            parentId: 'nav-guides',
            order: 1,
            slug: 'authentication-nav005',
        },
    ];
}

export function createDummyPageContents(portalId: string, navItems: readonly PortalNavigationItem[]): PageContent[] {
    return navItems
        .filter((item): item is PortalNavigationItem & { type: 'PAGE' } => item.type === 'PAGE')
        .map(item => ({
            id: `page-content-${item.id}`,
            portalId,
            navigationItemId: item.id,
            document: createPlaceholderDocument(item.title),
        }));
}
