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
import type { ApplicationSubscriptionsFilters } from '../types/applicationSubscription';

function sorted(values: string[] | undefined): string[] {
    return [...(values ?? [])].sort();
}

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

export const applicationSubscriptionKeys = {
    all: ['application-subscriptions'] as const,
    list: (envId: string, applicationId: string, filters: ApplicationSubscriptionsFilters | undefined, page: number, size: number) =>
        [
            ...applicationSubscriptionKeys.all,
            'list',
            envId,
            applicationId,
            filters?.apiKey ?? '',
            sorted(filters?.apis),
            sorted(filters?.status),
            sorted(filters?.securityTypes),
            page,
            size,
        ] as const,
    subscribedApis: (envId: string, applicationId: string) =>
        [...applicationSubscriptionKeys.all, 'subscribed-apis', envId, applicationId] as const,
    referenceSearch: (envId: string, query: string, includeApiProducts: boolean) =>
        [...applicationSubscriptionKeys.all, 'reference-search', envId, query, includeApiProducts] as const,
    subscribablePlans: (envId: string, referenceType: string, referenceId: string, applicationId: string) =>
        [...applicationSubscriptionKeys.all, 'subscribable-plans', envId, referenceType, referenceId, applicationId] as const,
    detail: (envId: string, applicationId: string, subscriptionId: string) =>
        [...applicationSubscriptionKeys.all, 'detail', envId, applicationId, subscriptionId] as const,
    apiKeys: (envId: string, applicationId: string, subscriptionId: string) =>
        [...applicationSubscriptionKeys.all, 'api-keys', envId, applicationId, subscriptionId] as const,
} as const;
