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
import { useCallback, useEffect, useState } from 'react';

import {
    createPortalCategory,
    deletePortalCategory,
    getCategoriesByPortalId,
    setCategoryEnabled,
    setCategoryMappedApis,
} from '../storage/portal-categories.storage';
import type { MappedApi, PortalCategory } from '../types';

export function usePortalCategories(portalId: string | undefined) {
    const [categories, setCategories] = useState<PortalCategory[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!portalId) {
            setCategories([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            setCategories(await getCategoriesByPortalId(portalId));
        } finally {
            setLoading(false);
        }
    }, [portalId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addCategory = useCallback(
        async (input: { name: string; description?: string }) => {
            if (!portalId) {
                return undefined;
            }
            const created = await createPortalCategory(portalId, input);
            await refresh();
            return created;
        },
        [portalId, refresh],
    );

    const removeCategory = useCallback(
        async (categoryId: string) => {
            await deletePortalCategory(categoryId);
            await refresh();
        },
        [refresh],
    );

    const toggleEnabled = useCallback(
        async (categoryId: string, enabled: boolean) => {
            await setCategoryEnabled(categoryId, enabled);
            await refresh();
        },
        [refresh],
    );

    const updateMappedApis = useCallback(
        async (categoryId: string, mappedApis: readonly MappedApi[]) => {
            await setCategoryMappedApis(categoryId, mappedApis);
            await refresh();
        },
        [refresh],
    );

    return {
        categories,
        loading,
        refresh,
        addCategory,
        removeCategory,
        toggleEnabled,
        updateMappedApis,
    };
}
