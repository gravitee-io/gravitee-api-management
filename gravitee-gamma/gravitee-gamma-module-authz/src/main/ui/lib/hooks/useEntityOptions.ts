import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChipOption } from '../../app/features/policy-management/PolicyStatementCard';
import { authzApiService } from '../api/authz-api.service';
import type { EntityResponse } from '../api/authz-api.types';
import { parseEntityUid } from '../entity-adapter';

export interface UseEntityOptionsResult {
    readonly options: readonly ChipOption[];
    readonly isLoading: boolean;
    readonly error: string | undefined;
}

export interface UseEntityOptionsOpts {
    /** Optional restrict to specific entity types (e.g. `['User', 'Group']`). */
    readonly typeFilter?: readonly string[];
}

/**
 * Maximum number of entities fetched in a single page. The chip picker is not
 * a paginated browser — once we exceed this we surface an error so the user
 * knows the picker may be incomplete. The list endpoint is paginated; for now
 * we deliberately fetch a single (large) page rather than aggregating to keep
 * the hook simple and fast.
 */
const PER_PAGE = 200;

/**
 * Build a short, human-readable summary of an entity's attributes for the
 * chip option description. Returns up to two `key=value` pairs (skipping
 * meta keys prefixed with `_`) so the dropdown stays scannable.
 */
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
        // Use the raw entity UID as the ChipOption id so it matches what
        // `parseGaplToStatements` produces when hydrating the visual editor
        // from stored GAPL — both sides must agree on the same canonical id
        // (`User::"alice"`) for chip selection to round-trip on edit.
        id: entity.uid,
        label: id,
        group: type,
        description: summarizeAttributes(entity.attributes),
    };
}

/**
 * Selector hook that turns the live list of entities into chip options for
 * the principal combobox in the policy builder.
 *
 * Behaviour:
 * - Calls `listEntities(environmentId, { page: 1, perPage: 200 })` once per
 *   `(environmentId, typeFilter)` pair.
 * - If `typeFilter` is given, drops options whose entity type is not in the
 *   filter. The filter is compared by stable string key, so callers may pass
 *   a fresh array literal on each render.
 * - If the backend reports `total > 200`, returns the loaded slice plus an
 *   `error` string telling the user to refine the schema or filter by type.
 * - Cancellation-safe: aborts in-flight on unmount or when the deps change.
 */
export function useEntityOptions(environmentId: string, opts?: UseEntityOptionsOpts): UseEntityOptionsResult {
    const typeFilter = opts?.typeFilter;
    // Stable string key for the typeFilter dependency: callers can pass a
    // fresh array literal (`['User', 'Group']`) without retriggering the
    // effect on every render.
    const typeFilterKey = useMemo(() => (typeFilter ? JSON.stringify([...typeFilter].sort()) : ''), [typeFilter]);

    const [allOptions, setAllOptions] = useState<readonly ChipOption[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | undefined>(undefined);

    const mountedRef = useRef(true);
    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
        };
    }, []);

    useEffect(() => {
        let cancelled = false;
        // Gate setState to break cascading renders when nothing actually
        // changes (isLoading already true / error already undefined).
        if (!isLoading) setIsLoading(true);
        if (error !== undefined) setError(undefined);

        authzApiService
            .listEntities(environmentId, { page: 1, perPage: PER_PAGE })
            .then(res => {
                if (cancelled) return;
                const mapped = res.data.map(toChipOption);
                setAllOptions(mapped);
                if (res.total > PER_PAGE) {
                    setError('Too many entities for chip picker; consider refining schema or filtering by type.');
                } else {
                    setError(undefined);
                }
            })
            .catch(e => {
                if (cancelled) return;
                setAllOptions([]);
                setError(e instanceof Error ? e.message : 'Failed to load entities');
            })
            .finally(() => {
                if (!cancelled) setIsLoading(false);
            });

        return () => {
            cancelled = true;
        };
        // typeFilterKey is intentionally a dependency so changing the filter
        // refetches. isLoading/error read for gating setState.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [environmentId, typeFilterKey]);

    const options = useMemo(() => {
        if (!typeFilter || typeFilter.length === 0) return allOptions;
        const allow = new Set(typeFilter);
        return allOptions.filter(o => allow.has(o.group));
    }, [allOptions, typeFilter, typeFilterKey]); // eslint-disable-line react-hooks/exhaustive-deps

    return { options, isLoading, error };
}
