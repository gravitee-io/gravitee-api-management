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
import { useEffect, useRef, useState } from 'react';

import { useEnvironmentStore } from '../../features/environment/environment.store';
import { gammaApi, managementApi, managementV2EnvironmentApi } from '../../shared/api/api-client';

interface CountHookOptions {
    /** When `false`, the hook does not fetch and settles with a `null` value. Use this to gate the
     *  call on module availability — avoids 403/404 noise on license-restricted
     *  deployments. Defaults to `true`. */
    readonly enabled?: boolean;
}

/**
 * Result of a card count hook.
 *
 * `loading` distinguishes "still fetching" from "settled" so the card can tell a genuine loading
 * state apart from a settled-without-value one (e.g. the endpoint returned an error on a
 * license-restricted deployment). A settled `null` means "no value to show" — the card falls back
 * to its description rather than showing a perpetual skeleton.
 */
export interface CountResult {
    readonly value: number | null;
    readonly loading: boolean;
}

/**
 * Shared engine for the card counts: manages the value/loading state and runs the provided
 * fetcher when enabled. The fetcher is read through a ref so the effect only re-runs on
 * `enabled`/`environmentId` changes, not on every render.
 */
function useCount(enabled: boolean, fetcher: (environmentId: string) => Promise<number | null>): CountResult {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [result, setResult] = useState<CountResult>({ value: null, loading: true });
    const fetcherRef = useRef(fetcher);
    fetcherRef.current = fetcher;

    useEffect(() => {
        if (!enabled || !environmentId) {
            setResult({ value: null, loading: false });
            return;
        }

        // Reset on every (re-)run so a stale value from a previous env can't flash through.
        setResult({ value: null, loading: true });

        let cancelled = false;
        fetcherRef
            .current(environmentId)
            .then(value => {
                if (!cancelled) setResult({ value, loading: false });
            })
            .catch(() => {
                // Settle without a value — the card shows its description instead of a spinner.
                if (!cancelled) setResult({ value: null, loading: false });
            });
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return result;
}

/**
 * The gamma APIM module only manages V4 HTTP proxy APIs, so the card count must match what the
 * module's own API list shows. Mirrors the module's server-side `apiTypes` filter
 * (`gravitee-gamma-module-apim` → `features/apis/services/apiList.ts`).
 */
const V4_HTTP_PROXY_API_TYPES = ['V4_HTTP_PROXY'];

/**
 * Live count for the API Management card.
 *
 * Hits the APIM v2 search endpoint with `perPage=1` — we only need the
 * `pagination.totalCount`. Routed through `managementV2EnvironmentApi` whose base URL is
 * `${managementBaseURL}/v2/environments`, so we only pass the envId-scoped suffix.
 *
 * Scoped to V4 HTTP proxy APIs so the count matches the APIM module's list rather than every
 * API type in the environment.
 */
export function useApiCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        managementV2EnvironmentApi
            .post<{ pagination?: { totalCount?: number } }>(`/${encodeURIComponent(environmentId)}/apis/_search?page=1&perPage=1`, {
                apiTypes: [...V4_HTTP_PROXY_API_TYPES],
            })
            .then(res => res?.pagination?.totalCount ?? null),
    );
}

/**
 * Live count for the Agent Management card.
 *
 * Hits the AIM catalog endpoint with `perPage=1` — we only need the `totalCount` from the
 * paginated response. This goes through the gamma API directly, no localStorage or
 * domain-id dependency.
 */
export function useAgentCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        gammaApi
            .get<{
                pagination?: { totalCount?: number };
            }>(`/environments/${encodeURIComponent(environmentId)}/modules/aim/catalog/agents?page=1&perPage=1`)
            .then(res => res?.pagination?.totalCount ?? null),
    );
}

/** Active application count for the Platform card. */
export function useActiveAppCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        managementApi
            .get<{
                page?: { total_elements?: number };
            }>(`/environments/${encodeURIComponent(environmentId)}/applications/_paged?status=ACTIVE&size=1`)
            .then(res => res?.page?.total_elements ?? null),
    );
}

/** Deployed policy count for the Authorization card. */
export function useDeployedPolicyCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        gammaApi
            .get<{ total?: number }>(`/environments/${encodeURIComponent(environmentId)}/modules/authz/policies?perPage=1&status=DEPLOYED`)
            .then(res => res?.total ?? null),
    );
}

/** Principal (user/group) count for the Authorization card. */
export function usePrincipalCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        gammaApi
            .get<{ total?: number }>(`/environments/${encodeURIComponent(environmentId)}/modules/authz/entities?perPage=1&kind=PRINCIPAL`)
            .then(res => res?.total ?? null),
    );
}

/** MCP server count for the Agent Management card. */
export function useMcpServerCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId =>
        gammaApi
            .get<{
                total?: number;
            }>(`/environments/${encodeURIComponent(environmentId)}/modules/aim/catalog/items?kind=mcp-server&perPage=1`)
            .then(res => res?.total ?? null),
    );
}

const EDGE_DEVICE_WINDOW_MS = 24 * 60 * 60 * 1000;

/**
 * Live device count for the Edge Management card.
 *
 * TEMPORARY — see https://gravitee.atlassian.net/browse/GMA-753. The edge module exposes no
 * dedicated device-count endpoint, so — unlike the other cards which read a server-side
 * `pagination.totalCount` from a cheap `perPage=1` call — we derive the count from the v2
 * analytics facets endpoint, mirroring the edge module's own `DevicesPage`: count the distinct
 * `EDGE_CLIENT` heartbeat buckets over a fixed 24h window.
 *
 * Not ideal: the value reflects devices seen in the last 24h (not a registered-device total)
 * and is capped at `limit: 100` buckets. Replace with a dedicated endpoint once the edge module
 * exposes one.
 */
export function useDeviceCount({ enabled = true }: CountHookOptions = {}): CountResult {
    return useCount(enabled, environmentId => {
        const now = Date.now();
        const from = now - EDGE_DEVICE_WINDOW_MS;
        return managementV2EnvironmentApi
            .post<{ metrics?: { name: string; buckets?: unknown[] }[] }>(`/${encodeURIComponent(environmentId)}/analytics/facets`, {
                timeRange: { from: new Date(from).toISOString(), to: new Date(now).toISOString() },
                filters: [{ name: 'EDGE_TYPE', operator: 'EQ', value: 'heartbeat' }],
                metrics: [{ name: 'EDGE_HEARTBEAT_COUNT', measures: ['COUNT'] }],
                by: ['EDGE_CLIENT'],
                limit: 100,
            })
            .then(res => res?.metrics?.find(m => m.name === 'EDGE_HEARTBEAT_COUNT')?.buckets?.length ?? null);
    });
}
