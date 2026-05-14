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

/** Matches Management API grant type objects (see applications/types.json). */
export interface ApplicationGrantType {
    type: string;
    name: string;
    response_types?: string[];
}

/** Enabled type from GET .../configuration/applications/types. */
export interface ApplicationTypeConfig {
    id: string;
    name: string;
    description?: string;
    default_grant_types: ApplicationGrantType[];
    requires_redirect_uris: boolean;
    allowed_grant_types: ApplicationGrantType[];
    mandatory_grant_types: ApplicationGrantType[];
}

export interface ApplicationTypesResponse {
    data: ApplicationTypeConfig[];
}

export interface ApplicationGroup {
    id: string;
    name: string;
}

export interface ApplicationTlsSettings {
    client_certificate?: string;
}

export interface ApplicationOAuthSettings {
    application_type: string;
    grant_types: string[];
    redirect_uris: string[];
    additional_client_metadata?: Record<string, string>;
}

export interface ApplicationSimpleSettings {
    type?: string;
    client_id?: string;
}

export interface ApplicationCreateSettings {
    app?: ApplicationSimpleSettings;
    oauth?: ApplicationOAuthSettings;
    tls?: ApplicationTlsSettings;
}

export interface CreateApplicationRequest {
    name: string;
    description: string;
    domain?: string;
    groups?: string[];
    settings?: ApplicationCreateSettings;
}

export interface CreatedApplication {
    id: string;
    name: string;
}

export interface RegisterApplicationDraft {
    name: string;
    description: string;
    domain: string;
    typeId: string;
    groups: string[];
    appType: string;
    appClientId: string;
    grantTypes: string[];
    redirectUris: string;
    clientCertificate: string;
    additionalClientMetadata: Record<string, string> | null;
}
