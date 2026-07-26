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
import { useCallback, useEffect, useMemo, useState } from 'react';

import { searchAiWorkspaces } from '../../editor/services/ai-workspace.service';
import { searchApiProducts } from '../../editor/services/api-product.service';
import { searchApis } from '../../editor/services/api.service';
import { getAllPortals } from '../../portals/storage/portals.storage';
import type { PortalGrantScopeType } from '../types/permissions.types';

export interface ScopeOption {
    scopeType: PortalGrantScopeType;
    id: string;
    name: string;
    description?: string;
}

const CATALOG_PAGE_SIZE = 100;

/** Loads every grantable scope once: developer portals plus the mocked asset catalogs. */
export function useScopeCatalog() {
    const [options, setOptions] = useState<ScopeOption[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            const [portals, apis, products, workspaces] = await Promise.all([
                getAllPortals(),
                searchApis({ size: CATALOG_PAGE_SIZE }),
                searchApiProducts({ size: CATALOG_PAGE_SIZE }),
                searchAiWorkspaces({ size: CATALOG_PAGE_SIZE }),
            ]);

            setOptions([
                ...portals.map(portal => ({
                    scopeType: 'PORTAL' as const,
                    id: portal.id,
                    name: portal.name,
                    description: portal.portalUrl,
                })),
                ...(apis.data ?? []).map(api => ({
                    scopeType: 'API' as const,
                    id: api.id,
                    name: api.name,
                    description: api.description,
                })),
                ...(products.data ?? []).map(product => ({
                    scopeType: 'API_PRODUCT' as const,
                    id: product.id,
                    name: product.name,
                    description: product.description,
                })),
                ...(workspaces.data ?? []).map(workspace => ({
                    scopeType: 'AI_WORKSPACE' as const,
                    id: workspace.id,
                    name: workspace.name,
                    description: workspace.description,
                })),
            ]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const optionsByKey = useMemo(() => {
        const map = new Map<string, ScopeOption>();
        for (const option of options) {
            map.set(`${option.scopeType}:${option.id}`, option);
        }
        return map;
    }, [options]);

    const optionsByType = useMemo(() => {
        const map = new Map<PortalGrantScopeType, ScopeOption[]>();
        for (const option of options) {
            const existing = map.get(option.scopeType) ?? [];
            existing.push(option);
            map.set(option.scopeType, existing);
        }
        return map;
    }, [options]);

    const labelFor = useCallback(
        (scopeType: PortalGrantScopeType, scopeId: string) =>
            optionsByKey.get(`${scopeType}:${scopeId}`)?.name ?? scopeId,
        [optionsByKey],
    );

    return { options, optionsByType, loading, labelFor, refresh };
}
