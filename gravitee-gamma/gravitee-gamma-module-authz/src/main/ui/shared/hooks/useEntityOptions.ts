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
import { useQueries, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { authzApiService } from '../api/authz-api.service';
import type { EntityResponse, PagedResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';
import type { ChipOption } from '../chip-option';
import { parseEntityUid } from '../entity-adapter';
import { uiTypeToKind } from '../entity-kind-registry';

export interface UseEntityOptionsResult {
    readonly options: readonly ChipOption[];
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export interface UseEntityOptionsOpts {
    readonly typeFilter?: readonly string[];
}

// Per-kind page size. With typeFilter the picker fans out into N parallel
// kind-scoped queries (each capped here), so the effective ceiling is
// PER_PAGE × N kinds rather than PER_PAGE total. Without typeFilter we fall
// back to a single all-kinds query bounded by the same number, leaving the
// "consider filtering by type" hint in place for envs that exceed it.
const PER_PAGE = 200;

function summarizeAttributes(attrs: Record<string, unknown>): string | undefined {
    const entries = Object.entries(attrs).filter(([k]) => !k.startsWith('_'));
    if (entries.length === 0) return undefined;
    const parts = entries.slice(0, 2).map(([k, v]) => {
        const rendered = typeof v === 'string' ? v : JSON.stringify(v);
        return `${k}=${rendered}`;
    });
    return parts.join(', ');
}

function toChipOption(entity: EntityResponse): ChipOption {
    const { type, id } = parseEntityUid(entity.uid);
    return {
        id: `${type}::"${id}"`,
        label: id,
        group: type,
        description: summarizeAttributes(entity.attributes),
    };
}

export function useEntityOptions(environmentId: string, opts?: UseEntityOptionsOpts): UseEntityOptionsResult {
    const typeFilter = opts?.typeFilter;

    // Stable, content-addressed list of canonical kinds. With typeFilter present
    // each entry becomes a kind-scoped fetch via entityIdPrefix; without it we
    // issue a single non-scoped fetch (kinds = []).
    const kindsKey = useMemo(() => (typeFilter ? JSON.stringify([...typeFilter].sort()) : ''), [typeFilter]);
    const kinds = useMemo<readonly string[]>(() => {
        if (!kindsKey) return [];
        return (JSON.parse(kindsKey) as readonly string[]).map(uiTypeToKind);
    }, [kindsKey]);

    const scopedResults = useQueries({
        queries: kinds.map(kind => ({
            queryKey: [...authzQueryKeys.entityOptions(environmentId), kind],
            queryFn: () => authzApiService.listEntities(environmentId, { page: 1, perPage: PER_PAGE, entityIdPrefix: `${kind}.` }),
            enabled: Boolean(environmentId),
            staleTime: 30_000,
        })),
    });

    // Only used when typeFilter is absent — falls back to the single all-kinds query.
    const allKindsQuery = useQuery({
        queryKey: authzQueryKeys.entityOptions(environmentId),
        queryFn: () => authzApiService.listEntities(environmentId, { page: 1, perPage: PER_PAGE }),
        enabled: Boolean(environmentId) && kinds.length === 0,
        staleTime: 30_000,
    });

    const pages = useMemo<ReadonlyArray<PagedResponse<EntityResponse> | undefined>>(
        () => (kinds.length === 0 ? [allKindsQuery.data] : scopedResults.map(r => r.data)),
        [kinds.length, allKindsQuery.data, scopedResults],
    );

    const isLoading = kinds.length === 0 ? allKindsQuery.isLoading : scopedResults.some(r => r.isLoading);
    const firstError = kinds.length === 0 ? allKindsQuery.error : scopedResults.find(r => r.error)?.error;
    const networkError = firstError instanceof Error ? firstError.message : firstError ? String(firstError) : undefined;

    const options = useMemo<readonly ChipOption[]>(() => {
        const merged: ChipOption[] = [];
        for (const page of pages) {
            if (!page) continue;
            for (const e of page.data) merged.push(toChipOption(e));
        }
        return merged;
    }, [pages]);

    const tooMany = pages.some(p => (p?.total ?? 0) > PER_PAGE);
    const error = networkError ?? (tooMany ? 'Too many entities to load; narrow the schema or filter by type.' : undefined);

    return { options, isLoading, error };
}
