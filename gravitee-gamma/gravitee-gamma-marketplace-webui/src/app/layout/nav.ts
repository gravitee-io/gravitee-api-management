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
import type { NavGroup } from '@gravitee/graphene-core';
import { FolderOpenIcon, HomeIcon, LayoutGridIcon } from '@gravitee/graphene-core/icons';

import type { Category } from '../../api/types';

export const HOME_NAV_KEY = 'home';
export const CATALOG_NAV_KEY = 'catalog';

const CATEGORY_PREFIX = 'category:';

export function categoryNavKey(categoryId: string): string {
    return `${CATEGORY_PREFIX}${categoryId}`;
}

export function parseCategoryNavKey(key: string): string | undefined {
    if (!key.startsWith(CATEGORY_PREFIX)) {
        return undefined;
    }
    return key.slice(CATEGORY_PREFIX.length);
}

export function getActiveNavKey(pathname: string, searchParams: URLSearchParams): string {
    if (pathname === '/') {
        return HOME_NAV_KEY;
    }
    if (pathname.startsWith('/catalog')) {
        const category = searchParams.get('category');
        if (category) {
            return categoryNavKey(category);
        }
        return CATALOG_NAV_KEY;
    }
    return '';
}

export function pathForNavKey(key: string): string {
    if (key === HOME_NAV_KEY) {
        return '/';
    }
    if (key === CATALOG_NAV_KEY) {
        return '/catalog';
    }
    const categoryId = parseCategoryNavKey(key);
    if (categoryId) {
        return `/catalog?category=${encodeURIComponent(categoryId)}`;
    }
    return '/';
}

export function buildNavGroups(categories: readonly Category[]): NavGroup[] {
    const groups: NavGroup[] = [
        {
            label: 'Browse',
            items: [
                { key: HOME_NAV_KEY, title: 'Home', icon: HomeIcon },
                { key: CATALOG_NAV_KEY, title: 'Catalog', icon: LayoutGridIcon },
            ],
        },
    ];

    if (categories.length > 0) {
        groups.push({
            label: 'Categories',
            items: categories.map(category => ({
                key: categoryNavKey(category.id),
                title: category.name ?? category.id,
                icon: FolderOpenIcon,
            })),
        });
    }

    return groups;
}

export function breadcrumbSegments(pathname: string): Array<{ label: string; to?: string }> {
    if (pathname === '/') {
        return [{ label: 'Home' }];
    }
    if (pathname.startsWith('/catalog')) {
        return [{ label: 'Home', to: '/' }, { label: 'Catalog' }];
    }
    if (pathname.startsWith('/dashboard')) {
        return [{ label: 'Home', to: '/' }, { label: 'Dashboard' }];
    }
    return [{ label: 'Home', to: '/' }];
}
