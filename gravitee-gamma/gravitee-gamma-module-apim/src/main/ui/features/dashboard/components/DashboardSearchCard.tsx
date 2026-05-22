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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { Card, CardContent, Input } from '@gravitee/graphene-core';
import { ArchiveIcon, RadioIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';

import { searchApiProducts } from '../../api-products/services/apiProduct';
import type { ApiProductListItem } from '../../api-products/types/apiProduct';
import { apiProductKeys } from '../../api-products/utils/queryKeys';
import { searchApis } from '../../apis/services/apiList';
import type { ApiListItem } from '../../apis/types/api';
import { apiListKeys } from '../../apis/utils/queryKeys';

const MAX_RESULTS = 5;

// ─── Sub-components ───────────────────────────────────────────────────────────

function SectionLabel({ children }: { children: string }) {
    return <p className="text-xs font-medium text-muted-foreground px-2 pt-3 pb-1">{children}</p>;
}

interface ResultRowProps {
    icon: React.ReactNode;
    name: string;
    badge: string;
    onClick: () => void;
}

function ResultRow({ icon, name, badge, onClick }: ResultRowProps) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="w-full flex items-center gap-3 px-2 py-2 rounded-lg text-left hover:bg-muted transition-colors"
        >
            <div className="rounded-md bg-muted p-1.5 shrink-0">{icon}</div>
            <span className="flex-1 text-sm font-medium truncate">{name}</span>
            <span className="text-xs text-muted-foreground shrink-0">{badge}</span>
        </button>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface DashboardSearchCardProps {
    onNavigateToApi: (apiId: string) => void;
    onNavigateToProduct: (productId: string) => void;
}

export function DashboardSearchCard({ onNavigateToApi, onNavigateToProduct }: DashboardSearchCardProps) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(envId);

    const [input, setInput] = useState('');
    const deferredInput = useDeferredValue(input);
    const query = deferredInput.trim();
    const hasQuery = query.length > 0;

    const apisQuery = useQuery({
        queryKey: apiListKeys.search(envId, query, 1, MAX_RESULTS),
        queryFn: () => searchApis(envId, { query }, 1, MAX_RESULTS),
        enabled: enabled && hasQuery,
        staleTime: 30_000,
    });

    const productsQuery = useQuery({
        queryKey: apiProductKeys.list(envId, query, 1, MAX_RESULTS),
        queryFn: () => searchApiProducts(envId, { query }, 1, MAX_RESULTS),
        enabled: enabled && hasQuery,
        staleTime: 30_000,
    });

    const apis: ApiListItem[] = apisQuery.data?.data ?? [];
    const products: ApiProductListItem[] = productsQuery.data?.data ?? [];
    const isSearching = hasQuery && (apisQuery.isFetching || productsQuery.isFetching);
    const hasResults = apis.length > 0 || products.length > 0;

    return (
        <Card>
            <CardContent className="pt-5 pb-4">
                <p className="text-sm font-semibold mb-3">Search</p>

                <div className="relative">
                    <SearchIcon
                        className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                        aria-hidden
                    />
                    <Input
                        style={{ paddingLeft: '2.25rem' }}
                        placeholder="Search APIs, API Products…"
                        value={input}
                        onChange={e => setInput(e.target.value)}
                    />
                </div>

                {hasQuery && (
                    /* Fixed-height scroll container — results won't push the page layout */
                    <div className="mt-2 max-h-60 overflow-y-auto rounded-lg border bg-card">
                        {isSearching && !hasResults && <p className="text-xs text-muted-foreground px-3 py-3">Searching…</p>}

                        {!isSearching && !hasResults && (
                            <p className="text-xs text-muted-foreground px-3 py-3">No results for &ldquo;{query}&rdquo;</p>
                        )}

                        {apis.length > 0 && (
                            <div className="p-1">
                                <SectionLabel>APIs</SectionLabel>
                                {apis.map((api: ApiListItem) => (
                                    <ResultRow
                                        key={api.id}
                                        icon={<RadioIcon className="size-3.5 text-primary" aria-hidden />}
                                        name={api.name}
                                        badge="API Proxy"
                                        onClick={() => onNavigateToApi(api.id)}
                                    />
                                ))}
                            </div>
                        )}

                        {products.length > 0 && (
                            <div className="p-1">
                                <SectionLabel>API Products</SectionLabel>
                                {products.map((product: ApiProductListItem) => (
                                    <ResultRow
                                        key={product.id}
                                        icon={<ArchiveIcon className="size-3.5 text-primary" aria-hidden />}
                                        name={product.name}
                                        badge="API Product"
                                        onClick={() => onNavigateToProduct(product.id)}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
