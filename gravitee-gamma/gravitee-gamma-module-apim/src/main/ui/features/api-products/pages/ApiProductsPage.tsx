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
import { useNavigate } from 'react-router-dom';

import { ApiProductListView, ApiProductsEmptyLanding } from '../components';
import { useApiProductList } from '../hooks/useApiProductList';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 10;

export function ApiProductsPage() {
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(timer);
    }, [search]);

    const { data, isLoading, isFetching, isError } = useApiProductList({
        query: debouncedSearch,
        page,
        perPage,
        productType: 'API_PRODUCT',
    });

    const products = data?.data ?? [];
    const totalCount = data?.pagination?.totalCount ?? 0;

    const handleSearchChange = (value: string) => {
        setSearch(value);
        setPage(DEFAULT_PAGE);
    };

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(DEFAULT_PAGE);
    };

    const handleCreateProduct = () => navigate('new');

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load API products. Please refresh and try again.</p>
            </div>
        );
    }

    const hasNoProducts = !isLoading && !isFetching && !search && !debouncedSearch && totalCount === 0;
    if (hasNoProducts) {
        return <ApiProductsEmptyLanding onCreateProduct={handleCreateProduct} />;
    }

    return (
        <ApiProductListView
            products={products}
            totalCount={totalCount}
            isLoading={isLoading}
            search={search}
            page={page}
            perPage={perPage}
            onSearchChange={handleSearchChange}
            onPageChange={setPage}
            onPerPageChange={handlePerPageChange}
            onCreateProduct={handleCreateProduct}
        />
    );
}
