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
import { kindToUiType } from '../entity-kind-registry';

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

// Umbrella group for entities the server already scoped by kind but whose uid
// carries no recognizable `<kind>.<id>` prefix and no `_kind` hint (e.g.
// parity-imported principals stored as bare ids like `alice`). Maps to the
// canonical GAPL entity type so the emitted token (`Principal::"alice"`) stays
// schema-valid.
const KIND_FALLBACK_GROUP: Record<EntityKindFilter, string> = { PRINCIPAL: 'Principal', RESOURCE: 'Resource' };

function resolveKind(typeFilter: readonly string[] | undefined): EntityKindFilter | undefined {
    if (!typeFilter || typeFilter.length === 0) return undefined;
    const allPrincipals = typeFilter.every(t => PRINCIPAL_UI_TYPES.has(t));
    if (allPrincipals) return 'PRINCIPAL';
    // Mixed or pure-resource filters fall back to a no-kind fetch and rely on
    // client-side filtering. (No current callsite mixes principals + resources.)
    return undefined;
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

function toChipOption(entity: EntityResponse, fallbackGroup?: string): ChipOption {
    const { type, id } = parseEntityUid(entity.uid);
    // Bare ids parse to `Unknown`; recover the specific type from the `_kind`
    // hint (as fromBackend does) before dropping to the kind umbrella group.
    const group = type !== 'Unknown' ? type : (kindToUiType(entity.attributes['_kind']) ?? fallbackGroup ?? type);
    return {
        id: `${group}::"${id}"`,
        label: id,
        group,
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

    const fallbackGroup = kind ? KIND_FALLBACK_GROUP[kind] : undefined;

    const allOptions = useMemo(() => query.data?.data.map(e => toChipOption(e, fallbackGroup)) ?? [], [query.data, fallbackGroup]);

    const allowedTypes = useMemo(() => (typeFilterKey ? new Set(typeFilterKey.split('\0')) : null), [typeFilterKey]);

    const options = useMemo<readonly ChipOption[]>(() => {
        if (!allowedTypes) return allOptions;
        return allOptions.filter(o => allowedTypes.has(o.group) || (fallbackGroup !== undefined && o.group === fallbackGroup));
    }, [allOptions, allowedTypes, fallbackGroup]);

    const tooMany = (query.data?.total ?? 0) > (query.data?.data.length ?? 0);
    const networkError = query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined;
    const error =
        networkError ?? (tooMany ? 'Too many entities for chip picker; consider refining schema or filtering by type.' : undefined);

    return { options, isLoading: query.isLoading, error };
}
