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

export interface ApiQualityScore {
    apiId: string;
    name: string;
    description: string;
    definitionVersion: string | null;
    apiType: string | null;
    titleScore: number;
    descriptionScore: number;
    totalScore: number;
    titleIssues: string[];
    descriptionIssues: string[];
}

export interface ApiSuggestion {
    apiId: string;
    suggestedTitle: string;
    suggestedDescription: string;
    reasoning: string;
}

export interface ScoreResult {
    titleScore: number;
    descriptionScore: number;
    totalScore: number;
    titleIssues: string[];
    descriptionIssues: string[];
}

interface Environment {
    id: string;
    hrids?: string[];
    name: string;
}

function getCsrfToken(): string | null {
    return localStorage.getItem('XSRF-TOKEN');
}

function setCsrfToken(value: string) {
    localStorage.setItem('XSRF-TOKEN', value);
}

async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    headers.set('X-Requested-With', 'XMLHttpRequest');
    const csrf = getCsrfToken();
    if (csrf) headers.set('X-Xsrf-Token', csrf);

    const res = await fetch(url, { ...init, headers, credentials: 'include' });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) setCsrfToken(newCsrf);

    if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
    }

    if (res.status === 204) return undefined as T;
    const text = await res.text();
    if (!text) return undefined as T;
    return JSON.parse(text) as T;
}

function getBaseUrls(): { gammaBase: string; managementBase: string; orgId: string } {
    const pathname = window.location.pathname;
    const parts = pathname.split('/').filter(Boolean);

    const envIdx = parts.indexOf('environments');
    const gammaOrigin = window.location.origin;

    let gammaBase = '';
    let managementBase = '';
    let orgId = 'DEFAULT';

    const constantsEl = document.querySelector('meta[name="gamma-base-url"]');
    if (constantsEl) {
        gammaBase = constantsEl.getAttribute('content') || '';
    }

    if (!gammaBase) {
        gammaBase = `${gammaOrigin}/gamma`;
    }

    try {
        const stored = JSON.parse(sessionStorage.getItem('gravitee-bootstrap') || '{}');
        if (stored.gammaBaseURL) gammaBase = stored.gammaBaseURL;
        if (stored.managementBaseURL) managementBase = stored.managementBaseURL;
        if (stored.organizationId) orgId = stored.organizationId;
    } catch {
        // fallback
    }

    if (!managementBase) {
        managementBase = `${gammaOrigin}/management`;
    }

    return { gammaBase, managementBase, orgId };
}

function managementV2Base(): string {
    const { managementBase } = getBaseUrls();
    return `${managementBase.replace(/\/$/, '')}/v2`;
}

export async function resolveEnvironmentId(envHrid: string): Promise<string> {
    const { managementBase, orgId } = getBaseUrls();
    const envs = await apiFetch<Environment[]>(`${managementBase}/organizations/${orgId}/environments`);

    if (!envs?.length) throw new Error('No environments found');

    const lower = envHrid.toLowerCase();
    const match =
        envs.find((e) => e.id.toLowerCase() === lower) ?? envs.find((e) => e.hrids?.some((h) => h.toLowerCase() === lower));

    if (match) return match.id;
    return envs[0]!.id;
}

export async function fetchCatalogQuality(envId: string): Promise<ApiQualityScore[]> {
    const { orgId } = getBaseUrls();
    const base = managementV2Base();
    return apiFetch<ApiQualityScore[]>(
        `${base}/organizations/${orgId}/environments/${encodeURIComponent(envId)}/catalog-quality/apis`,
    );
}

export async function fetchApiSuggestions(apiId: string, envId: string): Promise<ApiSuggestion> {
    const { orgId } = getBaseUrls();
    const base = managementV2Base();
    return apiFetch<ApiSuggestion>(
        `${base}/organizations/${orgId}/environments/${encodeURIComponent(envId)}/catalog-quality/apis/${encodeURIComponent(apiId)}/suggestions`,
        {
            method: 'POST',
        },
    );
}

export async function fetchScore(envId: string, title: string, description: string): Promise<ScoreResult> {
    const { orgId } = getBaseUrls();
    const base = managementV2Base();
    return apiFetch<ScoreResult>(
        `${base}/organizations/${orgId}/environments/${encodeURIComponent(envId)}/catalog-quality/score`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, description }),
        },
    );
}

export async function applyApiSuggestion(apiId: string, envId: string, name: string, description: string): Promise<void> {
    const { managementBase, orgId } = getBaseUrls();
    await apiFetch(`${managementBase}/v2/organizations/${orgId}/environments/${envId}/apis/${apiId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/merge-patch+json' },
        body: JSON.stringify({ name, description }),
    });
}
