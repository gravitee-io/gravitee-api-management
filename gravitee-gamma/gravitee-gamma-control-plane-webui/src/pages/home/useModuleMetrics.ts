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
 * Live count for the API Management card.
 *
 * Hits the APIM v2 search endpoint with `perPage=1` — we only need the
 * `pagination.totalCount`. Routed through `managementV2EnvironmentApi` whose base URL is
 * `${managementBaseURL}/v2/environments`, so we only pass the envId-scoped suffix.
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
            .post<{ pagination?: { totalCount?: number } }>(`/${encodeURIComponent(environmentId)}/apis/_search?page=1&perPage=1`, {})
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

export interface TrafficStats {
    readonly requestsTotal: number;
}

/** 24h traffic stats for the API Management card. */
export function useTrafficStats({ enabled = true }: CountHookOptions = {}): TrafficStats | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [stats, setStats] = useState<TrafficStats | null>(null);

    useEffect(() => {
        setStats(null);
        if (!enabled || !environmentId) return;

        const now = Date.now();
        const from = now - 24 * 60 * 60 * 1000;

        let cancelled = false;
        managementV2EnvironmentApi
            .get<{ requestsTotal?: number }>(`/${encodeURIComponent(environmentId)}/analytics/request-response-time?from=${from}&to=${now}`)
            .then(res => {
                if (!cancelled && res?.requestsTotal !== null && res?.requestsTotal !== undefined) {
                    setStats({ requestsTotal: res.requestsTotal });
                }
            })
            .catch(() => {});
        return () => {
            cancelled = true;
        };
    }, [enabled, environmentId]);

    return stats;
}
