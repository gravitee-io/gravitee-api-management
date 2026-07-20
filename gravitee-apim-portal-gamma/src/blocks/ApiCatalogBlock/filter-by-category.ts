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
import type { PortalCategory } from '../../features/settings/types';

/**
 * Filter published API entries by an admin-configured category mapping.
 * When no category is selected, returns all entries unchanged.
 * When a category id is selected but not found / disabled, returns an empty list.
 */
export function filterEntriesByCategory<T extends { apiId: string }>(
    entries: readonly T[],
    categories: readonly PortalCategory[],
    selectedCategoryId: string,
): T[] {
    const trimmed = selectedCategoryId.trim();
    if (!trimmed) {
        return [...entries];
    }

    const category = categories.find(item => item.id === trimmed && item.enabled);
    if (!category) {
        return [];
    }

    const mappedIds = new Set(category.mappedApis.map(api => api.id));
    return entries.filter(entry => mappedIds.has(entry.apiId));
}

/** Enabled categories suitable for the consumer catalog filter dropdown. */
export function getEnabledCategoriesForFilter(categories: readonly PortalCategory[]): PortalCategory[] {
    return categories.filter(category => category.enabled);
}

/** Category names assigned to an API via admin mapping (enabled categories only). */
export function getCategoryNamesForApi(
    apiId: string,
    categories: readonly PortalCategory[],
): string[] {
    return categories
        .filter(category => category.enabled && category.mappedApis.some(api => api.id === apiId))
        .map(category => category.name);
}
