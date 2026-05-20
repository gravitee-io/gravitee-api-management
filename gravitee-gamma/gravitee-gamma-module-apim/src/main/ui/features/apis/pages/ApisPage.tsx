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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApisEmptyLanding } from '../components';
import { ApisListView } from '../components/list';
import { useApiList } from '../hooks/useApiList';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 10;

export function ApisPage() {
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: ['environment-api-c'] });

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 200);
        return () => clearTimeout(timer);
    }, [search]);

    const { data, isLoading, isFetching } = useApiList({ query: debouncedSearch, page, perPage });

    const apis = data?.data ?? [];
    const totalCount = data?.pagination?.totalCount ?? 0;

    const handleSearchChange = (value: string) => {
        setSearch(value);
        setPage(DEFAULT_PAGE);
    };

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(DEFAULT_PAGE);
    };

    const handleCreateProxy = () => navigate('new');

    // Show empty landing only when we have a confirmed empty result with no active search.
    // !debouncedSearch guards the debounce window (search cleared but debouncedSearch still has old term).
    // !isFetching guards the keepPreviousData window (debouncedSearch cleared but new fetch still in flight with stale totalCount).
    const hasNoApis = !isLoading && !isFetching && !search && !debouncedSearch && totalCount === 0;
    if (hasNoApis) {
        return <ApisEmptyLanding onCreateProxy={handleCreateProxy} canCreate={canCreate} />;
    }

    return (
        <ApisListView
            apis={apis}
            totalCount={totalCount}
            isLoading={isLoading}
            search={search}
            debouncedSearch={debouncedSearch}
            page={page}
            perPage={perPage}
            onSearchChange={handleSearchChange}
            onPageChange={setPage}
            onPerPageChange={handlePerPageChange}
            onCreateProxy={handleCreateProxy}
            canCreate={canCreate}
        />
    );
}
