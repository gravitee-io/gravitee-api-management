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
import { authzApiService, type EntityKindFilter } from '../api/authz-api.service';
import type { EntityResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';
import type { ChipOption } from '../chip-option';
import { parseEntityUid } from '../entity-adapter';

export interface UseEntityOptionsResult {
    readonly options: readonly ChipOption[];
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export interface UseEntityOptionsOpts {
    readonly typeFilter?: readonly string[];
}

const PER_PAGE = 200;

// All-PRINCIPAL UI types known to the registry. When a caller's typeFilter is
// a strict subset of this set we can collapse the fetch to a single
// kind=PRINCIPAL request — the backend already groups user/group/serviceaccount/agent
// under that kind, so we don't need N kind-scoped requests.
const PRINCIPAL_UI_TYPES = new Set(['User', 'Group', 'ServiceAccount', 'AgentIdentity']);

function resolveKind(typeFilter: readonly string[] | undefined): EntityKindFilter | undefined {
    if (!typeFilter || typeFilter.length === 0) return undefined;
    const allPrincipals = typeFilter.every(t => PRINCIPAL_UI_TYPES.has(t));
    if (allPrincipals) return 'PRINCIPAL';
    // Mixed or pure-resource filters fall back to a no-kind fetch and rely on
    // client-side filtering. (No current callsite mixes principals + resources.)
    return undefined;
}

// Synced principals carry an unreadable entity id (the AM token `sub`, a UUID). Prefer a
// human attribute for the chip label, falling back to the id. The id stays visible via the
// description (it leads the attribute summary as `sub=...`) and is unchanged in the chip's
// `id` — the GAPL ref the PEP matches on.
const LABEL_ATTRS = ['displayName', 'email', 'username'] as const;

function readableLabel(attrs: Record<string, unknown>, fallbackId: string): string {
    for (const key of LABEL_ATTRS) {
        const value = attrs[key];
        if (typeof value === 'string' && value.trim().length > 0) return value;
    }
    return fallbackId;
}

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
        label: readableLabel(entity.attributes, id),
        group: type,
        description: summarizeAttributes(entity.attributes),
    };
}

export function useEntityOptions(environmentId: string, opts?: UseEntityOptionsOpts): UseEntityOptionsResult {
    const typeFilter = opts?.typeFilter;

    const kind = useMemo(() => resolveKind(typeFilter), [typeFilter]);

    // Stable join-key so callers can pass fresh arrays without re-running the filter memo.
    const typeFilterKey = useMemo(() => (typeFilter ? [...typeFilter].sort().join('\0') : ''), [typeFilter]);

    const query = useQuery({
        queryKey: [...authzQueryKeys.entityOptions(environmentId), kind ?? 'all'],
        queryFn: () =>
            authzApiService.listEntities(environmentId, kind ? { page: 1, perPage: PER_PAGE, kind } : { page: 1, perPage: PER_PAGE }),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const allOptions = useMemo(() => query.data?.data.map(toChipOption) ?? [], [query.data]);

    const allowedTypes = useMemo(() => (typeFilterKey ? new Set(typeFilterKey.split('\0')) : null), [typeFilterKey]);

    const options = useMemo<readonly ChipOption[]>(() => {
        if (!allowedTypes) return allOptions;
        return allOptions.filter(o => allowedTypes.has(o.group));
    }, [allOptions, allowedTypes]);

    const tooMany = (query.data?.total ?? 0) > (query.data?.data.length ?? 0);
    const networkError = query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined;
    const error =
        networkError ?? (tooMany ? 'Too many entities for chip picker; consider refining schema or filtering by type.' : undefined);

    return { options, isLoading: query.isLoading, error };
}
