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
import { gammaApi, managementV2EnvironmentApi } from '../../shared/api/api-client';

/**
 * localStorage key written by the AIM module's `am-config.ts`.
 *
 * ⚠️ CROSS-REPO COUPLING — keep in sync with
 * `gravitee-gamma-module-aim/src/main/ui/lib/api/am-config.ts` (search `STORAGE_KEY`).
 *
 * If AIM bumps the key (e.g. `v2` → `v3`) or changes the `AimConfig` shape, this read
 * silently returns `null` and the "X agents" badge just stops rendering — no crash,
 * but the badge disappears unnoticed. Reviewers of either repo should check the other
 * side when touching am-config.
 *
 * Long-term fix: AIM ships a host-facing stats endpoint (e.g. `/modules/aim/stats/agents/count`)
 * that does not require the host to know the AM domain — at which point this whole
 * localStorage dance can be deleted.
 */
const AIM_CONFIG_STORAGE_KEY = 'aim.am-config.v2';

interface AimConfig {
    readonly organizationId: string;
    readonly environmentId: string;
    readonly domainId: string;
}

/** Type guard — narrows `unknown` to `AimConfig` only after every required field is
 *  a non-empty string. */
function isAimConfig(value: unknown): value is AimConfig {
    if (typeof value !== 'object' || value === null) return false;
    const v = value as Record<string, unknown>;
    return (
        typeof v.organizationId === 'string' &&
        v.organizationId.length > 0 &&
        typeof v.environmentId === 'string' &&
        v.environmentId.length > 0 &&
        typeof v.domainId === 'string' &&
        v.domainId.length > 0
    );
}

function readAimConfig(): AimConfig | null {
    try {
        const raw = localStorage.getItem(AIM_CONFIG_STORAGE_KEY);
        if (!raw) return null;
        const parsed: unknown = JSON.parse(raw);
        return isAimConfig(parsed) ? parsed : null;
    } catch {
        return null;
    }
}

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
 * Routes through the AIM module's identity surface
 * (`/organizations/{orgId}/modules/aim/identity/environments/{envId}/domains/{domainId}/agents`)
 * which proxies the upstream Gravitee AM. The required `domainId` is held in
 * `localStorage[AIM_CONFIG_STORAGE_KEY]`, written by the AIM module on first config —
 * see the warning on `AIM_CONFIG_STORAGE_KEY` for the cross-repo coupling.
 */
export function useAgentCount({ enabled = true }: CountHookOptions = {}): number | null {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const [count, setCount] = useState<number | null>(null);

    useEffect(() => {
        // Reset on every (re-)run so a stale value can't survive an env switch or the
        // hook becoming disabled.
        setCount(null);
        if (!enabled || !environmentId) return;

        const cfg = readAimConfig();
        // Stale config guard: the AIM module stores its own (orgId, envId, domainId).
        // If the user switches environment in the host shell, that cached config now
        // points at a different env — we'd otherwise display "X agents" for the wrong
        // environment. Skip until AIM is re-opened on the current env and re-writes
        // its config.
        if (!cfg || cfg.environmentId !== environmentId) return;

        let cancelled = false;
        const path =
            `/modules/aim/identity` +
            `/environments/${encodeURIComponent(cfg.environmentId)}` +
            `/domains/${encodeURIComponent(cfg.domainId)}/agents?page=1&perPage=1`;
        gammaApi
            .get<{ total?: number }>(path)
            .then(res => {
                if (!cancelled) setCount(res?.total ?? null);
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
