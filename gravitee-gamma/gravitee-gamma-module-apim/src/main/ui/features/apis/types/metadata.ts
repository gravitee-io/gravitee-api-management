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

export type MetadataFormat = 'STRING' | 'NUMERIC' | 'BOOLEAN' | 'DATE' | 'MAIL' | 'URL';

export type MetadataSource = 'GLOBAL' | 'API';

export interface ApiMetadata {
    key: string;
    name: string;
    format: MetadataFormat;
    value?: string;
    defaultValue?: string;
}

export interface NewApiMetadataPayload {
    name: string;
    format: MetadataFormat;
    value: string;
}

export interface UpdateApiMetadataPayload {
    key: string;
    name: string;
    format: MetadataFormat;
    value: string;
    defaultValue?: string;
}

export interface SearchApiMetadataParams {
    page?: number;
    perPage?: number;
    source?: MetadataSource;
    sortBy?: string;
}

export interface ApiMetadataListResponse {
    data: ApiMetadata[];
    pagination?: {
        page?: number;
        perPage?: number;
        pageCount?: number;
        pageItemsCount?: number;
        totalCount?: number;
    };
}
