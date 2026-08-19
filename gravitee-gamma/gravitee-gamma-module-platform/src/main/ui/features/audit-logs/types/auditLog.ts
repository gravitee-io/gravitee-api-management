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

export type AuditReferenceType = 'ORGANIZATION' | 'ENVIRONMENT' | 'APPLICATION' | 'API';

export type AuditScope = 'organization' | 'environment';

export interface AuditEntity {
    id: string;
    referenceId: string;
    // The API tolerates audits with no reference type (AuditServiceImpl skips metadata resolution for them).
    referenceType: string | null;
    user: string;
    createdAt: number | string;
    event: string;
    properties?: Record<string, string>;
    patch?: string;
}

export interface AuditMetadataPage {
    content: AuditEntity[];
    pageNumber: number;
    pageElements: number;
    totalElements: number;
    metadata: Record<string, unknown>;
}

export interface AuditSearchParams {
    page: number;
    size: number;
    event?: string;
    type?: AuditReferenceType;
    environment?: string;
    application?: string;
    api?: string;
    from?: number;
    to?: number;
}

export interface AuditNamedRef {
    id: string;
    name: string;
}

export interface AuditGroupedRefs {
    group: string;
    items: AuditNamedRef[];
}

export interface AuditLogRow {
    id: string;
    createdAt: number;
    user: string;
    referenceType: string;
    reference: string;
    event: string;
    targets: Array<{ key: string; value: string }>;
    patch: string;
}

export type AuditDatePreset = '' | '24h' | '7d' | '30d' | '90d' | 'custom';

export type AuditExportFormat = 'csv' | 'json';
