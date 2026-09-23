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

export const PAGE_TYPES = {
    ASCIIDOC: 'ASCIIDOC',
    ASYNCAPI: 'ASYNCAPI',
    MARKDOWN: 'MARKDOWN',
    MARKDOWN_TEMPLATE: 'MARKDOWN_TEMPLATE',
    SWAGGER: 'SWAGGER',
    FOLDER: 'FOLDER',
    LINK: 'LINK',
    ROOT: 'ROOT',
    SYSTEM_FOLDER: 'SYSTEM_FOLDER',
    TRANSLATION: 'TRANSLATION',
} as const;

export type PageType = (typeof PAGE_TYPES)[keyof typeof PAGE_TYPES];

/** Page types the Documentation UI can create and edit (Next Gen Portal set). */
export const SUPPORTED_FOR_EDIT = ['MARKDOWN', 'SWAGGER', 'ASYNCAPI'] as const;
export type SupportedEditPageType = (typeof SUPPORTED_FOR_EDIT)[number];

export type Visibility = 'PUBLIC' | 'PRIVATE';

export type PageSourceType = 'FILL' | 'IMPORT' | 'EXTERNAL';

export interface AccessControl {
    referenceId?: string;
    referenceType?: string;
}

export interface PageSource {
    type?: string;
    configuration?: Record<string, unknown>;
}

export interface DocumentationPage {
    id?: string;
    crossId?: string;
    name?: string;
    type?: PageType;
    content?: string;
    order?: number;
    lastContributor?: string;
    published?: boolean;
    visibility?: Visibility;
    updatedAt?: string;
    contentType?: string;
    source?: PageSource;
    configuration?: Record<string, string>;
    homepage?: boolean;
    parentId?: string;
    parentPath?: string;
    hidden?: boolean;
    generalConditions?: boolean;
    accessControls?: AccessControl[];
    excludedAccessControls?: boolean;
    portalNavId?: string;
    portalPageContentId?: string;
}

export interface Breadcrumb {
    id: string;
    name: string;
    position: number;
}

export interface DocumentationPagesResult {
    pages: DocumentationPage[];
    breadcrumb?: Breadcrumb[];
}

export interface CreateDocumentationPayload {
    name: string;
    type: PageType;
    visibility?: Visibility;
    parentId?: string;
    content?: string;
    homepage?: boolean;
    source?: PageSource;
    configuration?: Record<string, unknown>;
    accessControls?: AccessControl[];
    excludedAccessControls?: boolean;
}

export type EditDocumentationPayload = Partial<CreateDocumentationPayload> & {
    name?: string;
    type?: PageType;
    order?: number;
    published?: boolean;
};

export interface FetcherListItem {
    id: string;
    name?: string;
    description?: string;
    version?: string;
    schema: string;
}

export const SPEC_GEN_STATES = {
    AVAILABLE: 'AVAILABLE',
    UNAVAILABLE: 'UNAVAILABLE',
    STARTED: 'STARTED',
    GENERATING: 'GENERATING',
} as const;

export type SpecGenState = (typeof SPEC_GEN_STATES)[keyof typeof SPEC_GEN_STATES];

export interface SpecGenRequestState {
    state: SpecGenState;
}

export interface OpenApiConfiguration {
    viewer: string;
    entrypointAsBasePath: boolean;
    entrypointsAsServers: boolean;
    tryItURL: string;
    tryIt: boolean;
    disableSyntaxHighlight: boolean;
    tryItAnonymous: boolean;
    showURL: boolean;
    displayOperationId: boolean;
    usePkce: boolean;
    docExpansion: string;
    enableFiltering: boolean;
    showExtensions: boolean;
    showCommonExtensions: boolean;
    maxDisplayedTags: number;
}

export const DEFAULT_OPENAPI_CONFIGURATION: OpenApiConfiguration = {
    viewer: 'Swagger',
    entrypointAsBasePath: false,
    entrypointsAsServers: false,
    tryItURL: '',
    tryIt: false,
    disableSyntaxHighlight: false,
    tryItAnonymous: false,
    showURL: false,
    displayOperationId: false,
    usePkce: false,
    docExpansion: 'none',
    enableFiltering: false,
    showExtensions: false,
    showCommonExtensions: false,
    maxDisplayedTags: -1,
};

export interface PortalNavigationItem {
    id: string;
    title?: string;
    type?: string;
    parentId?: string | null;
    area?: string;
    apiId?: string;
    portalPageContentId?: string;
    published?: boolean;
    order?: number;
    visibility?: Visibility;
    contentType?: string;
    categoryIds?: string[];
    children?: PortalNavigationItem[];
}

export interface PortalNavigationItemsResponse {
    items?: PortalNavigationItem[];
}

export interface PortalFolderOption {
    id: string;
    path: string;
    area: string;
}

export interface ApiPortalPlacement {
    folderId: string;
    folderPath: string;
    itemId: string;
    area: string;
    /** True when the API navigation node itself is published in the portal. */
    published: boolean;
}
