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

export type ApiProductDeploymentState = 'DEPLOYED' | 'NEED_REDEPLOY';

export interface ApiProductPrimaryOwner {
    id?: string;
    displayName?: string;
    email?: string;
}

export interface ApiProductListItem {
    id: string;
    name: string;
    description?: string;
    version: string;
    apiIds?: string[];
    groups?: string[];
    disableMembershipNotifications?: boolean;
    deploymentState?: ApiProductDeploymentState;
    createdAt?: string;
    updatedAt?: string;
    primaryOwner?: ApiProductPrimaryOwner;
}

export interface ApiProductPagination {
    page: number;
    perPage: number;
    pageCount: number;
    totalCount: number;
    pageItemsCount?: number;
}

export interface ApiProductListResponse {
    data: ApiProductListItem[];
    pagination: ApiProductPagination;
}

export interface CreateApiProductRequest {
    name: string;
    description?: string;
    version: string;
    apiIds?: string[];
}

export interface UpdateApiProductRequest {
    name: string;
    description?: string;
    version: string;
    apiIds?: string[];
    groups?: string[];
    disableMembershipNotifications?: boolean;
}

export interface VerifyApiProductNameRequest {
    name: string;
    apiProductId?: string;
}

export interface VerifyApiProductNameResponse {
    ok: boolean;
    reason?: string;
}

export interface ApiProductSearchQuery {
    query?: string;
    ids?: string[];
}
