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
import { Button, Input } from '@gravitee/graphene-core';
import { PlusIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

import { AiProductListTable } from './AiProductListTable';
import { AiProductStatsCards } from './AiProductStatsCards';
import { useAiProductsSubscribersTotal } from '../../hooks/useAiProductHooks';
import type { AiProduct } from '../../types/aiProduct';

interface AiProductListViewProps {
    products: AiProduct[];
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

export function AiProductListView({
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
}: AiProductListViewProps) {
    const searchInputId = useId();
    const deployedCount = products.filter(product => product.deploymentState === 'DEPLOYED').length;
    const { total: subscribers, isLoading: subscribersLoading } = useAiProductsSubscribersTotal(products.map(product => product.id));

    const toolbar = (
        <div className="relative max-w-sm flex-1">
            <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" aria-hidden />
            <label htmlFor={searchInputId} className="sr-only">
                Search AI products
            </label>
            <Input
                id={searchInputId}
                placeholder="Search by name, version, or owner..."
                value={search}
                onChange={e => onSearchChange(e.target.value)}
                className="pl-9"
            />
        </div>
    );

    return (
        <div className="space-y-4">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">AI Products</h1>
                    <p className="text-sm text-muted-foreground">Bundle LLM proxies, MCP proxies, and Agents for your consumers</p>
                </div>
                <Button onClick={onCreateProduct} className="shrink-0">
                    <PlusIcon className="size-4" aria-hidden />
                    Create AI Product
                </Button>
            </div>

            <AiProductStatsCards
                totalProducts={isLoading ? null : totalCount}
                deployedProducts={isLoading ? null : deployedCount}
                subscribers={isLoading || subscribersLoading ? null : subscribers}
            />

            <AiProductListTable
                products={products}
                isLoading={isLoading}
                skeletonRowCount={perPage}
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                onPageChange={onPageChange}
                onPageSizeChange={onPerPageChange}
                toolbar={toolbar}
            />
        </div>
    );
}
