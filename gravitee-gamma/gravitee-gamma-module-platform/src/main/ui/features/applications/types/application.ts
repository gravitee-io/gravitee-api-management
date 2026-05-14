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

export type ApplicationStatus = 'ACTIVE' | 'ARCHIVED';

export interface ApplicationOwner {
    id: string;
    displayName: string;
    type: string;
}

export interface ApplicationSettings {
    app?: {
        type?: string;
        client_id?: string;
    };
}

export interface ApplicationListItem {
    id: string;
    name: string;
    description?: string;
    status: ApplicationStatus;
    type?: string;
    created_at: number;
    updated_at: number;
    owner?: ApplicationOwner;
    settings?: ApplicationSettings;
    picture_url?: string;
    origin?: string;
}

export interface ApplicationPagedMeta {
    current: number;
    size: number;
    per_page: number;
    total_pages: number;
    total_elements: number;
}

export interface ApplicationListResponse {
    data: ApplicationListItem[];
    page: ApplicationPagedMeta;
}
