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

import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { AuditGroupedRefs, AuditMetadataPage, AuditNamedRef, AuditSearchParams } from '../types/auditLog';

export const AUDIT_EXPORT_MAX_ROWS = 10_000;
export const AUDIT_EXPORT_PAGE_SIZE = 1_000;
const API_LIST_PAGE_SIZE = 200;

export class AuditExportLimitError extends Error {
    constructor(readonly totalElements: number) {
        super(`Too many audit logs to export (${totalElements}). Narrow the date range or filters.`);
        this.name = 'AuditExportLimitError';
    }
}

export function buildAuditQuery(params: AuditSearchParams): string {
    const search = new URLSearchParams();
    search.set('page', String(params.page));
    search.set('size', String(params.size));
    if (params.event) {
        search.set('event', params.event);
    }
    if (params.type) {
        search.set('type', params.type);
    }
    if (params.environment && params.type === 'ENVIRONMENT') {
        search.set('environment', params.environment);
    }
    if (params.application && params.type === 'APPLICATION') {
        search.set('application', params.application);
    }
    if (params.api && params.type === 'API') {
        search.set('api', params.api);
    }
    if (params.from !== undefined) {
        search.set('from', String(params.from));
    }
    if (params.to !== undefined) {
        search.set('to', String(params.to));
    }
    return search.toString();
}

export async function searchOrgAudits(params: AuditSearchParams): Promise<AuditMetadataPage> {
    return apimFetchJsonOrg<AuditMetadataPage>(`/audit?${buildAuditQuery(params)}`);
}

export async function searchEnvAudits(environmentId: string, params: AuditSearchParams): Promise<AuditMetadataPage> {
    return apimFetchJsonV1Env<AuditMetadataPage>(environmentId, `/audit?${buildAuditQuery(params)}`);
}

export async function listOrgAuditEvents(): Promise<string[]> {
    return apimFetchJsonOrg<string[]>('/audit/events');
}

export async function listEnvAuditEvents(environmentId: string): Promise<string[]> {
    return apimFetchJsonV1Env<string[]>(environmentId, '/audit/events');
}

export async function listAuditEnvironments(): Promise<AuditNamedRef[]> {
    const environments = await apimFetchJsonOrg<Array<{ id: string; name?: string }>>('/environments');
    return (environments ?? []).map(environment => ({
        id: environment.id,
        name: environment.name || environment.id,
    }));
}

export async function listAuditApplications(environmentId: string): Promise<AuditNamedRef[]> {
    const applications = await apimFetchJsonV1Env<Array<{ id: string; name?: string }>>(environmentId, '/applications?status=active');
    return (applications ?? []).map(application => ({
        id: application.id,
        name: application.name || application.id,
    }));
}

interface ApisPage {
    data?: Array<{ id: string; name?: string }>;
    pagination?: { page?: number; pageCount?: number };
}

export async function listAuditApis(environmentId: string): Promise<AuditNamedRef[]> {
    const collected: AuditNamedRef[] = [];
    let page = 1;
    let pageCount = 1;
    do {
        const response = await apimFetchJsonV2<ApisPage>(environmentId, `/apis?page=${page}&perPage=${API_LIST_PAGE_SIZE}`);
        collected.push(
            ...(response.data ?? []).map(api => ({
                id: api.id,
                name: api.name || api.id,
            })),
        );
        pageCount = response.pagination?.pageCount ?? 1;
        page += 1;
    } while (page <= pageCount);
    return collected;
}

/**
 * Fans out `listItems` across every environment and groups the results by environment name.
 * Callers pass the environment list in so the `/environments` response is fetched and cached once.
 */
async function groupByEnvironment(
    environments: readonly AuditNamedRef[],
    listItems: (environmentId: string) => Promise<AuditNamedRef[]>,
): Promise<AuditGroupedRefs[]> {
    const groups = await Promise.all(
        environments.map(async environment => ({
            group: environment.name,
            items: await listItems(environment.id),
        })),
    );
    return groups.filter(group => group.items.length > 0);
}

export async function listOrgAuditApplicationsByEnvironment(environments: readonly AuditNamedRef[]): Promise<AuditGroupedRefs[]> {
    return groupByEnvironment(environments, listAuditApplications);
}

export async function listOrgAuditApisByEnvironment(environments: readonly AuditNamedRef[]): Promise<AuditGroupedRefs[]> {
    return groupByEnvironment(environments, listAuditApis);
}

async function collectAuditPages(fetchPage: (page: number, size: number) => Promise<AuditMetadataPage>): Promise<AuditMetadataPage> {
    const first = await fetchPage(1, AUDIT_EXPORT_PAGE_SIZE);
    if (first.totalElements > AUDIT_EXPORT_MAX_ROWS) {
        throw new AuditExportLimitError(first.totalElements);
    }
    const content = [...(first.content ?? [])];
    const metadata = { ...(first.metadata ?? {}) };
    const lastPage = Math.max(1, Math.ceil(first.totalElements / AUDIT_EXPORT_PAGE_SIZE));
    for (let page = 2; page <= lastPage; page += 1) {
        const next = await fetchPage(page, AUDIT_EXPORT_PAGE_SIZE);
        content.push(...(next.content ?? []));
        Object.assign(metadata, next.metadata ?? {});
    }
    return {
        content,
        metadata,
        pageNumber: 1,
        pageElements: content.length,
        totalElements: first.totalElements,
    };
}

export async function exportOrgAudits(params: Omit<AuditSearchParams, 'page' | 'size'>): Promise<AuditMetadataPage> {
    return collectAuditPages((page, size) => searchOrgAudits({ ...params, page, size }));
}

export async function exportEnvAudits(environmentId: string, params: Omit<AuditSearchParams, 'page' | 'size'>): Promise<AuditMetadataPage> {
    return collectAuditPages((page, size) => searchEnvAudits(environmentId, { ...params, page, size }));
}
