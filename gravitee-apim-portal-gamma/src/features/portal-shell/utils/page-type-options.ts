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
import type { OpenApiRenderer, PageContentType } from '../../portals/types';

export interface AddPageOptions {
    readonly contentType: PageContentType;
    readonly renderer?: OpenApiRenderer;
}

export const PAGE_TYPE_LABELS: Record<PageContentType, string> = {
    BLOCK: 'Block',
    OPENAPI: 'OpenAPI',
    HTML: 'HTML',
    ASYNCAPI: 'AsyncAPI',
};

export const PAGE_TYPE_DESCRIPTIONS: Record<PageContentType, string> = {
    BLOCK: 'Rich content built from blocks and layouts',
    OPENAPI: 'API documentation from an OpenAPI specification',
    HTML: 'Custom page authored as raw HTML',
    ASYNCAPI: 'Event-driven API documentation from AsyncAPI',
};

export type PageTypeOption = {
    readonly contentType: PageContentType;
    readonly label: string;
    readonly description: string;
};

export function getPageTypeOptions(
    contentTypes: readonly PageContentType[],
): ReadonlyArray<PageTypeOption> {
    return contentTypes.map(contentType => ({
        contentType,
        label: PAGE_TYPE_LABELS[contentType],
        description: PAGE_TYPE_DESCRIPTIONS[contentType],
    }));
}

export const PAGE_TYPE_OPTIONS = getPageTypeOptions(['BLOCK', 'OPENAPI', 'HTML', 'ASYNCAPI']);

/** Page types available when adding pages to the user menu (no API spec pages). */
export const USER_MENU_PAGE_TYPE_OPTIONS = getPageTypeOptions(['BLOCK', 'HTML']);

export const OPENAPI_RENDERER_LABELS = {
    swagger: 'Swagger UI',
    redoc: 'Redoc',
    gravitee: 'Gravitee Renderer',
} satisfies Record<OpenApiRenderer, string>;

export function normalizeOpenApiRenderer(renderer: string | undefined): OpenApiRenderer {
    if (renderer === 'redoc' || renderer === 'gravitee') {
        return renderer;
    }
    return 'swagger';
}
