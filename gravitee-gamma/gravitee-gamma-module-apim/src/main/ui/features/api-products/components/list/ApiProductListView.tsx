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
import { Button, DataTablePagination, Input } from '@gravitee/graphene-core';
import { PlusIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

import { ApiProductListTable } from './ApiProductListTable';
import { ApiProductStatsCards } from './ApiProductStatsCards';
import type { ApiProductListItem } from '../../types/apiProduct';

interface ApiProductListViewProps {
    products: ApiProductListItem[];
    totalCount: number;
    isLoading: boolean;
    search: string;
    page: number;
    perPage: number;
    onSearchChange: (value: string) => void;
    onPageChange: (page: number) => void;
    onPerPageChange: (perPage: number) => void;
    onCreateProduct: () => void;
}

export function ApiProductListView({
    products,
    totalCount,
    isLoading,
    search,
    page,
    perPage,
    onSearchChange,
    onPageChange,
    onPerPageChange,
    onCreateProduct,
}: ApiProductListViewProps) {
    const searchInputId = useId();

    return (
        <div className="space-y-6">
            {/* Page header */}
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Products</h1>
                    <p className="text-sm text-muted-foreground">Group together multiple APIs for consumers</p>
                </div>
                <Button onClick={onCreateProduct} className="shrink-0">
                    <PlusIcon className="size-4" aria-hidden />
                    Create API Product
                </Button>
            </div>

            {/* Stats */}
            <ApiProductStatsCards totalProducts={isLoading ? null : totalCount} />

            {/* Search + pagination row */}
            <div className="flex items-center justify-between gap-4">
                <div className="relative w-72 shrink-0">
                    <SearchIcon
                        className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                        aria-hidden
                    />
                    <label htmlFor={searchInputId} className="sr-only">
                        Search API products
                    </label>
                    <Input
                        id={searchInputId}
                        placeholder="Search by name"
                        value={search}
                        onChange={e => onSearchChange(e.target.value)}
                        className="pl-9"
                    />
                </div>

                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={[10, 25, 50, 100]}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>

            <ApiProductListTable products={products} isLoading={isLoading} skeletonRowCount={perPage} />

            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={[10, 25, 50, 100]}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>
        </div>
    );
}
