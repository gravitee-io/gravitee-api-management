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

export const apiProductKeys = {
    all: ['api-products'] as const,
    list: (envId: string, query: string, page: number, perPage: number) =>
        [...apiProductKeys.all, 'list', envId, query, page, perPage] as const,
    count: (envId: string, filter: object) => [...apiProductKeys.all, 'count', envId, JSON.stringify(filter)] as const,
    detail: (envId: string, productId: string) => [...apiProductKeys.all, 'detail', envId, productId] as const,
    verify: (envId: string, name: string, productId?: string) =>
        [...apiProductKeys.all, 'verify-name', envId, name, productId ?? ''] as const,
    apis: (envId: string, productId: string, page: number, perPage: number, query?: string) =>
        [...apiProductKeys.all, 'apis', envId, productId, page, perPage, query ?? ''] as const,
    availableApis: (envId: string, query: string, page: number, perPage: number) =>
        [...apiProductKeys.all, 'available-apis', envId, query, page, perPage] as const,
    members: (envId: string, productId: string) => [...apiProductKeys.all, 'members', envId, productId] as const,
    groupMembers: (envId: string, productId: string, groupId: string) =>
        [...apiProductKeys.all, 'group-members', envId, productId, groupId] as const,
    roles: () => [...apiProductKeys.all, 'roles'] as const,
    permissions: (envId: string, productId: string) => [...apiProductKeys.all, 'permissions', envId, productId] as const,
} as const;
