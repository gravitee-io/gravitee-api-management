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

/**
 * Keys for AI-Product-specific queries only. Product CRUD data reuses
 * `apiProductKeys` (same backend resource, shared cache).
 */
export const aiProductKeys = {
    all: ['ai-products'] as const,
    llmComponentSearch: (envId: string, query: string, page: number, perPage: number) =>
        [...aiProductKeys.all, 'llm-component-search', envId, query, page, perPage] as const,
    componentModels: (envId: string, apiId: string) => [...aiProductKeys.all, 'component-models', envId, apiId] as const,
    subscribersCount: (envId: string, productId: string) => [...aiProductKeys.all, 'subscribers-count', envId, productId] as const,
} as const;
