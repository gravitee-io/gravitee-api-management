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
import { useEffect, useState } from 'react';

import { useEnvironmentStore } from '../../features/environment/environment.store';
import { gammaApi, managementApi, managementV2EnvironmentApi } from '../../shared/api/api-client';

interface CountHookOptions {
    /** When `false`, the hook does not fetch and returns `null`. Use this to gate the
     *  call on module availability — avoids 403/404 noise on license-restricted
     *  deployments. Defaults to `true`. */
    readonly enabled?: boolean;
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
export function useApiCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        // Reset on every (re-)run so a stale value from a previous env/disabled state
        // can't flash through while the new request is in flight.
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        managementV2EnvironmentApi
            .post<{ pagination?: { totalCount?: number } }>(`/${encodeURIComponent(environmentId)}/apis/_search?page=1&perPage=1`, {
                apiTypes: [...V4_HTTP_PROXY_API_TYPES],
            })
            .then(res => {
                if (!cancelled) setCount(res?.pagination?.totalCount ?? null);
            })
            .catch(() => {
                /* silent — badge just won't render */
            });
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}

/**
 * Live count for the Agent Management card.
 *
 * Hits the AIM catalog endpoint with `perPage=1` — we only need the `total` from the
 * paginated response. This goes through the gamma API directly, no localStorage or
 * domain-id dependency.
 */
export function useAgentCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        gammaApi
            .get<{ pagination?: { totalCount?: number } }>(
                `/environments/${encodeURIComponent(environmentId)}/modules/aim/catalog/agents?page=1&perPage=1`,
            )
            .then(res => {
                if (!cancelled) setCount(res?.pagination?.totalCount ?? null);
            })
            .catch(() => {
                /* silent — metric just won't render */
            });
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}

/** Active application count for the Platform card. */
export function useActiveAppCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        managementApi
            .get<{ page?: { total_elements?: number } }>(
                `/environments/${encodeURIComponent(environmentId)}/applications/_paged?status=ACTIVE&size=1`,
            )
            .then(res => {
                if (!cancelled) setCount(res?.page?.total_elements ?? null);
            })
            .catch(() => {});
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}

/** Deployed policy count for the Authorization card. */
export function useDeployedPolicyCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        gammaApi
            .get<{ total?: number }>(`/environments/${encodeURIComponent(environmentId)}/modules/authz/policies?perPage=1&status=DEPLOYED`)
            .then(res => {
                if (!cancelled) setCount(res?.total ?? null);
            })
            .catch(() => {});
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}

/** Principal (user/group) count for the Authorization card. */
export function usePrincipalCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        gammaApi
            .get<{ total?: number }>(`/environments/${encodeURIComponent(environmentId)}/modules/authz/entities?perPage=1&kind=PRINCIPAL`)
            .then(res => {
                if (!cancelled) setCount(res?.total ?? null);
            })
            .catch(() => {});
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}

/** MCP server count for the Agent Management card. */
export function useMcpServerCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        let cancelled = false;
        gammaApi
            .get<{ total?: number }>(
                `/environments/${encodeURIComponent(environmentId)}/modules/aim/catalog/items?kind=mcp-server&perPage=1`,
            )
            .then(res => {
                if (!cancelled) setCount(res?.total ?? null);
            })
            .catch(() => {});
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
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
export function useDeviceCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        setCount(null);
        if (!enabled || !environmentId) return;

        const now = Date.now();
        const from = now - EDGE_DEVICE_WINDOW_MS;

        let cancelled = false;
        managementV2EnvironmentApi
            .post<{ metrics?: { name: string; buckets?: unknown[] }[] }>(`/${encodeURIComponent(environmentId)}/analytics/facets`, {
                timeRange: { from: new Date(from).toISOString(), to: new Date(now).toISOString() },
                filters: [{ name: 'EDGE_TYPE', operator: 'EQ', value: 'heartbeat' }],
                metrics: [{ name: 'EDGE_HEARTBEAT_COUNT', measures: ['COUNT'] }],
                by: ['EDGE_CLIENT'],
                limit: 100,
            })
            .then(res => {
                if (cancelled) return;
                const buckets = res?.metrics?.find(m => m.name === 'EDGE_HEARTBEAT_COUNT')?.buckets ?? [];
                setCount(buckets.length);
            })
            .catch(() => {
                /* silent — metric just won't render */
            });
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return count;
}
