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
import type { DeveloperPortal } from '../types';
import { DEFAULT_PORTAL_LABEL } from '../types';
import { createDummyNavigation, createDummyPageContents } from './dummy-navigation';
import { createDummyPortals } from './dummy-portals';
import { DB_NAME, PORTALS_STORE_NAME, runTransaction } from './db';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';

export { DB_NAME } from './db';
export const DB_VERSION = 2;
export const STORE_NAME = PORTALS_STORE_NAME;

function normalizePortal(portal: DeveloperPortal): DeveloperPortal {
    return {
        ...portal,
        portalLabel: portal.portalLabel ?? DEFAULT_PORTAL_LABEL,
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
    await runTransaction(PORTALS_STORE_NAME, 'readwrite', store => store.put(portal));
}

export async function deletePortal(id: string): Promise<void> {
    await runTransaction(PORTALS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function seedPortalsIfEmpty(): Promise<DeveloperPortal[]> {
    const existing = await getAllPortals();
    if (existing.length > 0) {
        return existing;
    }

    const dummyPortals = createDummyPortals();
    await Promise.all(dummyPortals.map(portal => savePortal(portal)));

    const demoPortalId = 'portal-payments';
    const navItems = createDummyNavigation(demoPortalId);
    const pageContents = createDummyPageContents(demoPortalId, navItems);

    await Promise.all(navItems.map(item => saveNavItem(item)));
    await Promise.all(pageContents.map(content => savePageContent(content)));

    return dummyPortals;
}
