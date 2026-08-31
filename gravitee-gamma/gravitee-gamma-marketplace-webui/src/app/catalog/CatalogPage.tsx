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
import { Alert, AlertDescription, Button, DataTableEmptyState, Spinner } from '@gravitee/graphene-core';
import { GlobeIcon } from '@gravitee/graphene-core/icons';

import { AgentCard } from './AgentCard';
import { hasCatalogFilters } from './catalog-params';
import { CatalogFilters } from './CatalogFilters';
import { CatalogNoResultsState, CatalogTable } from './CatalogTable';
import { useCatalogParams } from './useCatalogParams';
import { useCatalogSearch } from './useCatalogSearch';
import { useCategories } from '../layout/useCategories';

export function CatalogPage() {
    const { params, update } = useCatalogParams();
    const { agents, totalCount, labels, loading, error } = useCatalogSearch(params);
    const categories = useCategories();
    const filtersActive = hasCatalogFilters(params);
    const isFirstUse = totalCount === 0 && !filtersActive && !loading && !error;
    const isNoResults = agents.length === 0 && filtersActive && !loading && !error;
    const totalPages = Math.max(1, Math.ceil(totalCount / params.pageSize));

    const noResults = <CatalogNoResultsState onClear={() => update({ query: '', category: '', protocol: '', label: '' })} />;

    return (
        <div className="space-y-4">
            <div>
                <h1 className="text-lg font-semibold">Catalog</h1>
                <p className="text-sm text-muted-foreground">Discover agents you can subscribe to and run through the gateway.</p>
            </div>

            <CatalogFilters params={params} categories={categories} labels={labels} onChange={update} />

            {error ? (
                <Alert variant="destructive" role="alert">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : null}

            {loading && agents.length === 0 && !error && params.view === 'grid' ? (
                <div className="flex justify-center py-12">
                    <Spinner className="size-8" aria-label="Loading catalog" />
                </div>
            ) : null}

            {isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<GlobeIcon />}
                        title="No agents yet"
                        description="Published agents will appear here."
                    />
                </div>
            ) : null}

            {!error && !isFirstUse && params.view === 'grid' && !(loading && agents.length === 0) ? (
                <div className="space-y-4">
                    {isNoResults ? (
                        noResults
                    ) : (
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {agents.map(agent => (
                                <AgentCard key={agent.id} api={agent} />
                            ))}
                        </div>
                    )}
                    {totalCount > params.pageSize ? (
                        <div className="flex items-center justify-center gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                disabled={params.page <= 1}
                                onClick={() => update({ page: params.page - 1 })}
                            >
                                Previous page
                            </Button>
                            <span className="text-sm text-muted-foreground">
                                Page {params.page} of {totalPages}
                            </span>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                disabled={params.page >= totalPages}
                                onClick={() => update({ page: params.page + 1 })}
                            >
                                Next page
                            </Button>
                        </div>
                    ) : null}
                </div>
            ) : null}

            {!error && !isFirstUse && params.view === 'list' ? (
                <CatalogTable
                    agents={agents}
                    loading={loading}
                    page={params.page}
                    pageSize={params.pageSize}
                    totalCount={totalCount}
                    onPageChange={page => update({ page })}
                    onPageSizeChange={pageSize => update({ pageSize, page: 1 })}
                    emptyMessage={noResults}
                />
            ) : null}
        </div>
    );
}
