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
import { PORTAL_CATEGORIES_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { MappedApi, PortalCategory } from '../types';

function createCategoryId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `category-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeCategory(category: PortalCategory): PortalCategory {
    return {
        ...category,
        description: category.description ?? '',
        enabled: category.enabled ?? true,
        mappedApis: category.mappedApis ?? [],
    };
}

export async function getCategoriesByPortalId(portalId: string): Promise<PortalCategory[]> {
    const categories = await runTransaction<PortalCategory[]>(
        PORTAL_CATEGORIES_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('portalId');
            return index.getAll(portalId);
        },
    );

    return categories.map(normalizeCategory).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getPortalCategory(id: string): Promise<PortalCategory | undefined> {
    const category = await runTransaction<PortalCategory | undefined>(
        PORTAL_CATEGORIES_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    return category ? normalizeCategory(category) : undefined;
}

export async function savePortalCategory(category: PortalCategory): Promise<void> {
    await runTransaction(PORTAL_CATEGORIES_STORE_NAME, 'readwrite', store =>
        store.put(normalizeCategory(category)),
    );
}

export async function createPortalCategory(
    portalId: string,
    input: { name: string; description?: string },
): Promise<PortalCategory> {
    const category: PortalCategory = {
        id: createCategoryId(),
        portalId,
        name: input.name.trim(),
        description: (input.description ?? '').trim(),
        createdAt: Date.now(),
        enabled: true,
        mappedApis: [],
    };
    await savePortalCategory(category);
    return category;
}

export async function setCategoryEnabled(categoryId: string, enabled: boolean): Promise<PortalCategory | undefined> {
    const existing = await getPortalCategory(categoryId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeCategory({ ...existing, enabled });
    await savePortalCategory(updated);
    return updated;
}

export async function setCategoryMappedApis(
    categoryId: string,
    mappedApis: readonly MappedApi[],
): Promise<PortalCategory | undefined> {
    const existing = await getPortalCategory(categoryId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeCategory({ ...existing, mappedApis: [...mappedApis] });
    await savePortalCategory(updated);
    return updated;
}

export async function deletePortalCategory(id: string): Promise<void> {
    await runTransaction(PORTAL_CATEGORIES_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteCategoriesForPortal(portalId: string): Promise<void> {
    const categories = await getCategoriesByPortalId(portalId);
    await Promise.all(categories.map(category => deletePortalCategory(category.id)));
}
