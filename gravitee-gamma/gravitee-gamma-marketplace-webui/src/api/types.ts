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

export interface User {
    id?: string;
    first_name?: string;
    last_name?: string;
    display_name?: string;
    email?: string;
}

export type IdentityProviderType = 'GOOGLE' | 'GITHUB' | 'GRAVITEEIO_AM' | 'OIDC';

export interface IdentityProvider {
    id: string;
    name: string;
    description?: string;
    client_id?: string;
    email_required?: boolean;
    type?: IdentityProviderType;
    authorizationEndpoint?: string;
    tokenIntrospectionEndpoint?: string;
    userLogoutEndpoint?: string;
    color?: string;
    display?: string;
    requiredUrlParams?: string[];
    optionalUrlParams?: string[];
    scopes?: string[];
}

export interface Enabled {
    enabled?: boolean;
}

export interface PortalConfiguration {
    authentication?: {
        forceLogin?: Enabled;
        localLogin?: Enabled;
    };
}

export interface IdentityProvidersResponse {
    data?: IdentityProvider[];
}

export interface Category {
    id: string;
    name?: string;
    description?: string;
    order?: number;
    total_apis?: number;
}

export interface CategoriesResponse {
    data?: Category[];
}

export type ApiType = 'A2A_PROXY' | 'AUTHZ' | 'EDGE' | 'LLM_PROXY' | 'MCP_PROXY' | 'PROXY' | 'MESSAGE' | 'NATIVE';

export interface McpToolDefinition {
    name?: string;
    description?: string;
}

export interface Mcp {
    mcpPath?: string;
    tools?: Array<{
        toolDefinition?: McpToolDefinition;
    }>;
}

export type PageType = 'ASCIIDOC' | 'ASYNCAPI' | 'SWAGGER' | 'MARKDOWN' | 'FOLDER' | 'ROOT' | 'LINK';

export interface Page {
    id: string;
    name: string;
    type: PageType;
    order: number;
    parent?: string | null;
    content?: string;
}

export interface PagesResponse {
    data?: Page[];
}

export interface Api {
    id: string;
    name: string;
    version: string;
    description: string;
    owner: User;
    type?: ApiType;
    labels?: string[];
    categories?: string[];
    mcp?: Mcp;
    entrypoints?: string[];
}

export interface PaginationMetadata {
    current_page?: number;
    first?: number;
    last?: number;
    size?: number;
    total?: number;
    total_pages?: number;
}

export interface ApisResponse {
    data?: Api[];
    metadata?: {
        pagination?: PaginationMetadata;
    };
}
