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

import { useCallback, useMemo, useState, type Dispatch, type SetStateAction } from 'react';

import { useSharedPolicyGroupHistories } from './useSharedPolicyGroupHistories';
import type { TableSortingState } from '../../applications/utils/tableSort';
import { DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE } from '../utils/paginationConstants';
import { toSharedPolicyGroupHistoriesSortByParam } from '../utils/sharedPolicyGroupHistoriesSort';

/**
 * Pagination + sort state and histories query for the History tab.
 * Keeps {@link SharedPolicyGroupHistoryPage} presentational.
 */
export function useSharedPolicyGroupHistoryList(sharedPolicyGroupId: string | undefined) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSortingState>([]);

    const sortBy = useMemo(() => toSharedPolicyGroupHistoriesSortByParam(sorting), [sorting]);

    const handleSortingChange = useCallback<Dispatch<SetStateAction<TableSortingState>>>(updater => {
        setSorting(updater);
        setPage(1);
    }, []);

    const handlePageSizeChange = useCallback((size: number) => {
        setPageSize(size);
        setPage(1);
    }, []);

    const query = useSharedPolicyGroupHistories({
        sharedPolicyGroupId,
        page,
        perPage: pageSize,
        sortBy,
    });

    return {
        page,
        pageSize,
        sorting,
        setPage,
        setPageSize: handlePageSizeChange,
        setSorting: handleSortingChange,
        histories: query.data?.data ?? [],
        totalCount: query.data?.pagination.totalCount ?? 0,
        isLoading: query.isLoading,
        isError: query.isError,
    };
}
