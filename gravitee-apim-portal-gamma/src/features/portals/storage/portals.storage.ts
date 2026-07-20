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
import type { DeveloperPortal, PortalDocumentationViewer } from '../types';
import { DEFAULT_DOCUMENTATION_VIEWER, DEFAULT_PORTAL_LABEL, PORTAL_DOCUMENTATION_VIEWERS } from '../types';
import { createDummyPortals } from './dummy-portals';
import { DB_NAME, DB_VERSION, PORTALS_STORE_NAME, runTransaction } from './db';
import { deleteNavItemsForPortal } from './navigation-items.storage';
import { deletePageContentsForPortal } from './page-contents.storage';
import { seedPortalFromTemplate } from './seed-portal-template';
import { seedCatalogDataIfEmpty } from './seed-catalog-data';
import { deleteTenantsForPortal } from '../../tenants/storage/portal-tenants.storage';
import { deleteMembersForTenant } from '../../tenants/storage/portal-tenant-members.storage';
import { getTenantsByPortalId } from '../../tenants/storage/portal-tenants.storage';
import { seedPortalTenantsForPortal } from '../../tenants/storage/seed-portal-tenants';
import { deleteCategoriesForPortal } from '../../settings/storage/portal-categories.storage';
import { deleteSubscriptionFormsForPortal } from '../../settings/storage/portal-subscription-forms.storage';
import { deleteIdentityProvidersForPortal } from '../../settings/storage/portal-identity-providers.storage';

export { DB_NAME, DB_VERSION } from './db';
export const STORE_NAME = PORTALS_STORE_NAME;

function normalizeDocumentationViewer(
    viewer: PortalDocumentationViewer | undefined,
): PortalDocumentationViewer {
    if (viewer && PORTAL_DOCUMENTATION_VIEWERS.includes(viewer)) {
        return viewer;
    }
    return DEFAULT_DOCUMENTATION_VIEWER;
}

function normalizePortal(portal: DeveloperPortal): DeveloperPortal {
    return {
        ...portal,
        description: portal.description ?? '',
        layout: portal.layout ?? 'header-content-footer',
        showFooter: portal.showFooter ?? true,
        pageWidth: portal.pageWidth ?? 'narrow',
        portalIconUrl: portal.portalIconUrl ?? '',
        portalLabel: portal.portalLabel ?? DEFAULT_PORTAL_LABEL,
        footerLinks: portal.footerLinks ?? [],
        userMenuItems: portal.userMenuItems ?? [],
        portalUrl: portal.portalUrl ?? '',
        documentationViewer: normalizeDocumentationViewer(portal.documentationViewer),
    };
}

export async function getAllPortals(): Promise<DeveloperPortal[]> {
    const portals = await runTransaction<DeveloperPortal[]>(PORTALS_STORE_NAME, 'readonly', store => store.getAll());
    return portals.map(normalizePortal);
}

export async function getPortal(id: string): Promise<DeveloperPortal | undefined> {
    const portal = await runTransaction<DeveloperPortal | undefined>(PORTALS_STORE_NAME, 'readonly', store => store.get(id));
    return portal ? normalizePortal(portal) : undefined;
}

export async function savePortal(portal: DeveloperPortal): Promise<void> {
    await runTransaction(PORTALS_STORE_NAME, 'readwrite', store => store.put(normalizePortal(portal)));
}

export type PortalSettingsPatch = Partial<
    Pick<DeveloperPortal, 'name' | 'description' | 'portalUrl' | 'documentationViewer'>
>;

export async function updatePortalSettings(id: string, patch: PortalSettingsPatch): Promise<DeveloperPortal | undefined> {
    const existing = await getPortal(id);
    if (!existing) {
        return undefined;
    }

    const updated: DeveloperPortal = {
        ...existing,
        ...patch,
        updatedAt: new Date().toISOString(),
    };
    await savePortal(updated);
    return normalizePortal(updated);
}

export async function deletePortal(id: string): Promise<void> {
    await runTransaction(PORTALS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deletePortalWithRelatedData(id: string): Promise<void> {
    const tenants = await getTenantsByPortalId(id);
    await Promise.all(tenants.map(tenant => deleteMembersForTenant(tenant.id)));
    await deleteTenantsForPortal(id);
    await deleteCategoriesForPortal(id);
    await deleteSubscriptionFormsForPortal(id);
    await deleteIdentityProvidersForPortal(id);
    await deleteNavItemsForPortal(id);
    await deletePageContentsForPortal(id);
    await deletePortal(id);
}

export async function seedPortalsIfEmpty(): Promise<DeveloperPortal[]> {
    await seedCatalogDataIfEmpty();

    const existing = await getAllPortals();
    if (existing.length > 0) {
        return existing;
    }

    const dummyPortals = createDummyPortals();
    await Promise.all(dummyPortals.map(portal => savePortal(portal)));

    await seedPortalFromTemplate('portal-payments', 'payments');
    await seedPortalFromTemplate('portal-active-fitness', 'active-fitness');
    await Promise.all(dummyPortals.map(portal => seedPortalTenantsForPortal(portal.id)));

    return dummyPortals;
}
