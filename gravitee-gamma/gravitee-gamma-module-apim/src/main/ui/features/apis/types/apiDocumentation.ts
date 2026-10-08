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

export type PortalArea = 'HOMEPAGE' | 'TOP_NAVBAR';

export type PortalVisibility = 'PUBLIC' | 'PRIVATE';

export type PortalPageContentType = 'GRAVITEE_MARKDOWN' | 'OPENAPI' | 'ASYNCAPI';

export interface PortalNavigationItemSource {
    type: string;
    configuration: Record<string, unknown>;
    useAutoFetch?: boolean;
    fetchCron?: string;
    readonly lastFetchedAt?: string;
    readonly lastFetchAttemptAt?: string;
    readonly lastFetchError?: string;
    readonly subtreeImport?: boolean;
}

interface BasePortalNavigationItem {
    id: string;
    organizationId: string;
    environmentId: string;
    title: string;
    area: PortalArea;
    parentId?: string;
    rootId: string;
    order: number;
    published: boolean;
    visibility: PortalVisibility;
}

export interface PortalNavigationPage extends BasePortalNavigationItem {
    type: 'PAGE';
    portalPageContentId: string;
    source?: PortalNavigationItemSource;
}

export interface PortalNavigationFolder extends BasePortalNavigationItem {
    type: 'FOLDER';
    source?: PortalNavigationItemSource;
}

export interface PortalNavigationLink extends BasePortalNavigationItem {
    type: 'LINK';
    url: string;
}

/** An item of an API's documentation. The API-scoped endpoints never return API or API product items. */
export type ApiDocumentationItem = PortalNavigationPage | PortalNavigationFolder | PortalNavigationLink;

/** The navigation item listing an API in a portal. */
export interface PortalNavigationApi extends BasePortalNavigationItem {
    type: 'API';
    apiId: string;
    categoryIds?: string[];
}

export interface ApiPortalPublication {
    portalId: string;
    portalNavigationItem: PortalNavigationApi;
    /** Title of the section holding the listing, whose id is the listing's `parentId`. */
    sectionName: string;
}

export interface ApiPortalNavigationItemsResponse {
    /** Items carry their stored parent: a top-level item has no `parentId`, even when the API is listed. */
    items: ApiDocumentationItem[];
    /** Empty when the API is not listed. An API is published only when a listing exists and is published. */
    publications: ApiPortalPublication[];
}

export interface ApiPortalPublishLocation {
    id: string;
    name: string;
}

export interface ApiPortalPublishLocationsResponse {
    data: ApiPortalPublishLocation[];
}

export interface PublishApiToPortal {
    sectionId: string;
}

interface BaseCreateApiDocumentationItem {
    title: string;
    area: PortalArea;
    visibility: PortalVisibility;
    parentId?: string;
    order?: number;
}

export interface CreateApiDocumentationPage extends BaseCreateApiDocumentationItem {
    type: 'PAGE';
    contentType?: PortalPageContentType;
    source?: PortalNavigationItemSource;
}

export interface CreateApiDocumentationFolder extends BaseCreateApiDocumentationItem {
    type: 'FOLDER';
    source?: PortalNavigationItemSource;
}

export interface CreateApiDocumentationLink extends BaseCreateApiDocumentationItem {
    type: 'LINK';
    url: string;
}

export type CreateApiDocumentationItem = CreateApiDocumentationPage | CreateApiDocumentationFolder | CreateApiDocumentationLink;

interface BaseUpdateApiDocumentationItem {
    title: string;
    order: number;
    published: boolean;
    visibility: PortalVisibility;
    parentId?: string;
}

export interface UpdateApiDocumentationPage extends BaseUpdateApiDocumentationItem {
    type: 'PAGE';
    source?: PortalNavigationItemSource;
}

export interface UpdateApiDocumentationFolder extends BaseUpdateApiDocumentationItem {
    type: 'FOLDER';
    source?: PortalNavigationItemSource;
}

export interface UpdateApiDocumentationLink extends BaseUpdateApiDocumentationItem {
    type: 'LINK';
    url: string;
}

/** Replaces the item: a field left out is cleared, an external source included. */
export type UpdateApiDocumentationItem = UpdateApiDocumentationPage | UpdateApiDocumentationFolder | UpdateApiDocumentationLink;

export interface ImportPortalNavigationRequest {
    title: string;
    parentId?: string;
    visibility?: PortalVisibility;
    source: PortalNavigationItemSource;
}

export interface PortalNavigationItemFetchResult {
    navigationItemId?: string;
    title: string;
    success: boolean;
    error?: string;
}

export interface PortalNavigationItemsFetchSummary {
    succeeded: number;
    failed: number;
    results: PortalNavigationItemFetchResult[];
}

export interface ImportPortalNavigationResponse {
    rootFolder: PortalNavigationFolder;
    summary: PortalNavigationItemsFetchSummary;
}

export interface PortalPageContent {
    id: string;
    type: PortalPageContentType;
    content: string;
}

export interface UpdatePortalPageContent {
    content: string;
}
