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

export interface Audit {
    id: string;
    createdAt: string;
    user: { id: string; displayName: string };
    event: string;
    properties: Array<{ key: string; value: string; name: string }>;
    patch: string;
}

export interface AuditPage {
    data: Audit[];
    pagination: {
        page: number;
        perPage: number;
        pageCount: number;
        pageItemsCount: number;
        totalCount: number;
    };
}

export interface AuditSearchParams {
    page: number;
    perPage: number;
    events?: string;
    from?: number;
    to?: number;
}
