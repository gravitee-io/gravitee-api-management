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
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import type { ChipOption } from '../chip-option';
import { authzApiService } from '../api/authz-api.service';
import type { EntityResponse } from '../api/authz-api.types';
import { parseEntityUid } from '../entity-adapter';
import { authzQueryKeys } from '../api/query-keys';

export interface UseEntityOptionsResult {
    readonly options: readonly ChipOption[];
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export interface UseEntityOptionsOpts {
    /** Optional restrict to specific entity types (e.g. `['User', 'Group']`). */
    readonly typeFilter?: readonly string[];
}

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
    // Stable string key for the typeFilter: callers can pass a fresh array
    // literal on every render without causing a new network fetch.
    const typeFilterKey = useMemo(() => (typeFilter ? JSON.stringify([...typeFilter].sort()) : ''), [typeFilter]);

    const query = useQuery({
        // typeFilterKey in the query key: different filter values → different
        // cache entry + re-fetch. Same values (fresh array reference) → same
        // key → no re-fetch.
        queryKey: [...authzQueryKeys.entityOptions(environmentId), typeFilterKey],
        queryFn: () => authzApiService.listEntities(environmentId, { page: 1, perPage: PER_PAGE }),
        staleTime: 30_000,
    });

    const allOptions = useMemo(() => query.data?.data.map(toChipOption) ?? [], [query.data]);

    const tooMany = (query.data?.total ?? 0) > PER_PAGE;

    const options = useMemo(() => {
        if (!typeFilter || typeFilter.length === 0) return allOptions;
        const allow = new Set(typeFilter);
        return allOptions.filter(o => allow.has(o.group));
    }, [allOptions, typeFilter]);

    const networkError =
        query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined;
    const error = networkError ?? (tooMany ? 'Too many entities for chip picker; consider refining schema or filtering by type.' : undefined);

    return { options, isLoading: query.isLoading, error };
}
