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
import type { PortalTenant, PortalTenantFeatures } from '../types/portal-tenant.types';

function collectDescendantIds(navItems: readonly PortalNavigationItem[], rootId: string): Set<string> {
    const ids = new Set<string>([rootId]);
    let changed = true;

    while (changed) {
        changed = false;
        for (const item of navItems) {
            if (item.parentId && ids.has(item.parentId) && !ids.has(item.id)) {
                ids.add(item.id);
                changed = true;
            }
        }
    }

    return ids;
}

function isApiAllowed(tenant: PortalTenant, apiId: string): boolean {
    if (tenant.apiAccessMode === 'all') {
        return true;
    }

    return tenant.allowedApiIds.includes(apiId);
}

function matchesFeatureNavItem(item: PortalNavigationItem, features: PortalTenantFeatures): boolean {
    const slug = item.slug.toLowerCase();
    const title = item.title.toLowerCase();

    if (!features.catalog && (slug.includes('api-reference') || title.includes('api catalog'))) {
        return false;
    }

    if (!features.subscriptions && (slug.includes('subscribe') || slug.includes('subscription') || title === 'subscribe')) {
        return false;
    }

    if (!features.dashboard && (slug.includes('application') || title.includes('my applications'))) {
        return false;
    }

    if (!features.documentation) {
        if (slug.includes('getting-started') || slug.includes('guides') || title === 'guides') {
            return false;
        }

        if (item.parentId && slug.includes('quick-start')) {
            return false;
        }

        if (
            title.includes('quick start')
            || title.includes('authentication')
            || title.includes('webhook')
            || title.includes('error handling')
            || title.includes('processing payment')
        ) {
            return false;
        }
    }

    return true;
}

export function filterNavItemsForTenantPreview(
    navItems: readonly PortalNavigationItem[],
    tenant: PortalTenant,
): PortalNavigationItem[] {
    const hiddenIds = new Set<string>();

    for (const item of navItems) {
        if (item.type === 'API' && !isApiAllowed(tenant, item.apiId)) {
            for (const id of collectDescendantIds(navItems, item.id)) {
                hiddenIds.add(id);
            }
        }

        if (!matchesFeatureNavItem(item, tenant.features)) {
            for (const id of collectDescendantIds(navItems, item.id)) {
                hiddenIds.add(id);
            }
        }
    }

    return navItems.filter(item => !hiddenIds.has(item.id));
}

export function isApiVisibleInTenantPreview(tenant: PortalTenant, apiId: string): boolean {
    return isApiAllowed(tenant, apiId);
}

export function countEnabledFeatures(features: PortalTenantFeatures): number {
    return Object.values(features).filter(Boolean).length;
}
