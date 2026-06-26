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
import type { PortalNavigationPage } from '../../portals/types';
import { isExternalUrl } from './link-target';

function portalPageSlugs(portalPages: readonly PortalNavigationPage[]): Set<string> {
    return new Set(portalPages.map(page => page.slug));
}

function extractSlugFromPortalPath(url: string, portalId?: string): string | null {
    const trimmed = url.trim();
    const withEdit = portalId
        ? new RegExp(`^/portals/${portalId}/edit/([^/]+)$`)
        : /^\/portals\/[^/]+\/edit\/([^/]+)$/;
    const withoutEdit = portalId
        ? new RegExp(`^/portals/${portalId}/([^/]+)$`)
        : /^\/portals\/[^/]+\/([^/]+)$/;

    const editMatch = trimmed.match(withEdit);
    if (editMatch) {
        return editMatch[1];
    }

    const viewMatch = trimmed.match(withoutEdit);
    if (viewMatch && viewMatch[1] !== 'edit') {
        return viewMatch[1];
    }

    return null;
}

export function isPortalPageSlug(url: string, portalPages: readonly PortalNavigationPage[]): boolean {
    const trimmed = url.trim();
    if (!trimmed || isExternalUrl(trimmed) || trimmed.includes('/')) {
        return false;
    }

    return portalPageSlugs(portalPages).has(trimmed);
}

export function parsePortalPageSlug(
    url: string,
    portalPages: readonly PortalNavigationPage[],
    portalId?: string,
): string | null {
    const trimmed = url.trim();
    if (!trimmed || isExternalUrl(trimmed)) {
        return null;
    }

    if (isPortalPageSlug(trimmed, portalPages)) {
        return trimmed;
    }

    const pathSlug = extractSlugFromPortalPath(trimmed, portalId);
    if (pathSlug && portalPageSlugs(portalPages).has(pathSlug)) {
        return pathSlug;
    }

    return null;
}

export function resolveUserMenuItemPath(
    url: string,
    portalPages: readonly PortalNavigationPage[],
    getPagePath: (slug: string) => string,
    portalId?: string,
): string {
    const slug = parsePortalPageSlug(url, portalPages, portalId);
    if (slug) {
        return getPagePath(slug);
    }

    return url.trim();
}

export function getUserMenuItemDisplayUrl(
    url: string,
    portalPages: readonly PortalNavigationPage[],
    portalId?: string,
): string {
    const slug = parsePortalPageSlug(url, portalPages, portalId);
    if (slug) {
        return slug;
    }

    return url;
}
