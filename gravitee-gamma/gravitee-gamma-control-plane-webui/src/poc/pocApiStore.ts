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

export const POC_API_STORAGE_KEY = 'gravitee-poc-apis';

export const DEMO_API_ID = 'demo-api';

export type PocApiDetail = Record<string, unknown> & {
    id: string;
    name: string;
    apiVersion?: string;
    description?: string;
    type?: string;
    definitionVersion?: string;
    state?: string;
    deploymentState?: string;
    lifecycleState?: string;
    visibility?: string;
    listeners?: Array<Record<string, unknown>>;
    endpointGroups?: Array<Record<string, unknown>>;
    primaryOwner?: { displayName: string; email: string };
    createdAt?: string;
    updatedAt?: string;
};

export type PocPlan = Record<string, unknown> & {
    id: string;
    name?: string;
    status?: string;
    security?: { type?: string; configuration?: unknown };
    definitionVersion?: string;
    mode?: string;
};

export interface PocApiStore {
    apis: Record<string, PocApiDetail>;
    plans: Record<string, PocPlan[]>;
}

const DEMO_API: PocApiDetail = {
    id: DEMO_API_ID,
    name: 'Sports Management API',
    apiVersion: 'v1',
    description: 'Demo proxy API for the offline POC',
    type: 'PROXY',
    definitionVersion: 'V4',
    state: 'STARTED',
    deploymentState: 'DEPLOYED',
    lifecycleState: 'PUBLISHED',
    visibility: 'PRIVATE',
    listeners: [{ type: 'HTTP', paths: [{ path: '/sports/', host: '' }] }],
    labels: ['demo'],
    categories: ['Sports'],
    allowedInApiProducts: false,
    primaryOwner: { displayName: 'POC Demo User', email: 'demo@gravitee.io' },
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
};

function emptyStore(): PocApiStore {
    return {
        apis: { [DEMO_API_ID]: { ...DEMO_API } },
        plans: { [DEMO_API_ID]: [] },
    };
}

function isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

export function loadPocApiStore(): PocApiStore {
    if (!isBrowser()) {
        return emptyStore();
    }
    try {
        const raw = window.localStorage.getItem(POC_API_STORAGE_KEY);
        if (!raw) {
            const seeded = emptyStore();
            savePocApiStore(seeded);
            return seeded;
        }
        const parsed = JSON.parse(raw) as Partial<PocApiStore>;
        const apis = parsed.apis && typeof parsed.apis === 'object' ? parsed.apis : {};
        const plans = parsed.plans && typeof parsed.plans === 'object' ? parsed.plans : {};
        if (!apis[DEMO_API_ID]) {
            apis[DEMO_API_ID] = { ...DEMO_API };
        }
        if (!plans[DEMO_API_ID]) {
            plans[DEMO_API_ID] = [];
        }
        return { apis, plans };
    } catch {
        const seeded = emptyStore();
        savePocApiStore(seeded);
        return seeded;
    }
}

export function savePocApiStore(store: PocApiStore): void {
    if (!isBrowser()) {
        return;
    }
    window.localStorage.setItem(POC_API_STORAGE_KEY, JSON.stringify(store));
}

export function listApis(): PocApiDetail[] {
    return Object.values(loadPocApiStore().apis);
}

export function getApi(apiId: string): PocApiDetail | undefined {
    return loadPocApiStore().apis[apiId];
}

export function normalizeContextPath(path: string | undefined): string {
    if (!path) {
        return '/';
    }
    let normalized = path.trim();
    if (!normalized.startsWith('/')) {
        normalized = `/${normalized}`;
    }
    if (normalized.length > 1 && normalized.endsWith('/')) {
        normalized = normalized.slice(0, -1);
    }
    return normalized || '/';
}

export function collectApiPaths(api: PocApiDetail): string[] {
    const paths: string[] = [];
    for (const listener of api.listeners ?? []) {
        const pathEntries = listener.paths as Array<{ path?: string }> | undefined;
        if (Array.isArray(pathEntries)) {
            for (const entry of pathEntries) {
                if (entry?.path) {
                    paths.push(normalizeContextPath(entry.path));
                }
            }
        }
        const hosts = listener.hosts as Array<{ path?: string }> | undefined;
        if (Array.isArray(hosts)) {
            for (const host of hosts) {
                if (host?.path) {
                    paths.push(normalizeContextPath(host.path));
                }
            }
        }
    }
    return paths;
}

export function isPathTaken(path: string, excludeApiId?: string): boolean {
    const normalized = normalizeContextPath(path);
    for (const api of listApis()) {
        if (excludeApiId && api.id === excludeApiId) {
            continue;
        }
        if (collectApiPaths(api).includes(normalized)) {
            return true;
        }
    }
    return false;
}

export function createApiFromRequest(body: Record<string, unknown>): PocApiDetail {
    const store = loadPocApiStore();
    const id = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `poc-api-${Date.now()}`;
    const now = new Date().toISOString();
    const api: PocApiDetail = {
        ...body,
        id,
        name: String(body.name ?? 'Untitled API'),
        apiVersion: String(body.apiVersion ?? '1.0'),
        description: body.description != null ? String(body.description) : '',
        type: 'PROXY',
        definitionVersion: 'V4',
        state: 'STOPPED',
        deploymentState: 'NEED_REDEPLOY',
        lifecycleState: 'CREATED',
        visibility: String(body.visibility ?? 'PRIVATE'),
        listeners: Array.isArray(body.listeners) ? (body.listeners as Array<Record<string, unknown>>) : [],
        endpointGroups: Array.isArray(body.endpointGroups) ? (body.endpointGroups as Array<Record<string, unknown>>) : [],
        allowedInApiProducts: Boolean(body.allowedInApiProducts),
        primaryOwner: { displayName: 'POC Demo User', email: 'demo@gravitee.io' },
        createdAt: now,
        updatedAt: now,
    };
    store.apis[id] = api;
    store.plans[id] = [];
    savePocApiStore(store);
    return api;
}

export function updateApi(apiId: string, patch: Partial<PocApiDetail>): PocApiDetail | undefined {
    const store = loadPocApiStore();
    const existing = store.apis[apiId];
    if (!existing) {
        return undefined;
    }
    const updated: PocApiDetail = {
        ...existing,
        ...patch,
        id: apiId,
        updatedAt: new Date().toISOString(),
    };
    store.apis[apiId] = updated;
    savePocApiStore(store);
    return updated;
}

export function createPlan(apiId: string, body: Record<string, unknown>): PocPlan | undefined {
    const store = loadPocApiStore();
    if (!store.apis[apiId]) {
        return undefined;
    }
    const id = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `poc-plan-${Date.now()}`;
    const plan: PocPlan = {
        ...body,
        id,
        name: String(body.name ?? 'Default plan'),
        status: 'STAGING',
        definitionVersion: String(body.definitionVersion ?? 'V4'),
        mode: String(body.mode ?? 'STANDARD'),
        security: (body.security as PocPlan['security']) ?? { type: 'KEY_LESS' },
    };
    const plans = store.plans[apiId] ?? [];
    plans.push(plan);
    store.plans[apiId] = plans;
    savePocApiStore(store);
    return plan;
}

export function publishPlan(apiId: string, planId: string): PocPlan | undefined {
    const store = loadPocApiStore();
    const plans = store.plans[apiId];
    if (!plans) {
        return undefined;
    }
    const plan = plans.find(p => p.id === planId);
    if (!plan) {
        return undefined;
    }
    plan.status = 'PUBLISHED';
    savePocApiStore(store);
    return plan;
}

export function startApi(apiId: string): PocApiDetail | undefined {
    return updateApi(apiId, { state: 'STARTED', deploymentState: 'DEPLOYED' });
}

export function toListItem(api: PocApiDetail): Record<string, unknown> {
    return {
        id: api.id,
        name: api.name,
        apiVersion: api.apiVersion,
        description: api.description,
        type: api.type,
        definitionVersion: api.definitionVersion,
        state: api.state,
        deploymentState: api.deploymentState,
        lifecycleState: api.lifecycleState,
        visibility: api.visibility,
        listeners: api.listeners,
        primaryOwner: api.primaryOwner,
    };
}
