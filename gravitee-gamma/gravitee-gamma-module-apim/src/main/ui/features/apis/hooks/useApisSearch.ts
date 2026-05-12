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
import { useEffect, useMemo, useState } from 'react';

import { type ApimRuntimeConfig, useApimRuntime } from '../../../core/context/apimRuntimeContext';
import { searchApisV2 } from '../../../services/apis/apisSearch.api';
import type { ApisSearchResult, ApiSummary } from '../types/apisSearch.types';

export type UseApisSearchParams = Readonly<{
    query: string;
    page: number;
    perPage: number;
    sortBy: string;
}>;

export type UseApisSearchState =
    | { readonly status: 'idle'; readonly data: null; readonly error: null }
    | { readonly status: 'loading'; readonly data: ApisSearchResult | null; readonly error: null }
    | { readonly status: 'success'; readonly data: ApisSearchResult; readonly error: null }
    | { readonly status: 'error'; readonly data: ApisSearchResult | null; readonly error: Error };

function normalizeError(err: unknown): Error {
    if (err instanceof Error) return err;
    return new Error(String(err));
}

async function fetchApis(runtime: ApimRuntimeConfig, params: UseApisSearchParams, signal: AbortSignal): Promise<ApisSearchResult> {
    return await searchApisV2(runtime, { ...params, signal });
}

export function useApisSearch(params: UseApisSearchParams): UseApisSearchState & {
    readonly totalCount: number;
    readonly apis: readonly ApiSummary[];
} {
    const runtime = useApimRuntime();
    const [state, setState] = useState<UseApisSearchState>({ status: 'idle', data: null, error: null });

    useEffect(() => {
        const abortController = new AbortController();
        setState(prev => ({ status: 'loading', data: prev.status === 'success' ? prev.data : prev.data, error: null }));

        fetchApis(runtime, params, abortController.signal)
            .then(data => setState({ status: 'success', data, error: null }))
            .catch((err: unknown) => {
                if (abortController.signal.aborted) return;
                setState(prev => ({ status: 'error', data: prev.data, error: normalizeError(err) }));
            });

        return () => abortController.abort();
    }, [params.page, params.perPage, params.query, params.sortBy, runtime]);

    const apis = useMemo(() => state.data?.apis ?? [], [state.data]);
    const totalCount = state.data?.pagination.totalCount ?? 0;

    return {
        ...state,
        apis,
        totalCount,
    };
}
