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
import type { JsonSchema } from '@gravitee/graphene-core';

import type { FetcherListItem, OpenApiConfiguration, PageType } from '../types/documentation';
import { DEFAULT_OPENAPI_CONFIGURATION, SUPPORTED_FOR_EDIT } from '../types/documentation';

export function pageTypeTitle(pageType: PageType | undefined): string {
    switch (pageType) {
        case 'ASCIIDOC':
            return 'AsciiDoc';
        case 'ASYNCAPI':
            return 'AsyncAPI';
        case 'SWAGGER':
            return 'OpenAPI';
        case 'MARKDOWN':
            return 'Markdown';
        case 'FOLDER':
            return 'Folder';
        default:
            return pageType ?? 'Page';
    }
}

export function isSupportedEditType(type: string | undefined): type is (typeof SUPPORTED_FOR_EDIT)[number] {
    return type !== undefined && (SUPPORTED_FOR_EDIT as readonly string[]).includes(type);
}

export type PortalPageContentType = 'GRAVITEE_MARKDOWN' | 'OPENAPI' | 'ASYNCAPI';

export function toPortalContentType(type: string | undefined): PortalPageContentType | undefined {
    if (type === 'MARKDOWN') return 'GRAVITEE_MARKDOWN';
    if (type === 'SWAGGER') return 'OPENAPI';
    if (type === 'ASYNCAPI') return 'ASYNCAPI';
    return undefined;
}

export function fromPortalContentType(type: string | undefined): PageType | undefined {
    if (type === 'GRAVITEE_MARKDOWN') return 'MARKDOWN';
    if (type === 'OPENAPI') return 'SWAGGER';
    if (type === 'ASYNCAPI') return 'ASYNCAPI';
    return undefined;
}

export function normalizeParentId(parentId: string | null | undefined): string | null {
    if (!parentId || parentId === 'ROOT') return null;
    return parentId;
}

export function toApiParentId(parentId: string | null | undefined): string {
    return normalizeParentId(parentId) ?? 'ROOT';
}

export function parseConfigurationBoolean(value: string | undefined): boolean {
    if (!value) return false;
    try {
        return JSON.parse(value) === true;
    } catch {
        return value === 'true';
    }
}

export function parseConfigurationNumber(value: string | undefined): number {
    if (!value) return -1;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : -1;
}

export function openApiConfigurationToRecord(config: OpenApiConfiguration): Record<string, unknown> {
    return { ...config };
}

export function openApiConfigurationFromPage(configuration: Record<string, string> | undefined): OpenApiConfiguration {
    return {
        viewer: configuration?.['viewer'] ?? DEFAULT_OPENAPI_CONFIGURATION.viewer,
        entrypointAsBasePath: parseConfigurationBoolean(configuration?.['entrypointAsBasePath']),
        entrypointsAsServers: parseConfigurationBoolean(configuration?.['entrypointsAsServers']),
        tryItURL: configuration?.['tryItURL'] ?? '',
        tryIt: parseConfigurationBoolean(configuration?.['tryIt']),
        disableSyntaxHighlight: parseConfigurationBoolean(configuration?.['disableSyntaxHighlight']),
        tryItAnonymous: parseConfigurationBoolean(configuration?.['tryItAnonymous']),
        showURL: parseConfigurationBoolean(configuration?.['showURL']),
        displayOperationId: parseConfigurationBoolean(configuration?.['displayOperationId']),
        usePkce: parseConfigurationBoolean(configuration?.['usePkce']),
        docExpansion: configuration?.['docExpansion'] ?? DEFAULT_OPENAPI_CONFIGURATION.docExpansion,
        enableFiltering: parseConfigurationBoolean(configuration?.['enableFiltering']),
        showExtensions: parseConfigurationBoolean(configuration?.['showExtensions']),
        showCommonExtensions: parseConfigurationBoolean(configuration?.['showCommonExtensions']),
        maxDisplayedTags: parseConfigurationNumber(configuration?.['maxDisplayedTags']),
    };
}

export function parseFetcherSchema(fetcher: FetcherListItem): JsonSchema | undefined {
    try {
        const parsed: unknown = JSON.parse(fetcher.schema);
        if (typeof parsed === 'object' && parsed !== null) {
            return parsed as JsonSchema;
        }
        return undefined;
    } catch {
        return undefined;
    }
}

export function formatUpdatedAt(value: string | undefined): string {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString();
}

export function acceptedFileTypes(pageType: PageType | undefined): string {
    switch (pageType) {
        case 'MARKDOWN':
            return '.md,text/markdown,text/plain';
        case 'ASCIIDOC':
            return '.adoc,.asciidoc,text/plain';
        case 'SWAGGER':
        case 'ASYNCAPI':
            return '.json,.yaml,.yml,application/json,text/yaml,text/plain';
        default:
            return '*/*';
    }
}
