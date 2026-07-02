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
import { useQuery } from '@tanstack/react-query';
import { createContext, useContext, useMemo, type ReactNode } from 'react';
import type { OpenAPIV3 } from 'openapi-types';

import { resolveOpenApiSpecContent } from '../../features/editor/utils/resolve-openapi-spec';
import { getOpenApiSpec } from '../../features/editor/services/openapi.service';
import { usePortalPageOptional } from '../../features/portal-shell/context/PortalPageContext';

import {
    getOperationsByTag,
    getSchemasForOperations,
    parseOpenApiDocument,
    type ParsedOpenApiSpec,
    type ParsedOperation,
} from './openapi-spec-utils';

export interface ApiSpecContextValue {
    readonly spec: ParsedOpenApiSpec | undefined;
    readonly isLoading: boolean;
    readonly error: Error | null;
    readonly operations: readonly ParsedOperation[];
    readonly tags: readonly string[];
    readonly getOperationsByTag: (tag: string, operationId?: string) => ParsedOperation[];
    readonly getSchemasByTag: (tag: string, operationId?: string) => Record<string, OpenAPIV3.SchemaObject>;
}

const ApiSpecContext = createContext<ApiSpecContextValue | null>(null);

interface ApiSpecProviderProps {
    readonly children: ReactNode;
}

export function ApiSpecProvider({ children }: ApiSpecProviderProps) {
    const portalPage = usePortalPageOptional();
    const apiId = portalPage?.apiNavItem?.apiId;

    const { data, isLoading, error } = useQuery({
        queryKey: ['apiSpec', apiId],
        queryFn: async () => {
            if (!apiId || !portalPage) {
                return undefined;
            }

            const content = await resolveOpenApiSpecContent(
                { type: 'API', apiId },
                portalPage.navItems,
                portalPage.selectedNavItemId ?? '',
            );

            if (!content.trim()) {
                const fallback = await getOpenApiSpec(apiId);
                return parseOpenApiDocument(fallback.content);
            }

            return parseOpenApiDocument(content);
        },
        enabled: Boolean(apiId),
        staleTime: 5 * 60 * 1000,
    });

    const value = useMemo<ApiSpecContextValue>(() => {
        const operations = data?.operations ?? [];
        const tags = data?.tags ?? [];

        return {
            spec: data,
            isLoading,
            error: error instanceof Error ? error : error ? new Error('Failed to load API spec') : null,
            operations,
            tags,
            getOperationsByTag: (tag: string, operationId?: string) =>
                getOperationsByTag(operations, tag, operationId),
            getSchemasByTag: (tag: string, operationId?: string) => {
                const scopedOperations = getOperationsByTag(operations, tag, operationId);
                if (!data?.document) {
                    return {};
                }
                return getSchemasForOperations(data.document, scopedOperations);
            },
        };
    }, [data, error, isLoading]);

    return <ApiSpecContext.Provider value={value}>{children}</ApiSpecContext.Provider>;
}

export function useApiSpec(): ApiSpecContextValue {
    const context = useContext(ApiSpecContext);
    if (!context) {
        throw new Error('useApiSpec must be used within ApiSpecProvider');
    }
    return context;
}

export function useApiSpecOptional(): ApiSpecContextValue | null {
    return useContext(ApiSpecContext);
}
