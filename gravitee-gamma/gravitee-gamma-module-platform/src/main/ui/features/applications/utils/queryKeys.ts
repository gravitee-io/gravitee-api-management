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

export const applicationDetailKeys = {
    all: ['application-detail'] as const,
    detail: (envId: string, applicationId: string) => [...applicationDetailKeys.all, envId, applicationId] as const,
    typeConfig: (envId: string, applicationId: string) => [...applicationDetailKeys.all, 'type-config', envId, applicationId] as const,
    certificates: (envId: string, applicationId: string) => [...applicationDetailKeys.all, 'certificates', envId, applicationId] as const,
} as const;

export const applicationPermissionKeys = {
    all: ['application-permissions'] as const,
    detail: (envId: string, applicationId: string) => [...applicationPermissionKeys.all, envId, applicationId] as const,
} as const;

export const currentUserKeys = {
    all: ['current-user'] as const,
    detail: () => [...currentUserKeys.all, 'detail'] as const,
} as const;

export const applicationListKeys = {
    all: ['application-list'] as const,
    search: (envId: string, query: string, status: string, page: number, perPage: number) =>
        [...applicationListKeys.all, 'search', envId, query, status, page, perPage] as const,
    count: (envId: string, filter: object) => [...applicationListKeys.all, 'count', envId, filter] as const,
    types: (envId: string) => [...applicationListKeys.all, 'types', envId] as const,
    groups: (envId: string) => [...applicationListKeys.all, 'groups', envId] as const,
} as const;
