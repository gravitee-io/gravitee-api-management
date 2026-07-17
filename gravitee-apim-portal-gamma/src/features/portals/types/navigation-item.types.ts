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
import type { AsyncApiSpecSource, OpenApiSpecSource } from './spec-source.types';

export type { AsyncApiSpecSource, OpenApiSpecSource } from './spec-source.types';

export type PortalNavigationItemType = 'PAGE' | 'FOLDER' | 'LINK' | 'API' | 'API_PRODUCT';

export type PortalNavigationArea = 'HEADER' | 'FOOTER' | 'USER_MENU';

export type PageContentType = 'BLOCK' | 'OPENAPI' | 'HTML' | 'ASYNCAPI';

export type OpenApiRenderer = 'swagger' | 'redoc' | 'gravitee';

export interface BaseNavigationItem {
    readonly id: string;
    readonly portalId: string;
    readonly title: string;
    readonly type: PortalNavigationItemType;
    readonly parentId: string | null;
    readonly order: number;
    readonly slug: string;
    readonly area?: PortalNavigationArea;
    readonly published?: boolean;
}

export interface PortalNavigationBlockPage extends BaseNavigationItem {
    readonly type: 'PAGE';
    readonly contentType?: 'BLOCK';
}

export interface PortalNavigationOpenApiPage extends BaseNavigationItem {
    readonly type: 'PAGE';
    readonly contentType: 'OPENAPI';
    readonly renderer: OpenApiRenderer;
    readonly specSource: OpenApiSpecSource;
}

export interface PortalNavigationHtmlPage extends BaseNavigationItem {
    readonly type: 'PAGE';
    readonly contentType: 'HTML';
}

export interface PortalNavigationAsyncApiPage extends BaseNavigationItem {
    readonly type: 'PAGE';
    readonly contentType: 'ASYNCAPI';
    readonly specSource: AsyncApiSpecSource;
}

export type PortalNavigationPage =
    | PortalNavigationBlockPage
    | PortalNavigationOpenApiPage
    | PortalNavigationHtmlPage
    | PortalNavigationAsyncApiPage;

export interface PortalNavigationFolder extends BaseNavigationItem {
    readonly type: 'FOLDER';
}

export interface PortalNavigationLink extends BaseNavigationItem {
    readonly type: 'LINK';
    readonly url: string;
}

export interface PortalNavigationApi extends BaseNavigationItem {
    readonly type: 'API';
    readonly apiId: string;
}

export interface PortalNavigationApiProduct extends BaseNavigationItem {
    readonly type: 'API_PRODUCT';
    readonly apiProductId: string;
}

export type PortalNavigationItem =
    | PortalNavigationPage
    | PortalNavigationFolder
    | PortalNavigationLink
    | PortalNavigationApi
    | PortalNavigationApiProduct;
