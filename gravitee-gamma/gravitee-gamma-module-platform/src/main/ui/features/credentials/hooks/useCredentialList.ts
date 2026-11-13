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
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useCredentialsPaged } from './useCredentials';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';
import type { CredentialsPagedResponse } from '../types/credential';

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS: readonly number[] = TABLE_PAGE_SIZE_OPTIONS;

type ListParamKey = 'q' | 'page' | 'size';

function readListState(searchParams: URLSearchParams) {
    const page = Number(searchParams.get('page'));
    const size = Number(searchParams.get('size'));
    return {
        q: searchParams.get('q') ?? '',
        page: Number.isInteger(page) && page > 1 ? page : 1,
        size: PAGE_SIZE_OPTIONS.includes(size) ? size : DEFAULT_PAGE_SIZE,
    };
}

function withChanges(current: URLSearchParams, changes: Partial<Record<ListParamKey, string | undefined>>) {
    const next = new URLSearchParams(current);
    for (const [key, value] of Object.entries(changes)) {
        next.delete(key);
        if (value) {
            next.set(key, value);
        }
    }
    return next;
}

/** The credentials list, with its search and paging kept in the URL so that a reload or a shared link restores them. */
export function useCredentialList() {
    const [searchParams, setSearchParams] = useSearchParams();
    const state = readListState(searchParams);
    const query = useCredentialsPaged({ query: state.q.trim(), page: state.page, perPage: state.size });

    // Keeps the last page on screen under the error, and the toolbar with it, while a search or a retry fails.
    const [lastLoaded, setLastLoaded] = useState<CredentialsPagedResponse>();
    const [lastError, setLastError] = useState<Error | null>(null);
    const answered = query.isSuccess && !query.isPlaceholderData;
    if (answered && query.data !== lastLoaded) setLastLoaded(query.data);
    if (answered && lastError) setLastError(null);
    if (query.error && query.error !== lastError) setLastError(query.error);

    const lastPage = Math.max(1, Math.ceil((query.data?.pagination.totalCount ?? 0) / state.size));
    const pastTheEnd = answered && state.page > lastPage;
    useEffect(() => {
        if (pastTheEnd) {
            setSearchParams(current => withChanges(current, { page: lastPage > 1 ? String(lastPage) : undefined }), { replace: true });
        }
    }, [pastTheEnd, lastPage, setSearchParams]);

    const setCriteria = (changes: Partial<Record<ListParamKey, string | undefined>>) =>
        setSearchParams(current => withChanges(current, { ...changes, page: undefined }), { replace: true });
    const setPaging = (changes: Partial<Record<ListParamKey, string | undefined>>) =>
        setSearchParams(current => withChanges(current, changes));

    return {
        ...state,
        pageSizeOptions: PAGE_SIZE_OPTIONS,
        hasCriteria: state.q.trim() !== '',
        settled: answered && !pastTheEnd,
        result: query.data ?? lastLoaded,
        error: query.error ?? (query.isFetching ? lastError : null),
        isFetching: query.isFetching || pastTheEnd,
        refetch: query.refetch,
        setQuery: (q: string) => setCriteria({ q }),
        setPage: (page: number) => setPaging({ page: page > 1 ? String(page) : undefined }),
        setPageSize: (size: number) => setPaging({ size: size === DEFAULT_PAGE_SIZE ? undefined : String(size), page: undefined }),
        reset: () => setCriteria({ q: undefined }),
    };
}
