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
import { buildObservabilityBaseUrl } from '@gravitee/gamma-lib-observability';

import { ApimApiError, graviteeConsoleAuthHeaders, loadApimBootstrap, persistCsrfFromResponse } from '../../../shared/api/apimClient';

export type RuntimeOverlayStatus = 'ACTIVE' | 'STOPPED' | 'EXPIRED';

export interface RuntimeOverlay {
    id: string;
    kind: string;
    apiId: string;
    startAt: number;
    endAt: number;
    status: RuntimeOverlayStatus;
    payload: Record<string, unknown>;
}

export interface StartRuntimeOverlayRequest {
    apiId: string;
    kind: string;
    durationSeconds: number;
    payload: Record<string, unknown>;
}

const useDevProxy = process.env.NODE_ENV !== 'production';

async function runtimeOverlaysUrl(environmentId: string): Promise<string> {
    const { gammaBaseURL, organizationId } = await loadApimBootstrap();
    return `${buildObservabilityBaseUrl(gammaBaseURL, organizationId, environmentId, { useDevProxy })}/runtime-overlays`;
}

async function send<T>(url: string, init?: RequestInit): Promise<T | undefined> {
    const headers = new Headers(init?.headers);
    Object.entries(graviteeConsoleAuthHeaders()).forEach(([name, value]) => headers.set(name, value));
    if (init?.body !== undefined && init.body !== null) headers.set('Content-Type', 'application/json');

    const response = await fetch(url, { ...init, headers, credentials: 'include' });
    persistCsrfFromResponse(response);
    if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new ApimApiError(response.status, text || `${url} → ${response.status}`);
    }
    if (response.status === 204) return undefined;
    const text = await response.text();
    return text ? (JSON.parse(text) as T) : undefined;
}

export async function listRuntimeOverlays(environmentId: string): Promise<RuntimeOverlay[]> {
    return (await send<RuntimeOverlay[]>(await runtimeOverlaysUrl(environmentId))) ?? [];
}

export async function startRuntimeOverlay(environmentId: string, request: StartRuntimeOverlayRequest): Promise<void> {
    await send(await runtimeOverlaysUrl(environmentId), { method: 'POST', body: JSON.stringify(request) });
}

export async function stopRuntimeOverlay(environmentId: string, overlay: Pick<RuntimeOverlay, 'id' | 'apiId'>): Promise<void> {
    const params = new URLSearchParams({ apiId: overlay.apiId });
    await send(`${await runtimeOverlaysUrl(environmentId)}/${encodeURIComponent(overlay.id)}?${params}`, { method: 'DELETE' });
}

/** The server status is only as fresh as the last poll: an overlay whose end has passed is shown expired right away. */
export function liveStatus(overlay: RuntimeOverlay, now: number): RuntimeOverlayStatus {
    return overlay.status === 'ACTIVE' && now >= overlay.endAt ? 'EXPIRED' : overlay.status;
}
