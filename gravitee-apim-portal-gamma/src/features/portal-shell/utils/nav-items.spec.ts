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
import type { PortalNavigationItem } from '../../portals/types';
import {
    canPublishNavItem,
    filterVisibleNavItems,
    isNavItemPublished,
    isNavItemVisible,
} from './nav-items';

const folder: PortalNavigationItem = {
    id: 'folder-1',
    portalId: 'p1',
    title: 'Guides',
    type: 'FOLDER',
    parentId: null,
    order: 0,
    slug: 'guides',
};

const publishedPage: PortalNavigationItem = {
    id: 'page-1',
    portalId: 'p1',
    title: 'Getting Started',
    type: 'PAGE',
    parentId: 'folder-1',
    order: 0,
    slug: 'getting-started',
    published: true,
};

const unpublishedPage: PortalNavigationItem = {
    id: 'page-2',
    portalId: 'p1',
    title: 'Draft Page',
    type: 'PAGE',
    parentId: 'folder-1',
    order: 1,
    slug: 'draft-page',
    published: false,
};

const unpublishedFolder: PortalNavigationItem = {
    ...folder,
    published: false,
};

const childOfUnpublishedFolder: PortalNavigationItem = {
    id: 'page-3',
    portalId: 'p1',
    title: 'Child Page',
    type: 'PAGE',
    parentId: 'folder-1',
    order: 0,
    slug: 'child-page',
    published: false,
};

describe('nav-items published helpers', () => {
    it('should treat missing published field as published', () => {
        expect(isNavItemPublished(folder)).toBe(true);
    });

    it('should detect explicitly unpublished items', () => {
        expect(isNavItemPublished(unpublishedPage)).toBe(false);
    });

    it('should hide unpublished items from visibility check', () => {
        expect(isNavItemVisible(unpublishedPage, [folder, unpublishedPage])).toBe(false);
    });

    it('should hide children of unpublished parents even when child is published', () => {
        const items = [unpublishedFolder, childOfUnpublishedFolder];
        expect(isNavItemVisible(childOfUnpublishedFolder, items)).toBe(false);
    });

    it('should filter visible nav items', () => {
        const items = [folder, publishedPage, unpublishedPage];
        expect(filterVisibleNavItems(items, items)).toEqual([folder, publishedPage]);
    });

    it('should allow publishing when parent is published', () => {
        expect(canPublishNavItem(unpublishedPage, [folder, unpublishedPage])).toEqual({ allowed: true });
    });

    it('should block publishing when parent is unpublished', () => {
        const items = [unpublishedFolder, childOfUnpublishedFolder];
        expect(canPublishNavItem(childOfUnpublishedFolder, items)).toEqual({
            allowed: false,
            reason: 'A navigation item cannot be published within an unpublished folder',
        });
    });
});
