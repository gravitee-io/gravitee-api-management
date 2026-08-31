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
import { useEffect, useState } from 'react';

import type { CatalogParams } from './catalog-params';
import { matchesLabel, matchesProtocol, searchApis } from '../../api/catalog';
import type { Api } from '../../api/types';

const LOAD_ERROR = 'Unable to load the catalog. Please try again.';

export interface CatalogSearchState {
    agents: Api[];
    totalCount: number;
    labels: string[];
    loading: boolean;
    error: string | null;
}

function uniqueLabels(agents: readonly Api[]): string[] {
    return [...new Set(agents.flatMap(agent => agent.labels ?? []))].sort((a, b) => a.localeCompare(b));
}

export function useCatalogSearch(params: CatalogParams): CatalogSearchState {
    const [state, setState] = useState<CatalogSearchState>({
        agents: [],
        totalCount: 0,
        labels: [],
        loading: true,
        error: null,
    });

    useEffect(() => {
        let cancelled = false;
        // Portal search has no protocol or label query params, so those filters are applied locally.
        const clientFilter = Boolean(params.protocol || params.label);
        setState(current => ({ ...current, loading: true, error: null }));

        searchApis({
            query: params.query,
            category: params.category,
            page: clientFilter ? 1 : params.page,
            size: clientFilter ? -1 : params.pageSize,
        })
            .then(response => {
                if (cancelled) {
                    return;
                }
                const all = response.data ?? [];
                const filtered = all.filter(
                    agent => matchesProtocol(agent.type, params.protocol) && matchesLabel(agent.labels, params.label),
                );
                if (clientFilter) {
                    const start = (params.page - 1) * params.pageSize;
                    setState({
                        agents: filtered.slice(start, start + params.pageSize),
                        totalCount: filtered.length,
                        labels: uniqueLabels(all),
                        loading: false,
                        error: null,
                    });
                    return;
                }
                setState({
                    agents: filtered,
                    totalCount: response.metadata?.pagination?.total ?? filtered.length,
                    labels: uniqueLabels(all),
                    loading: false,
                    error: null,
                });
            })
            .catch(() => {
                if (!cancelled) {
                    setState({ agents: [], totalCount: 0, labels: [], loading: false, error: LOAD_ERROR });
                }
            });

        return () => {
            cancelled = true;
        };
    }, [params.query, params.category, params.protocol, params.label, params.page, params.pageSize]);

    return state;
}
