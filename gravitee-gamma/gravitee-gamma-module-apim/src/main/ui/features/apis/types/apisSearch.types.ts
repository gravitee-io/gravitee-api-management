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
export type ApisSearchResponse = {
    readonly data: readonly ApiSearchItemResponse[];
    readonly pagination: {
        readonly page: number;
        readonly perPage: number;
        readonly pageCount: number;
        readonly pageItemsCount: number;
        readonly totalCount: number;
    };
};

export type ApiSearchItemResponse = {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    readonly apiVersion?: string;
    readonly state?: 'STARTED' | 'STOPPED' | 'CLOSED' | string;
    readonly type?: string;
    readonly definitionVersion?: string;
    readonly createdAt?: string;
    readonly updatedAt?: string;
    readonly deployedAt?: string;
    readonly deploymentState?: 'DEPLOYED' | 'NEED_REDEPLOY' | string;
    readonly lifecycleState?: 'PUBLISHED' | 'UNPUBLISHED' | 'CREATED' | 'DEPRECATED' | 'ARCHIVED' | string;
    readonly primaryOwner?: { readonly id?: string; readonly displayName?: string; readonly email?: string };
    readonly listeners?: ReadonlyArray<{ readonly type?: string; readonly paths?: ReadonlyArray<{ readonly path?: string }> }>;
};

export type ApiSummary = {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    readonly version?: string;
    readonly state?: string;
    readonly deploymentState?: string;
    readonly lifecycleState?: string;
    readonly primaryOwnerDisplayName?: string;
    readonly contextPath?: string;
    readonly deployedAt?: Date;
    readonly updatedAt?: Date;
};

export type ApisSearchResult = {
    readonly apis: readonly ApiSummary[];
    readonly pagination: ApisSearchResponse['pagination'];
};
