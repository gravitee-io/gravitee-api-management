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

import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiDocumentationItem,
    ApiPortalNavigationItemsResponse,
    ApiPortalPublication,
    ApiPortalPublishLocationsResponse,
    CreateApiDocumentationItem,
    FetchPortalNavigationItemResponse,
    ImportPortalNavigationRequest,
    ImportPortalNavigationResponse,
    PortalNavigationPage,
    PortalPageContent,
    PortalPageOpenApiConfiguration,
    PublishApiToPortal,
    UpdateApiDocumentationItem,
    UpdatePortalPageContent,
} from '../types/apiDocumentation';

const documentationPath = (apiId: string) => `/apis/${encodeURIComponent(apiId)}/portal-navigation-items`;
const itemPath = (apiId: string, navId: string) => `${documentationPath(apiId)}/${encodeURIComponent(navId)}`;

export async function listApiDocumentation(environmentId: string, apiId: string): Promise<ApiPortalNavigationItemsResponse> {
    return apimFetchJsonV2<ApiPortalNavigationItemsResponse>(environmentId, documentationPath(apiId));
}

export async function createApiDocumentationItem(
    environmentId: string,
    apiId: string,
    payload: CreateApiDocumentationItem,
): Promise<ApiDocumentationItem> {
    return apimFetchJsonV2<ApiDocumentationItem>(environmentId, documentationPath(apiId), {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updateApiDocumentationItem(
    environmentId: string,
    apiId: string,
    navId: string,
    payload: UpdateApiDocumentationItem,
    options: { propagatePublishToChildren?: boolean } = {},
): Promise<ApiDocumentationItem> {
    const query = options.propagatePublishToChildren ? '?propagatePublishToChildren=true' : '';
    return apimFetchJsonV2<ApiDocumentationItem>(environmentId, `${itemPath(apiId, navId)}${query}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

export async function deleteApiDocumentationItem(environmentId: string, apiId: string, navId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, itemPath(apiId, navId), { method: 'DELETE' });
}

export async function fetchApiDocumentationItem(
    environmentId: string,
    apiId: string,
    navId: string,
): Promise<FetchPortalNavigationItemResponse> {
    return apimFetchJsonV2<FetchPortalNavigationItemResponse>(environmentId, `${itemPath(apiId, navId)}/_fetch`, { method: 'POST' });
}

export async function importApiDocumentation(
    environmentId: string,
    apiId: string,
    request: ImportPortalNavigationRequest,
): Promise<ImportPortalNavigationResponse> {
    return apimFetchJsonV2<ImportPortalNavigationResponse>(environmentId, `${documentationPath(apiId)}/_import`, {
        method: 'POST',
        body: JSON.stringify(request),
    });
}

export async function listApiPublishLocations(environmentId: string, apiId: string): Promise<ApiPortalPublishLocationsResponse> {
    return apimFetchJsonV2<ApiPortalPublishLocationsResponse>(environmentId, `${documentationPath(apiId)}/_publish-locations`);
}

export async function publishApiToPortal(environmentId: string, apiId: string, request: PublishApiToPortal): Promise<ApiPortalPublication> {
    return apimFetchJsonV2<ApiPortalPublication>(environmentId, `${documentationPath(apiId)}/_publish`, {
        method: 'POST',
        body: JSON.stringify(request),
    });
}

/** Creates the API's Overview page from the API's details, unpublished. */
export async function createApiOverviewPage(environmentId: string, apiId: string): Promise<PortalNavigationPage> {
    return apimFetchJsonV2<PortalNavigationPage>(environmentId, `${documentationPath(apiId)}/_overview-page`, { method: 'POST' });
}

export async function unpublishApiFromPortal(environmentId: string, apiId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `${documentationPath(apiId)}/_unpublish`, { method: 'POST' });
}

export async function getApiDocumentationPageContent(environmentId: string, apiId: string, navId: string): Promise<PortalPageContent> {
    return apimFetchJsonV2<PortalPageContent>(environmentId, `${itemPath(apiId, navId)}/content`);
}

export async function saveApiDocumentationPageContent(
    environmentId: string,
    apiId: string,
    navId: string,
    payload: UpdatePortalPageContent,
): Promise<PortalPageContent> {
    return apimFetchJsonV2<PortalPageContent>(environmentId, `${itemPath(apiId, navId)}/content`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

/** Saves how the portal renders an OpenAPI page, leaving its content as stored. */
export async function saveApiDocumentationPageConfiguration(
    environmentId: string,
    apiId: string,
    navId: string,
    configuration: PortalPageOpenApiConfiguration,
): Promise<PortalPageContent> {
    return apimFetchJsonV2<PortalPageContent>(environmentId, `${itemPath(apiId, navId)}/content/configuration`, {
        method: 'PATCH',
        body: JSON.stringify(configuration),
    });
}
