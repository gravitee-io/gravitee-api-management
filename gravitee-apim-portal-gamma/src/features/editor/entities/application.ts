/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ApplicationOwner } from './member';

export type ApplicationType = 'SIMPLE' | 'SPA' | 'WEB' | 'NATIVE' | 'BACKEND_TO_BACKEND';

export interface GrantTypeConfig {
    type: string;
    name: string;
    response_types: string[];
}

export interface ApplicationTypeConfig {
    id: string;
    name: string;
    description: string;
    applicationType: ApplicationType;
    requires_redirect_uris: boolean;
    allowed_grant_types: GrantTypeConfig[];
    default_grant_types: GrantTypeConfig[];
    mandatory_grant_types: GrantTypeConfig[];
}

export interface ApplicationSettingsOAuth {
    client_id?: string;
    client_secret?: string;
    redirect_uris?: string[];
    grant_types?: string[];
    application_type?: string;
    additional_client_metadata?: Record<string, string>;
}

export interface ApplicationSettingsApp {
    client_id?: string;
    type?: string;
}

export interface ApplicationSettings {
    oauth?: ApplicationSettingsOAuth;
    app?: ApplicationSettingsApp;
}

export interface Application {
    id: string;
    name: string;
    description?: string;
    domain?: string;
    applicationType?: ApplicationType;
    api_key_mode?: 'EXCLUSIVE' | 'SHARED' | 'UNSPECIFIED';
    created_at?: string;
    updated_at?: string;
    owner?: ApplicationOwner;
    portalTenantId?: string;
    settings: ApplicationSettings;
}

export interface ApplicationsResponse {
    data: Application[];
    metadata?: {
        pagination?: {
            current_page: number;
            size: number;
            total: number;
            total_pages: number;
        };
    };
}
