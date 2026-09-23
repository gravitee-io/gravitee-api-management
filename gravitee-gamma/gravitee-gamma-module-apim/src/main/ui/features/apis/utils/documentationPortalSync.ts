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
import type { DocumentationPage, PortalNavigationItem } from '../types/documentation';
import { fromPortalContentType, normalizeParentId } from './documentationFormatters';

function byId<T extends { id?: string }>(items: T[]): Map<string, T> {
    return new Map(items.filter(item => item.id).map(item => [item.id!, item]));
}

export function documentationPathKey(pages: DocumentationPage[], page: DocumentationPage): string {
    const items = byId(pages);
    const parts: string[] = [];
    let current: DocumentationPage | undefined = page;
    const seen = new Set<string>();
    while (current?.id && !seen.has(current.id)) {
        seen.add(current.id);
        parts.unshift((current.name ?? '').trim().toLowerCase());
        const parent = normalizeParentId(current.parentId);
        current = parent ? items.get(parent) : undefined;
    }
    return parts.join('\0');
}

export function navigationPathKey(items: PortalNavigationItem[], item: PortalNavigationItem, apiNavId: string): string {
    const byNavId = byId(items);
    const parts: string[] = [];
    let current: PortalNavigationItem | undefined = item;
    const seen = new Set<string>();
    while (current?.id && current.id !== apiNavId && !seen.has(current.id)) {
        seen.add(current.id);
        parts.unshift((current.title ?? '').trim().toLowerCase());
        if (!current.parentId || current.parentId === apiNavId) break;
        current = byNavId.get(current.parentId);
    }
    return parts.join('\0');
}

export function isDescendantOfApi(items: PortalNavigationItem[], item: PortalNavigationItem, apiNavId: string): boolean {
    const byNavId = byId(items);
    let current: PortalNavigationItem | undefined = item;
    const seen = new Set<string>();
    while (current?.parentId && !seen.has(current.id ?? '')) {
        if (current.id) seen.add(current.id);
        if (current.parentId === apiNavId) return true;
        current = byNavId.get(current.parentId);
    }
    return false;
}

export function isUnderApiNode(items: PortalNavigationItem[], item: PortalNavigationItem): boolean {
    const byNavId = byId(items);
    let current: PortalNavigationItem | undefined = item;
    const seen = new Set<string>();
    while (current?.parentId && current.id && !seen.has(current.id)) {
        seen.add(current.id);
        const parent = byNavId.get(current.parentId);
        if (parent?.type === 'API') return true;
        current = parent;
    }
    return false;
}

export function apiDocumentationNavItems(items: PortalNavigationItem[], apiNavId: string): PortalNavigationItem[] {
    return items.filter(
        item =>
            item.id !== apiNavId &&
            (item.type === 'FOLDER' || item.type === 'PAGE') &&
            isDescendantOfApi(items, item, apiNavId),
    );
}

export function mapNavItemToPage(
    item: PortalNavigationItem,
    apiNavId: string,
    pageType?: DocumentationPage['type'],
): DocumentationPage {
    const parentId = !item.parentId || item.parentId === apiNavId ? undefined : item.parentId;
    return {
        id: item.id,
        name: item.title,
        type: item.type === 'FOLDER' ? 'FOLDER' : (pageType ?? fromPortalContentType(item.contentType) ?? 'MARKDOWN'),
        published: item.published,
        visibility: item.visibility,
        order: item.order,
        parentId,
        portalNavId: item.id,
        portalPageContentId: item.portalPageContentId,
    };
}

export function mergeDocumentationWithPortal(
    classicPages: DocumentationPage[],
    navItems: PortalNavigationItem[],
    apiNavId: string,
): DocumentationPage[] {
    const docs = apiDocumentationNavItems(navItems, apiNavId);
    const classicByPath = new Map(classicPages.filter(page => page.id).map(page => [documentationPathKey(classicPages, page), page]));
    const usedClassic = new Set<string>();
    const merged: DocumentationPage[] = [];

    for (const nav of docs) {
        const path = navigationPathKey(navItems, nav, apiNavId);
        const classic = classicByPath.get(path);
        if (classic?.id) {
            usedClassic.add(classic.id);
            merged.push({
                ...classic,
                name: nav.title ?? classic.name,
                published: nav.published ?? classic.published,
                visibility: nav.visibility ?? classic.visibility,
                order: nav.order ?? classic.order,
                portalNavId: nav.id,
                portalPageContentId: nav.portalPageContentId ?? classic.portalPageContentId,
            });
        } else {
            merged.push(mapNavItemToPage(nav, apiNavId));
        }
    }

    for (const page of classicPages) {
        if (page.id && !usedClassic.has(page.id)) merged.push(page);
    }
    return merged;
}

export function breadcrumbFor(pages: DocumentationPage[], parentId: string | null): { id: string; name: string; position: number }[] {
    const items = byId(pages);
    const crumbs: { id: string; name: string; position: number }[] = [];
    let current = parentId ? items.get(parentId) : undefined;
    const seen = new Set<string>();
    while (current?.id && !seen.has(current.id)) {
        seen.add(current.id);
        crumbs.unshift({ id: current.id, name: current.name ?? 'Folder', position: 0 });
        const parent = normalizeParentId(current.parentId);
        current = parent ? items.get(parent) : undefined;
    }
    return crumbs.map((crumb, index) => ({ ...crumb, position: index + 1 }));
}
